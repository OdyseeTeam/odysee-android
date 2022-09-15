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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.collect.Ordering;
import com.odysee.app.OdyseeApp;
import com.odysee.app.callable.LighthouseSearch;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.exceptions.LbryUriException;
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
    private String currentClaimType;
    private List<String> currentMediaTypes = Arrays.asList(Claim.STREAM_TYPE_VIDEO,
            Claim.STREAM_TYPE_AUDIO, Claim.STREAM_TYPE_IMAGE, Claim.STREAM_TYPE_TEXT);
    private String currentTimeFilter;
    private String currentSortBy;

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
                    root.findViewById(R.id.sort_by_filter_spinner).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.sort_by_filter_label).setVisibility(View.VISIBLE);
                    if (((Chip) root.findViewById(R.id.chipSearchFile)).isChecked()) {
                        root.findViewById(R.id.file_type_filters).setVisibility(View.VISIBLE);
                    }
                } else {
                    root.findViewById(R.id.chipgroupFilter).setVisibility(View.GONE);
                    root.findViewById(R.id.file_type_label).setVisibility(View.GONE);
                    root.findViewById(R.id.publish_time_filter_label).setVisibility(View.GONE);
                    root.findViewById(R.id.time_filter_spinner).setVisibility(View.GONE);
                    root.findViewById(R.id.sort_by_filter_spinner).setVisibility(View.GONE);
                    root.findViewById(R.id.sort_by_filter_label).setVisibility(View.GONE);
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
                String claimType = null; // None selected
                if (checkedId == R.id.chipSearchFile) {
                    claimType = Claim.TYPE_STREAM;
                } else if (checkedId == R.id.chipSearchChannel) {
                    claimType = Claim.TYPE_CHANNEL;
                }
                search(currentQuery, currentFrom, claimType, currentMediaTypes, currentTimeFilter, currentSortBy);

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
                setMediaTypes(isChecked, null, null, null);
            }
        });
        ((CheckBox) root.findViewById(R.id.audio_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setMediaTypes(null, isChecked, null, null);
            }
        });
        ((CheckBox) root.findViewById(R.id.image_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setMediaTypes(null, null, isChecked, null);
            }
        });
        ((CheckBox) root.findViewById(R.id.text_filetype_checkbox)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setMediaTypes(null, null, null, isChecked);
            }
        });

        ((AppCompatSpinner) root.findViewById(R.id.time_filter_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (resultListAdapter != null) {
                    String timeFilter;
                    switch (position) {
                        case 1: // Last 24 hours
                            timeFilter = "today";
                            break;
                        case 2: // This week
                            timeFilter = "thisweek";
                            break;
                        case 3: // This month
                            timeFilter = "thismonth";
                            break;
                        case 4: // This year
                            timeFilter = "thisyear";
                            break;
                        case 0: // Anytime
                        default:
                            timeFilter = null;
                    }
                    search(currentQuery, currentFrom, currentClaimType, currentMediaTypes, timeFilter, currentSortBy);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ((AppCompatSpinner) root.findViewById(R.id.sort_by_filter_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String sortBy;
                switch (position) {
                    case 1: // Newest first
                        sortBy = "release_time";
                        break;
                    case 2: // Oldest first
                        sortBy = "^release_time";
                        break;
                    case 0: // Relevance
                    default:
                        sortBy = null;
                }
                search(currentQuery, currentFrom, currentClaimType, currentMediaTypes, currentTimeFilter, sortBy);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
                        ((MainActivity) context).handleMuteChannel(channel);
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

        applyFilterForMutedChannels(Lbryio.mutedChannels);
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

    private boolean checkQuery(String query,
                               @Nullable String claimType,
                               @NonNull List<String> mediaTypes,
                               @Nullable String timeFilter,
                               @Nullable String sortBy) {
        if ((!Helper.isNullOrEmpty(query) && !query.equalsIgnoreCase(currentQuery)) ||
                (!Objects.equals(claimType, currentClaimType)) ||
                (mediaTypes.size() != currentMediaTypes.size() && !mediaTypes.containsAll(currentMediaTypes)) ||
                (!Objects.equals(timeFilter, currentTimeFilter)) ||
                (!Objects.equals(sortBy, currentSortBy))) {
            // new query, reset values
            currentFrom = 0;
            currentQuery = query;
            currentClaimType = claimType;
            currentMediaTypes = mediaTypes;
            currentTimeFilter = timeFilter;
            currentSortBy = sortBy;
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

    private void setMediaTypes(
            @Nullable Boolean showVideos,
            @Nullable Boolean showAudios,
            @Nullable Boolean showImages,
            @Nullable Boolean showTexts
    ) {
        List<String> mediaTypes = new ArrayList<>(currentMediaTypes);

        if (showVideos != null && showVideos && !mediaTypes.contains(Claim.STREAM_TYPE_VIDEO)) {
            mediaTypes.add(Claim.STREAM_TYPE_VIDEO);
        } else if (showVideos != null && !showVideos) {
            mediaTypes.remove(Claim.STREAM_TYPE_VIDEO);
        }

        if (showAudios != null && showAudios && !mediaTypes.contains(Claim.STREAM_TYPE_AUDIO)) {
            mediaTypes.add(Claim.STREAM_TYPE_AUDIO);
        } else if (showAudios != null && !showAudios) {
            mediaTypes.remove(Claim.STREAM_TYPE_AUDIO);
        }

        if (showImages != null && showImages && !mediaTypes.contains(Claim.STREAM_TYPE_IMAGE)) {
            mediaTypes.add(Claim.STREAM_TYPE_IMAGE);
        } else if (showImages != null && !showImages) {
            mediaTypes.remove(Claim.STREAM_TYPE_IMAGE);
        }

        if (showTexts != null && showTexts && !mediaTypes.contains(Claim.STREAM_TYPE_TEXT)) {
            mediaTypes.add(Claim.STREAM_TYPE_TEXT);
        } else if (showTexts != null && !showTexts) {
            mediaTypes.remove(Claim.STREAM_TYPE_TEXT);
        }

        search(currentQuery, currentFrom, currentClaimType, mediaTypes, currentTimeFilter, currentSortBy);
    }

    private void logSearch(String query) {
        Bundle bundle = new Bundle();
        bundle.putString("query", query);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_SEARCH, bundle);
    }

    public void search(String query, int from) {
        search(query, from, currentClaimType, currentMediaTypes, currentTimeFilter, currentSortBy);
    }

    public void search(
            String query, int from,
            @Nullable String claimType,
            @NonNull List<String> mediaTypes,
            @Nullable String timeFilter,
            @Nullable String sortBy
    ) {
        boolean queryChanged = checkQuery(query, claimType, mediaTypes, timeFilter, sortBy);

        if (query.equals("") || searchLoading) {
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

        LighthouseSearch c = new LighthouseSearch(currentQuery, PAGE_SIZE, currentFrom, canShowMatureContent, null,
                currentClaimType, String.join(",", currentMediaTypes), currentTimeFilter, currentSortBy);
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
                    Future<List<String>> future = ((OdyseeApp) a.getApplication()).getExecutor().submit(c);

                    try {
                        List<String> urls = future.get();

                        Callable<List<Claim>> resolveCallable = () -> Lbry.resolve(urls, Lbry.API_CONNECTION_STRING);
                        Future<List<Claim>> resolveFuture = ((OdyseeApp) a.getApplication()).getExecutor().submit(resolveCallable);

                        List<Claim> results = resolveFuture.get();
                        int size = results.size();
                        results.removeIf(Objects::isNull);
                        int removedCount = size - results.size();

                        if (!urls.contains("")) {
                            urls.add(""); // Explicit empty string as catch-all for LbryUri.normalize errors
                        }
                        Collections.sort(results, Ordering.explicit(urls).onResultOf(claim -> {
                            try {
                                return LbryUri.normalize(claim.getPermanentUrl());
                            } catch (LbryUriException ex) {
                                ex.printStackTrace();
                            }
                            return "";
                        }));

                        List<Claim> sanitizedClaims = results.stream().filter(item -> !item.getValueType().equalsIgnoreCase(Claim.TYPE_REPOST))
                                .collect(Collectors.toList());

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

                                    resultListAdapter.filterBlockedChannels(Lbryio.mutedChannels);
                                    checkNothingToBeShown();
                                }
                            }
                        });

                        contentHasReachedEnd = results.size() < PAGE_SIZE - removedCount;
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
                        searchLoading = false;
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

    public void applyFilterForMutedChannels(List<LbryUri> blockedChannels) {
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
