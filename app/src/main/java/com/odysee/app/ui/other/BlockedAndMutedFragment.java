package com.odysee.app.ui.other;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.SignInActivity;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.Tag;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.wallet.LoadSharedUserStateTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import okhttp3.ResponseBody;

public class BlockedAndMutedFragment extends BaseFragment {
    public static final int BLOCKED_AND_MUTED_CONTEXT_GROUP_ID = 21;
    private static final int FILTER_BLOCKED = 1;
    private static final int FILTER_MUTED = 2;

    private boolean loadInProgress;
    private ProgressBar blockedAndMutedLoading;

    private RecyclerView blockedAndMutedList;
    private ClaimListAdapter adapter;
    private TextView linkFilterBlocked;
    private TextView linkFilterMuted;
    private int currentFilter; // 1 - blocked, 2 - muted

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_blocked_muted, container, false);

        blockedAndMutedLoading = root.findViewById(R.id.blocked_muted_loading);
        blockedAndMutedList = root.findViewById(R.id.blocked_muted_item_list);
        linkFilterBlocked = root.findViewById(R.id.blocked_muted_filter_blocked);
        linkFilterMuted = root.findViewById(R.id.blocked_muted_filter_muted);

        blockedAndMutedList.setLayoutManager(new LinearLayoutManager(getContext()));

        linkFilterBlocked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (loadInProgress) {
                    return;
                }
                updateCurrentFilter(FILTER_BLOCKED);
                loadBlocked();
            }
        });

        linkFilterMuted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (loadInProgress) {
                    return;
                }
                updateCurrentFilter(FILTER_MUTED);
                loadMuted();
            }
        });

        return root;
    }

    private void updateCurrentFilter(int filter) {
        this.currentFilter = filter;
        if (filter == FILTER_BLOCKED) {
            linkFilterBlocked.setTypeface(null, Typeface.BOLD);
            linkFilterMuted.setTypeface(null, Typeface.NORMAL);
        } else if (filter == FILTER_MUTED) {
            linkFilterBlocked.setTypeface(null, Typeface.NORMAL);
            linkFilterMuted.setTypeface(null, Typeface.BOLD);
        }
    }

    public void onResume() {
        super.onResume();
        checkFilter();

        if (currentFilter == FILTER_BLOCKED) {
            loadBlocked();
        } else if (currentFilter == FILTER_MUTED) {
            loadMuted();
        }
    }

    private void checkFilter() {
        if (currentFilter != FILTER_BLOCKED && currentFilter != FILTER_MUTED) {
            updateCurrentFilter(FILTER_BLOCKED);
        }
    }

    private void loadBlocked() {
        if (loadInProgress) {
            return;
        }

        loadInProgress = true;
        Helper.setViewVisibility(blockedAndMutedLoading, View.VISIBLE);
        blockedAndMutedList.setAdapter(null);

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;

            ((OdyseeApp) activity.getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<BlockedChannel> allBlockedChannels = new ArrayList<>();
                        for (Claim modChannel : Lbry.ownChannels) {
                            // perform moderation.BlockedList request for each channel
                            // (a more efficient way to do this is required if a user has a lot of channels)
                            JSONObject params = new JSONObject();
                            params.put("channel_id", modChannel.getClaimId());
                            params.put("channel_name", modChannel.getName());
                            params.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);
                            JSONObject jsonChannelSign = Comments.channelSignName(params, modChannel.getClaimId(), modChannel.getName());

                            Map<String, Object> options = new HashMap<>();
                            options.put("mod_channel_id", modChannel.getClaimId());
                            options.put("mod_channel_name", modChannel.getName());
                            options.put("signature", Helper.getJSONString("signature", "", jsonChannelSign));
                            options.put("signing_ts", Helper.getJSONString("signing_ts", "", jsonChannelSign));
                            options.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);

                            okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "moderation.BlockedList");
                            ResponseBody responseBody = response.body();
                            JSONObject jsonResponse = new JSONObject(responseBody.string());
                            if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                                throw new ApiCallException("invalid json response");
                            }

                            JSONObject result = Helper.getJSONObject("result", jsonResponse);
                            if (!result.has("blocked_channels") || result.isNull("blocked_channels")) {
                                throw new ApiCallException("missing blocked_channels key from json response");
                            }

                            JSONArray items = result.getJSONArray("blocked_channels");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                BlockedChannel channel = new BlockedChannel(
                                    Helper.getJSONString("blocked_channel_id", null, item),
                                        Helper.getJSONString("blocked_channel_name", null, item)
                                );
                                if (!Helper.isNullOrEmpty(channel.getChannelId()) && !Helper.isNullOrEmpty(channel.getName())) {
                                    allBlockedChannels.add(channel);
                                }
                            }
                        }

                        // next we need to resolve the channels
                        List<String> blockedChannelUrls = new ArrayList<>();
                        List<LbryUri> blockedChannelLbryUris = new ArrayList<>();
                        for (BlockedChannel channel : allBlockedChannels) {
                            LbryUri url = LbryUri.tryParse(String.format("lbry://%s:%s", channel.getName(), channel.getChannelId()));
                            if (url != null) {
                                blockedChannelLbryUris.add(url);
                                blockedChannelUrls.add(url.toString());
                            }
                        }

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Lbryio.blockedChannels = new ArrayList<>(blockedChannelLbryUris);
                                didLoadBlockedChannelUrls(blockedChannelUrls);
                            }
                        });
                    } catch (JSONException | ApiCallException | IOException ex) {
                        // pass
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                didFailLoadingBlockedChannels();
                            }
                        });
                    }
                }
            });
        }
    }

    private void loadMuted() {
        if (loadInProgress) {
            return;
        }

        loadInProgress = true;
        Helper.setViewVisibility(blockedAndMutedLoading, View.VISIBLE);
        blockedAndMutedList.setAdapter(null);

        Context context = getContext();
        LoadSharedUserStateTask loadTask = new LoadSharedUserStateTask(context, new LoadSharedUserStateTask.LoadSharedUserStateHandler() {
            @Override
            public void onSuccess(List<Subscription> subscriptions, List<Tag> followedTags, List<LbryUri> mutedChannels,
                                  List<String> editedCollectionClaimIds) {
                Lbryio.subscriptions = new ArrayList<>(subscriptions);
                Lbryio.mutedChannels = new ArrayList<>(mutedChannels);

                List<String> mutedChannelUrls = new ArrayList<>(mutedChannels.size());
                for (LbryUri url : mutedChannels) {
                    mutedChannelUrls.add(url.toString());
                }
                didLoadMutedChannelUrls(mutedChannelUrls);
            }

            @Override
            public void onError(Exception error) {
                didFailLoadingMutedChannels();
            }
        }, Lbryio.AUTH_TOKEN);
        loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void didLoadMutedChannelUrls(List<String> urls) {
        // resolve the urls
        ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, blockedAndMutedLoading, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                didLoadMutedChannels(claims);
            }

            @Override
            public void onError(Exception error) {
                didFailLoadingMutedChannels();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void didLoadBlockedChannelUrls(List<String> urls) {
        // resolve the urls
        ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, blockedAndMutedLoading, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                didLoadBlockedChannels(claims);
            }

            @Override
            public void onError(Exception error) {
                didFailLoadingBlockedChannels();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void didLoadBlockedChannels(List<Claim> claims) {
        loadInProgress = false;
        Helper.setViewVisibility(blockedAndMutedLoading, View.GONE);

        adapter = new ClaimListAdapter(claims, getContext());
        adapter.setContextGroupId(BLOCKED_AND_MUTED_CONTEXT_GROUP_ID);
        blockedAndMutedList.setAdapter(adapter);
    }

    private void didFailLoadingBlockedChannels() {
        loadInProgress = false;
        Helper.setViewVisibility(blockedAndMutedLoading, View.GONE);
        showError(getString(R.string.load_blocked_failed));
    }

    private void didLoadMutedChannels(List<Claim> claims) {
        loadInProgress = false;
        Helper.setViewVisibility(blockedAndMutedLoading, View.GONE);

        adapter = new ClaimListAdapter(claims, getContext());
        adapter.setContextGroupId(BLOCKED_AND_MUTED_CONTEXT_GROUP_ID);
        blockedAndMutedList.setAdapter(adapter);
    }

    private void didFailLoadingMutedChannels() {
        loadInProgress = false;
        Helper.setViewVisibility(blockedAndMutedLoading, View.GONE);
        showError(getString(R.string.load_muted_failed));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == BLOCKED_AND_MUTED_CONTEXT_GROUP_ID && (item.getItemId() == R.id.action_block || item.getItemId() == R.id.action_mute)) {
            if (adapter != null) {
                int position = adapter.getPosition();
                Claim claim = adapter.getItems().get(position);
                if (claim != null && claim.getSigningChannel() != null) {
                    Claim channel = claim.getSigningChannel();
                    boolean isBlocked = Lbryio.isChannelBlocked(channel);
                    boolean isMuted = Lbryio.isChannelMuted(channel);

                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        MainActivity activity = (MainActivity) context;
                        if (item.getItemId() == R.id.action_block) {
                            if (!isBlocked) {
                                activity.handleUnblockChannel(channel, null);
                            } else {
                                activity.handleBlockChannel(channel, null);
                            }
                        } else {
                            if (!isMuted) {
                                activity.handleMuteChannel(channel);
                            } else {
                                activity.handleMuteChannel(channel);
                            }
                        }
                    }
                }
            }
            return true;
        }

        if (item.getGroupId() == BLOCKED_AND_MUTED_CONTEXT_GROUP_ID && item.getItemId() == R.id.action_report) {
            if (adapter != null) {
                int position = adapter.getCurrentPosition();
                Claim claim = adapter.getItems().get(position);
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).handleReportClaim(claim);
                }
            }
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Data
    public static final class BlockedChannel {
        private String channelId;
        private String name;
        public BlockedChannel(String channelId, String name) {
            this.channelId = channelId;
            this.name = name;
        }
    }
}
