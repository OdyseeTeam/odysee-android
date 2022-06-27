package com.odysee.app.ui.findcontent;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.odysee.app.OdyseeApp;
import com.odysee.app.callable.LighthouseSearch;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.callable.Search;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.LbryFile;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

import lombok.Setter;

public class SearchFragment extends BaseFragment implements
        ClaimListAdapter.ClaimListItemListener, DownloadActionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int SEARCH_CONTEXT_GROUP_ID = 3;
    private static final int PAGE_SIZE = 25;

    private ClaimListAdapter resultListAdapter;
    private ProgressBar loadingView;
    private RecyclerView resultList;
    private TextView explainerView;
    private View lassoSpacemanView;
    private View filterLink;
    private ChipGroup filterGroup;

    @Setter
    private String currentQuery;
    private boolean searchLoading;
    private boolean contentHasReachedEnd;
    private boolean didResolveFeatured;
    private int currentFrom;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);

        loadingView = root.findViewById(R.id.search_loading);
        explainerView = root.findViewById(R.id.search_explainer);
        lassoSpacemanView = root.findViewById(R.id.lasso_spaceman);

        filterLink = root.findViewById(R.id.search_filter_link);
        filterGroup = root.findViewById(R.id.chipgroupFilter);
        resultList = root.findViewById(R.id.search_result_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        resultList.setLayoutManager(llm);
        resultList.setAdapter(resultListAdapter);
        resultList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (searchLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!contentHasReachedEnd) {
                            // load more
                            int newFrom = currentFrom + PAGE_SIZE;
                            search(currentQuery, newFrom);
                        }
                    }
                }
            }
        });

        filterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = root.findViewById(R.id.chipgroupFilter).getVisibility();

                if (visibility == View.GONE) {
                    root.findViewById(R.id.chipgroupFilter).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.file_type_label).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.publish_time_filter_label).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.time_filter_spinner).setVisibility(View.VISIBLE);
                    if (((Chip) root.findViewById(R.id.chipSearchFile)).isChecked()) {
                        root.findViewById(R.id.file_type_filters).setVisibility(View.VISIBLE);
                    }
                } else {
                    root.findViewById(R.id.chipgroupFilter).setVisibility(View.GONE);
                    root.findViewById(R.id.file_type_label).setVisibility(View.GONE);
                    root.findViewById(R.id.publish_time_filter_label).setVisibility(View.GONE);
                    root.findViewById(R.id.time_filter_spinner).setVisibility(View.GONE);
                    root.findViewById(R.id.file_type_filters).setVisibility(View.GONE);
                }
            }
        });

        Context context = getContext();
        if (context != null) {
            ((Chip) root.findViewById(R.id.chipSearchFile)).setChipBackgroundColor(ContextCompat.getColorStateList(getContext(), R.color.chip_background));
            ((Chip) root.findViewById(R.id.chipSearchChannel)).setChipBackgroundColor(ContextCompat.getColorStateList(getContext(), R.color.chip_background));
        }

        ((ChipGroup)root.findViewById(R.id.chipgroupFilter)).setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                checkNothingToBeShown();
                if (checkedId == View.NO_ID) {
                    if (resultListAdapter != null) {
                        resultListAdapter.clearSearchFilters();
                    }
                } else if (checkedId == R.id.chipSearchFile) {
                    if (resultListAdapter != null) {
                        resultListAdapter.setFilterByFile(true);
                    }
                } else if (checkedId == R.id.chipSearchChannel) {
                    if (resultListAdapter != null) {
                        resultListAdapter.setFilterByChannel(true);
                    }
                }

                if (checkedId == R.id.chipSearchFile) {
                    root.findViewById(R.id.file_type_filters).setVisibility(View.VISIBLE);
                } else {
                    root.findViewById(R.id.file_type_filters).setVisibility(View.GONE);
                }
            }
        });

        ((CheckBox) root.findViewById(R.id.video_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (resultListAdapter != null) {
                    resultListAdapter.setFileTypeFilters(isChecked, null, null, null);
                }
            }
        });
        ((CheckBox) root.findViewById(R.id.audio_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (resultListAdapter != null) {
                    resultListAdapter.setFileTypeFilters(null, isChecked, null, null);
                }
            }
        });
        ((CheckBox) root.findViewById(R.id.image_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (resultListAdapter != null) {
                    resultListAdapter.setFileTypeFilters(null, null, isChecked, null);
                }
            }
        });
        ((CheckBox) root.findViewById(R.id.text_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (resultListAdapter != null) {
                    resultListAdapter.setFileTypeFilters(null, null, null, isChecked);
                }
            }
        });

        ((AppCompatSpinner) root.findViewById(R.id.time_filter_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (resultListAdapter != null) {
                    Calendar cal = Calendar.getInstance();
                    if (position == 0) {
                        resultListAdapter.clearTimeframeFilter();
                    } else {
                        switch (position) {
                            case 1: // Last 24 hours
                                cal.add(Calendar.HOUR_OF_DAY, -24);
                                break;
                            case 2: // This week
                                cal.add(Calendar.DAY_OF_YEAR, -7);
                                break;
                            case 3: // This month
                                cal.add(Calendar.MONTH, -1);
                                break;
                            case 4: // This year
                                cal.add(Calendar.YEAR, -1);
                                break;
                        }
                        resultListAdapter.setFilterTimeframeFrom(cal.getTimeInMillis());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return root;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == SEARCH_CONTEXT_GROUP_ID && item.getItemId() == R.id.action_block) {
            if (resultListAdapter != null) {
                int position = resultListAdapter.getPosition();
                Claim claim = resultListAdapter.getItems().get(position);
                if (claim != null && claim.getSigningChannel() != null) {
                    Claim channel = claim.getSigningChannel();
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).handleBlockChannel(channel);
                    }
                }
            }
            return true;
        }

        return super.onContextItemSelected(item);
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        Helper.setWunderbarValue(currentQuery, context);
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                LbryAnalytics.setCurrentScreen(activity, "Search", "Search");
                activity.addDownloadActionListener(this);
                activity.updateCurrentDisplayFragment(this);
                activity.updateMiniPlayerMargins(false);
            }
        }
        if (!Helper.isNullOrEmpty(currentQuery)) {
            logSearch(currentQuery);
            search(currentQuery, currentFrom);
        } else {
            lassoSpacemanView.setVisibility(View.VISIBLE);
            explainerView.setText(getString(R.string.search_type_to_discover));
            explainerView.setVisibility(View.VISIBLE);
        }

        applyFilterForBlockedChannels(Lbryio.blockedChannels);
    }

    public void onPause() {
        Context context = getContext();
        if (context != null) {
            MainActivity activity = (MainActivity) context;
            activity.removeDownloadActionListener(this);
            activity.updateMiniPlayerMargins(true);
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        MainActivity activity = (MainActivity) getContext();
        if (activity != null) {
            activity.resetCurrentDisplayFragment();
            activity.showBottomNavigation();
            activity.cancelScheduledSearchFuture();
        }

        super.onStop();
    }

    private boolean checkQuery(String query) {
        if (!Helper.isNullOrEmpty(query) && !query.equalsIgnoreCase(currentQuery)) {
            // new query, reset values
            currentFrom = 0;
            currentQuery = query;
            if (resultListAdapter != null) {
                resultListAdapter.clearItems();
            }
            return true;
        }

        if (Helper.isNullOrEmpty(query)) {
            lassoSpacemanView.setVisibility(View.VISIBLE);
            explainerView.setText(getString(R.string.search_type_to_discover));
            resultList.setVisibility(View.GONE);
            explainerView.setVisibility(View.VISIBLE);
        }
        return false;
    }

    private Claim buildFeaturedItem(String query) {
        Claim claim = new Claim();
        claim.setName(query);
        claim.setFeatured(true);
        claim.setUnresolved(true);
        claim.setConfirmations(1);
        return claim;
    }

    private void resolveFeaturedItem(String query) {
        String sanitizedQuery = query.trim();
        sanitizedQuery = sanitizedQuery.replaceAll(LbryUri.REGEX_INVALID_URI, "");

        List<String> possibleUrls = new ArrayList<>();
        possibleUrls.add("lbry://" + sanitizedQuery);
        if (!sanitizedQuery.startsWith("@")) {
            possibleUrls.add("lbry://@" + sanitizedQuery);
        }

        ResolveTask task = new ResolveTask(possibleUrls, Lbry.API_CONNECTION_STRING, null, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                // Add resolved claim(s) to cache
                for (Claim claim : claims) {
                    final ClaimCacheKey key = ClaimCacheKey.fromClaim(claim);
                    Lbry.claimCache.put(key, claim);
                }

                if (claims.size() > 0) {
                    Collections.sort(claims, (c1, c2) ->
                            (int) (Double.parseDouble(c1.getMeta().getEffectiveAmount()) -
                                    Double.parseDouble(c2.getMeta().getEffectiveAmount())));
                    Claim featuredItem = claims.get(0);
                    featuredItem.setFeatured(true);

                    Context context = getContext();
                    boolean canShowMatureContent = false;
                    if (context != null) {
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                        canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
                    }
                    if (!featuredItem.isMature() || canShowMatureContent) {
                        List<Claim> items = resultListAdapter.getItems();
                        items.removeIf(c -> !c.getClaimId().equals(featuredItem.getClaimId()));
                        resultListAdapter.removeItems(items);
                        resultListAdapter.addFeaturedItem(featuredItem);
                    }

                    loadingView.setVisibility(View.GONE);
                    didResolveFeatured = true;
                } else {
                    resultListAdapter.addFeaturedItem(buildFeaturedItem(query));
                }
                checkNothingToBeShown();
            }

            @Override
            public void onError(Exception error) {
                loadingView.setVisibility(View.GONE);
                error.printStackTrace();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void logSearch(String query) {
        Bundle bundle = new Bundle();
        bundle.putString("query", query);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_SEARCH, bundle);
    }

    public void search(String query, int from) {
        boolean queryChanged = checkQuery(query);

        if (query.equals("")) {
            return;
        }
        if (!queryChanged && from > 0) {
            currentFrom = from;
        }

        if (queryChanged) {
            didResolveFeatured = false;
            logSearch(query);
        }

        searchLoading = true;
        Context context = getContext();
        boolean canShowMatureContent = false;
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
        }

        // If the query consists of a single word -characters not separated by spaces-,
        // modify the request so it returns channels on top
        if (currentQuery != null) {
            final String[] split = currentQuery.split(" ");
            if (split.length == 1 && !currentQuery.startsWith("@")) {
                currentQuery = "@".concat(query);
            }
        }

        Activity a = getActivity();

        Callable<List<Claim>> c = new LighthouseSearch(currentQuery, PAGE_SIZE, currentFrom, canShowMatureContent, null);
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingView.setVisibility(View.VISIBLE);
                }
            });

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Future<List<Claim>> future = ((OdyseeApp) a.getApplication()).getExecutor().submit(c);

                    try {
                        List<Claim> results = future.get();

                        List<Claim> sanitizedClaims = new ArrayList<>(results.size());

                        for (Claim item : results) {
                            if (!item.getValueType().equalsIgnoreCase(Claim.TYPE_REPOST)) {
                                sanitizedClaims.add(item);
                            }
                        }

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Context context = getContext();
                                if (context != null) {
                                    if (resultListAdapter == null) {
                                        resultListAdapter = new ClaimListAdapter(sanitizedClaims, context);
                                        resultListAdapter.setContextGroupId(SEARCH_CONTEXT_GROUP_ID);
                                        resultListAdapter.setListener(SearchFragment.this);
                                        if (resultList != null) {
                                            resultList.setAdapter(resultListAdapter);
                                        }
                                    } else {
                                        resultList.setVisibility(View.VISIBLE);
                                        resultListAdapter.addItems(sanitizedClaims);
                                    }

                                    resultListAdapter.filterBlockedChannels(Lbryio.blockedChannels);
                                    checkNothingToBeShown();
                                }
                            }
                        });

                        // Lighthouse doesn't return "valueType" of the claim, so another request is needed
                        // to determine if an item is a playlist and get the items on the playlist.
                        List<String> claimIds = new ArrayList<>();

                        for (Claim sanitizedClaim : sanitizedClaims) {
                            if (!sanitizedClaim.getValueType().equalsIgnoreCase(Claim.TYPE_CHANNEL)) {
                                claimIds.add(sanitizedClaim.getClaimId());
                            }
                        }

                        Map<String, Object> claimSearchOptions = new HashMap<>(2);

                        claimSearchOptions.put("claim_ids", claimIds);
                        claimSearchOptions.put("page_size", claimIds.size());

                        Future<List<Claim>> futureSearch = ((OdyseeApp) a.getApplication()).getExecutor().submit(new Search(claimSearchOptions));
                        List<Claim> totalResults = futureSearch.get();

                        // For each claim returned from Lighthouse, replace it by the one using Search API
                        for (int i = 0; i < sanitizedClaims.size(); i++) {
                            if (!Claim.TYPE_CHANNEL.equalsIgnoreCase(sanitizedClaims.get(i).getValueType())) {
                                int finalI = i;
                                Claim found = totalResults.stream().filter(filteredClaim -> {
                                    return sanitizedClaims.get(finalI).getClaimId().equalsIgnoreCase(filteredClaim.getClaimId());
                                }).findAny().orElse(null);

                                if (found != null) {
                                    sanitizedClaims.set(i, found);

                                    if (resultListAdapter != null) {
                                        a.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!found.getValueType().equalsIgnoreCase(Claim.TYPE_REPOST)) {
                                                    resultListAdapter.setItem(found.getClaimId(), found);
                                                } else {
                                                    resultListAdapter.removeItem(found);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        }
                        contentHasReachedEnd = results.size() < PAGE_SIZE;
                        searchLoading = false;

                        if (didResolveFeatured) {
                            a.runOnUiThread(() -> loadingView.setVisibility(View.GONE));
                        } else {
                            resolveFeaturedItem(query);
                        }

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int itemCount = resultListAdapter == null ? 0 : resultListAdapter.getItemCount();

                                if (itemCount == 0) {
                                    filterLink.setVisibility(View.GONE);
                                } else {
                                    filterLink.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    public void onClaimClicked(Claim claim, int position) {
        if (Helper.isNullOrEmpty(claim.getName())) {
            // never should happen, but if it does, do nothing
            return;
        }

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (claim.isUnresolved()) {
                // open the publish page
//                Map<String, Object> params = new HashMap<>();
//                params.put("suggestedUrl", claim.getName());
//                activity.openFragment(PublishFragment.class, true, NavMenuItem.ID_ITEM_NEW_PUBLISH, params);
            } else if (claim.getName().startsWith("@")) {
                activity.openChannelClaim(claim);
            } else {
                // not a channel
                activity.openFileClaim(claim);
            }
        }
    }

    private void checkNothingToBeShown() {
        List<Claim> adapterItems = resultListAdapter.getItems();
        Claim firstStream = adapterItems.stream().filter(s -> Claim.TYPE_STREAM.equalsIgnoreCase(s.getValueType()))
                                                 .findAny().orElse(null);
        int checked = filterGroup.getCheckedChipId();
        if (adapterItems.size() == 0
            || (checked == R.id.chipSearchFile && firstStream == null)) {
            lassoSpacemanView.setVisibility(View.VISIBLE);
            Helper.setViewText(explainerView, getString(R.string.search_no_results, currentQuery));
            explainerView.setVisibility(View.VISIBLE);
        } else {
            lassoSpacemanView.setVisibility(View.GONE);
            explainerView.setVisibility(View.GONE);
        }
    }

    public void applyFilterForBlockedChannels(List<LbryUri> blockedChannels) {
        if (resultListAdapter != null) {
            resultListAdapter.filterBlockedChannels(blockedChannels);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT)) {
            search(currentQuery, currentFrom);
        }
    }

    public void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {
        if ("abort".equals(downloadAction)) {
            if (resultListAdapter != null) {
                resultListAdapter.clearFileForClaimOrUrl(outpoint, uri);
            }
            return;
        }

        try {
            JSONObject fileInfo = new JSONObject(fileInfoJson);
            LbryFile claimFile = LbryFile.fromJSONObject(fileInfo);
            String claimId = claimFile.getClaimId();
            if (resultListAdapter != null) {
                resultListAdapter.updateFileForClaimByIdOrUrl(claimFile, claimId, uri);
            }
        } catch (JSONException ex) {
            // invalid file info for download
        }
    }
}
