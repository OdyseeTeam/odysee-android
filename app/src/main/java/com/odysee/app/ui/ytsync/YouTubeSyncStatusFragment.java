package com.odysee.app.ui.ytsync;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.odysee.app.R;
import com.odysee.app.adapter.YouTubeSyncItemAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.listener.YouTubeSyncListener;
import com.odysee.app.model.YouTubeSyncItem;
import com.odysee.app.model.lbryinc.User;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Setter;

public class YouTubeSyncStatusFragment extends Fragment {
    @Setter
    private YouTubeSyncListener listener;
    public static final String SYNC_STATUS_SYNCED = "synced";
    public static final String SYNC_STATUS_ABANDONED = "abandoned";
    public static final String TRANSFER_STATE_COMPLETED_TRANSFER = "completed_transfer";
    public static final String TRANSFER_STATE_TRANSFERRED = "transferred";

    private static final int REFRESH_INTERVAL = 30; // 30 seconds
    private YouTubeSyncItemAdapter adapter;
    private TextView textHint;
    private MaterialButton claimChannelButton;
    private MaterialButton newSyncButton;
    private MaterialButton exploreOdyseeButton;
    private ProgressBar loadProgress;
    private RecyclerView listView;

    private boolean refreshScheduled;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> future;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_youtube_sync_status, container, false);

        Context context = getContext();
        listView = root.findViewById(R.id.youtube_sync_item_list);
        LinearLayoutManager llm = new LinearLayoutManager(context);
        listView.setLayoutManager(llm);

        loadProgress = root.findViewById(R.id.youtube_sync_status_load_progress);

        claimChannelButton = root.findViewById(R.id.youtube_sync_claim_channel_button);
        textHint = root.findViewById(R.id.youtube_sync_status_hint);
        textHint.setMovementMethod(LinkMovementMethod.getInstance());
        textHint.setText(HtmlCompat.fromHtml(getString(R.string.you_will_be_able), HtmlCompat.FROM_HTML_MODE_LEGACY));

        newSyncButton = root.findViewById(R.id.youtube_sync_new_sync);
        exploreOdyseeButton = root.findViewById(R.id.youtube_sync_explore_odysee);
        newSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onNewSyncPressed();
                }
            }
        });

        exploreOdyseeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onDonePressed();
                }
            }
        });

        claimChannelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<YouTubeSyncItem> claimable = adapter.getClaimableItems();
                if (claimable.size() > 0) {
                    doClaimItems(claimable);
                }
            }
        });

        return root;
    }

    private void doClaimItems(List<YouTubeSyncItem> claimable) {
        if (claimable.size() == 0) {
            return;
        }

        claimChannelButton.setEnabled(false);
        loadProgress.setVisibility(View.VISIBLE);
        cancelRefreshSchedule();

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> claimableChannelNames = new ArrayList<>();
                    for (YouTubeSyncItem item : claimable) {
                        claimableChannelNames.add(item.getChannel().getLbryChannelName().toLowerCase());
                    }

                    JSONObject response = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_ADDRESS_LIST, null, Lbryio.AUTH_TOKEN);
                    JSONArray items = response.getJSONArray("items");
                    if (items.length() > 0) {
                        JSONObject addressItem = items.getJSONObject(0);
                        String address = Helper.getJSONString("address", "", addressItem);
                        String publicKey = Helper.getJSONString("pubkey", "", addressItem);

                        Map<String, String> options = new HashMap<>();
                        options.put("address", address);
                        options.put("public_key", publicKey);

                        JSONArray transferredList = (JSONArray) Lbryio.parseResponse(Lbryio.call("yt", "transfer", options, null));
                        if (transferredList.length() > 0) {
                            for (int i = 0; i < transferredList.length(); i++) {
                                JSONObject transferredItem = transferredList.getJSONObject(i);
                                JSONObject channelData = transferredItem.getJSONObject("channel");
                                String lbryChannelName = Helper.getJSONString("lbry_channel_name", "", channelData);
                                if (claimableChannelNames.contains(lbryChannelName.toLowerCase())) {
                                    importChannel(Helper.getJSONString("channel_certificate", "", channelData));
                                }
                            }
                        }
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            showMessage(getString(R.string.successfully_claimed_youtube_channels));
                            loadStatus();
                        }
                    });
                } catch (ApiCallException | LbryioRequestException | LbryioResponseException | JSONException ex) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            showError(ex.getMessage());
                            loadStatus();
                        }
                    });
                }
            }
        });
    }

    private void importChannel(String channelCert) throws ApiCallException {
        if (Helper.isNullOrEmpty(channelCert)) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("channel_data", channelCert);
        Lbry.authenticatedGenericApiCall("channel_import", params, Lbryio.AUTH_TOKEN);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelRefreshSchedule();
    }

    private void cancelRefreshSchedule() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        refreshScheduled = false;
    }

    private void loadStatus() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                loadProgress.setVisibility(View.VISIBLE);
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray status = (JSONArray) Lbryio.parseResponse(Lbryio.call("yt", "transfer", null, Helper.METHOD_POST, null));
                    if (status.length() == 0) {
                        // if we made it to this screen then this should never happen, but if it does, redirect to a new YouTube sync
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.onNewSyncPressed();
                                }
                            }
                        });
                        return;
                    }

                    // Parse the status items
                    List<YouTubeSyncItem> itemList = new ArrayList<>();
                    for (int i = 0; i < status.length(); i++) {
                        JSONObject object = status.getJSONObject(i);
                        Type type = new TypeToken<YouTubeSyncItem>(){}.getType();
                        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
                        YouTubeSyncItem item = gson.fromJson(object.toString(), type);
                        itemList.add(item);
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onYouTubeSyncItemsLoaded(itemList);
                        }
                    });
                } catch (LbryioRequestException | LbryioResponseException | JSONException ex) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            loadProgress.setVisibility(View.INVISIBLE);
                            showError(ex.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void onYouTubeSyncItemsLoaded(List<YouTubeSyncItem> items) {
        loadProgress.setVisibility(View.INVISIBLE);
        if (adapter == null) {
            adapter = new YouTubeSyncItemAdapter(items, getContext());
            listView.setAdapter(adapter);
        } else {
            adapter.update(items);
        }

        loadFollowerCounts(items);

        List<YouTubeSyncItem> claimableItems = adapter.getClaimableItems();
        claimChannelButton.setEnabled(claimableItems.size() > 0);
        claimChannelButton.setVisibility(claimableItems.size() > 0 ? View.VISIBLE : View.GONE);
        if (claimableItems.size() > 0) {
            claimChannelButton.setText(getResources().getQuantityString(R.plurals.claim_channel, claimableItems.size(), claimableItems.size()));
        }

        List<YouTubeSyncItem> pendingItems = adapter.getPendingItems();
        if (!refreshScheduled && (pendingItems.size() > 0 || claimableItems.size() > 0)) {
            scheduleRefreshStatus();
            refreshScheduled = true;
        }
    }

    private void scheduleRefreshStatus() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        future = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    loadStatus();
                } catch (Exception ex) {
                    // pass
                }
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);
    }

    private void loadFollowerCounts(List<YouTubeSyncItem> items) {
        List<String> channelClaimIds = new ArrayList<>();
        for (YouTubeSyncItem item : items) {
            channelClaimIds.add(item.getChannel().getChannelClaimId());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> options = new HashMap<>();
                    options.put("claim_id", Helper.join(channelClaimIds, ","));
                    JSONArray counts = (JSONArray) Lbryio.parseResponse(Lbryio.call("subscription", "sub_count", options, null));


                    if (channelClaimIds.size() == counts.length()) {
                        Map<String, Integer> result = new HashMap<>();
                        for (int i = 0; i < counts.length(); i++) {
                            result.put(channelClaimIds.get(i), counts.getInt(i));
                        }
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onFollowerCountsLoaded(result);
                            }
                        });
                    }
                } catch (LbryioRequestException | LbryioResponseException | JSONException ex) {
                    // pass
                }
            }
        });
    }

    private void onFollowerCountsLoaded(Map<String, Integer> countMap) {
        if (adapter != null) {
            adapter.updateFollowerCounts(countMap);
        }
    }

    public void showError(String message) {
        View v = getView();
        if (v != null) {
            Snackbar.make(v, message, Snackbar.LENGTH_LONG).
                    setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
        }
    }

    public void showMessage(String message) {
        View v = getView();
        if (v != null) {
            Snackbar.make(v, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
