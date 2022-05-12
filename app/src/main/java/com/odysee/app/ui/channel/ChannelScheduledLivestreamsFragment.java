package com.odysee.app.ui.channel;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.callable.ChannelLiveStatus;
import com.odysee.app.callable.Search;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Predefined;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;

public class ChannelScheduledLivestreamsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Setter
    private String channelId;
    private RecyclerView scheduledStreamsList;
    private View contentLoading;
    private View bigLivestreamsLoading;
    private View noLivestreamsView;
    private ClaimListAdapter livestreamsListAdapter;
    private boolean scheduledClaimSearchLoading;
    private boolean scheduledHasReachedEnd;
    private int currentClaimSearchPage;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel_livestreams, container, false);

        bigLivestreamsLoading = root.findViewById(R.id.channel_livestreams_list_progress);
        contentLoading = root.findViewById(R.id.channel_content_load_progress);
        noLivestreamsView = root.findViewById(R.id.channel_livestreams_no_claim_search_lists);

        scheduledStreamsList = root.findViewById(R.id.channel_livestreams_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        scheduledStreamsList.setLayoutManager(llm);
        scheduledStreamsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (scheduledClaimSearchLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!scheduledHasReachedEnd) {
                            // load more
                            currentClaimSearchPage++;
                            fetchClaimSearchScheduledLivestreams();
                        }
                    }
                }
            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        }
        fetchClaimSearchScheduledLivestreams();
    }

    public void onPause() {
        Context context = getContext();
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    private Map<String, Object> buildScheduledLivestreamsOptions() {
        Context context = getContext();
        boolean canShowMatureContent = false;
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
        }

        Date ct = new Date();
        Calendar cal = GregorianCalendar.getInstance(); // locale-specific
        cal.setTime(ct);
//        cal.add(Calendar.MINUTE, -5);

        int releaseTime = Long.valueOf(cal.getTimeInMillis() / 1000L).intValue();

        return Lbry.buildClaimSearchOptions(
                Collections.singletonList(Claim.TYPE_STREAM),
                null,
                canShowMatureContent ? null : new ArrayList<>(Predefined.MATURE_TAGS),
                null,
                !Helper.isNullOrEmpty(channelId) ? Collections.singletonList(channelId) : null,
                null,
                Collections.singletonList("^release_time"),
                ">" + releaseTime,
                0,
                0,
                currentClaimSearchPage == 0 ? 1 : currentClaimSearchPage,
                Helper.CONTENT_PAGE_SIZE);
    }

    private View getLoadingView() {
        return (livestreamsListAdapter == null || livestreamsListAdapter.getItemCount() == 0) ? bigLivestreamsLoading : contentLoading;
    }

    private void fetchClaimSearchScheduledLivestreams() {
        fetchClaimSearchScheduledLivestreams(false);
    }

    private void fetchClaimSearchScheduledLivestreams(boolean reset) {
        if (reset && livestreamsListAdapter != null) {
            livestreamsListAdapter.clearItems();
            currentClaimSearchPage = 1;
        }

        scheduledClaimSearchLoading = true;
        Helper.setViewVisibility(noLivestreamsView, View.GONE);
        Helper.setViewVisibility(getLoadingView(), View.VISIBLE);
        // channel claim
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Claim> scheduledClaims = findScheduledLivestreams();

                Activity a = getActivity();

                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Helper.setViewVisibility(getLoadingView(), View.GONE);
                        }
                    });
                }
                if (scheduledClaims != null && scheduledClaims.size() > 0) {
                    scheduledClaims = Helper.filterClaimsByOutpoint(scheduledClaims);

                    if (a != null) {
                        List<Claim> finalScheduledClaims = scheduledClaims;
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (livestreamsListAdapter == null) {
                                    Context context = getContext();
                                    if (context != null) {
                                        livestreamsListAdapter = new ClaimListAdapter(finalScheduledClaims, context);
                                        livestreamsListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                                            @Override
                                            public void onClaimClicked(Claim claim, int position) {
                                                Context context = getContext();
                                                if (context instanceof MainActivity) {
                                                    MainActivity activity = (MainActivity) context;
                                                    activity.openFileClaim(claim);
                                                }
                                            }
                                        });
                                    }
                                } else {
                                    livestreamsListAdapter.addItems(finalScheduledClaims);
                                }

                                if (scheduledStreamsList != null && scheduledStreamsList.getAdapter() == null) {
                                    scheduledStreamsList.setAdapter(livestreamsListAdapter);
                                }

                                scheduledHasReachedEnd = finalScheduledClaims.size() < Helper.CONTENT_PAGE_SIZE;
                                scheduledClaimSearchLoading = false;
                            }
                        });
                    }
                }
                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkNoScheduledLivestreams();
                        }
                    });
                }
            }
        });
        t.start();
    }

    private List<Claim> findScheduledLivestreams() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Claim> upcomingClaims = null;
            Callable<Map<String, JSONObject>> callable = new ChannelLiveStatus(Collections.singletonList(channelId), true);
            Future<Map<String, JSONObject>> futureUpcoming = executor.submit(callable);
            Map<String, JSONObject> upcomingJsonData = futureUpcoming.get();

            if (upcomingJsonData.size() > 0) {
                if (upcomingJsonData.containsKey(channelId)) {
                    JSONObject channelData = upcomingJsonData.get(channelId);
                    if (channelData != null && channelData.has("FutureClaims")) {
                        JSONArray jsonClaimIds = channelData.optJSONArray("FutureClaims");

                        if (jsonClaimIds != null) {
                            List<String> claimIds = new ArrayList<>();
                            for (int j = 0; j < jsonClaimIds.length(); j++) {
                                JSONObject obj = jsonClaimIds.getJSONObject(j);
                                claimIds.add(obj.getString("ClaimID"));
                            }

                            Map<String, Object> claimSearchOptions = buildScheduledLivestreamsOptions();

                            claimSearchOptions.put("claim_type", Collections.singletonList(Claim.TYPE_STREAM));
                            claimSearchOptions.put("has_no_source", true);
                            claimSearchOptions.put("claim_ids", claimIds);
                            Future<List<Claim>> upcomingFuture = executor.submit(new Search(claimSearchOptions));

                            upcomingClaims = upcomingFuture.get();
                            if (channelData.has("ActiveClaim")) {
                                // Extract active livestream's claimId to compare with future ones
                                JSONObject activeClaimIdJSON = (JSONObject) channelData.get("ActiveClaim");

                                upcomingClaims.removeIf(t -> {
                                    try {
                                        return channelData.getBoolean("Live") && t.getClaimId().equalsIgnoreCase(activeClaimIdJSON.getString("ClaimID"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                });
                            }
                        }
                    }
                }
            }

            executor.shutdown();
            return upcomingClaims;
        } catch (InterruptedException | ExecutionException | JSONException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }
            return null;
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    private void checkNoScheduledLivestreams() {
        boolean noPlaylists = livestreamsListAdapter == null || livestreamsListAdapter.getItemCount() == 0;
        Helper.setViewVisibility(noLivestreamsView, noPlaylists ? View.VISIBLE : View.GONE);
    }

    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equalsIgnoreCase(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT)) {
            fetchClaimSearchScheduledLivestreams(true);
        }
    }
}
