package com.odysee.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.R;
import com.odysee.app.model.YouTubeSyncItem;
import com.odysee.app.ui.ytsync.YouTubeSyncStatusFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YouTubeSyncItemAdapter extends RecyclerView.Adapter<YouTubeSyncItemAdapter.ViewHolder> {
    private Context context;
    private List<YouTubeSyncItem> items;

    private static Map<String, Integer> transferStateResourceMap = new HashMap<>();
    static {
        transferStateResourceMap.put(YouTubeSyncStatusFragment.TRANSFER_STATE_COMPLETED_TRANSFER, R.string.completed_transfer);
    }

    public YouTubeSyncItemAdapter(List<YouTubeSyncItem> items, Context context) {
        this.items = new ArrayList<>(items);
        this.context = context;
    }

    public void update(List<YouTubeSyncItem> updatedItems) {
        List<YouTubeSyncItem> itemsToAdd = new ArrayList<>();
        for (YouTubeSyncItem updatedItem : updatedItems) {
            if (!items.contains(updatedItem)) {
                itemsToAdd.add(updatedItem);
            } else {
                int itemIndex = items.indexOf(updatedItem);
                items.set(itemIndex, updatedItem);
            }
        }

        items.addAll(itemsToAdd);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_youtube_sync, parent, false);
        return new YouTubeSyncItemAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeSyncItem item = items.get(position);
        YouTubeSyncItem.Channel channel = item.getChannel();

        int bgColor = Helper.generateRandomColorForValue(channel.getChannelClaimId());
        if (bgColor == 0) {
            bgColor = Helper.generateRandomColorForValue(channel.getLbryChannelName());
        }

        String thumbnailUrl = null;
        if (item.getClaim() != null) {
            thumbnailUrl = item.getClaim().getThumbnailUrl(Utils.STREAM_THUMBNAIL_WIDTH, Utils.STREAM_THUMBNAIL_HEIGHT, Utils.STREAM_THUMBNAIL_Q);
        }

        boolean completed = YouTubeSyncStatusFragment.TRANSFER_STATE_COMPLETED_TRANSFER.equalsIgnoreCase(channel.getTransferState());
        boolean ineligible = YouTubeSyncStatusFragment.SYNC_STATUS_ABANDONED.equalsIgnoreCase(channel.getSyncStatus()) && completed;
        boolean synced = YouTubeSyncStatusFragment.SYNC_STATUS_SYNCED.equalsIgnoreCase(channel.getSyncStatus());

        holder.completedArea.setVisibility(completed && !ineligible ? View.VISIBLE : View.GONE);
        holder.pendingArea.setVisibility(!completed ? View.VISIBLE : View.GONE);
        holder.ineligibleArea.setVisibility(ineligible ? View.VISIBLE : View.GONE);

        holder.textIneligible.setText(context.getString(R.string.not_eligible_to_be_synced, channel.getLbryChannelName()));

        holder.noThumbnailView.setVisibility(Helper.isNullOrEmpty(thumbnailUrl) ? View.VISIBLE : View.GONE);
        Helper.setIconViewBackgroundColor(holder.noThumbnailView, bgColor, false, context);
        holder.alphaView.setText(channel.getLbryChannelName().substring(1, 2).toUpperCase());

        holder.textYouTubeChannelName.setText(channel.getYtChannelName());
        holder.textLbryChannelName.setText(channel.getLbryChannelName());
        holder.textTransferStatus.setVisibility(completed ? View.VISIBLE : View.GONE);

        holder.textClaimHandle.setText(context.getString(R.string.claim_your_handle, channel.getLbryChannelName()));
        holder.textPendingStats.setText(context.getString(R.string.sync_stats_message, channel.getTotalVideos(), channel.getTotalSubs()));

        holder.waitProgress.setVisibility(synced ? View.INVISIBLE : View.VISIBLE);
        holder.waitCheck.setVisibility(synced ? View.VISIBLE : View.INVISIBLE);
        holder.waitCheck.setImageResource(synced ? R.drawable.ic_circle_checked : R.drawable.ic_circle_unchecked);

        StringBuilder sb = new StringBuilder(context.getResources().getQuantityString(R.plurals.yt_sync_followers, channel.getFollowerCount(), channel.getFollowerCount()));
        sb.append(" ").append(context.getString(R.string.dot_separator)).append(" ");
        sb.append(context.getResources().getQuantityString(R.plurals.yt_sync_uploads, item.getTotalPublishedVideos(), item.getTotalPublishedVideos()));
        holder.textFollowersAndUploads.setText(sb.toString());

        String transferState = channel.getTransferState();
        holder.textTransferStatus.setText(
                transferStateResourceMap.containsKey(transferState) ?
                context.getString(transferStateResourceMap.get(channel.getTransferState())) : "");
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateFollowerCounts(Map<String, Integer> countMap) {
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            for (int i = 0; i < items.size(); i++) {
                if (entry.getKey().equalsIgnoreCase(items.get(i).getChannel().getChannelClaimId())) {
                    items.get(i).getChannel().setFollowerCount(entry.getValue());
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<YouTubeSyncItem> getClaimableItems() {
        List<YouTubeSyncItem> claimable = new ArrayList<>();
        for (YouTubeSyncItem item : items) {
            YouTubeSyncItem.Channel channel = item.getChannel();
            if (YouTubeSyncStatusFragment.SYNC_STATUS_SYNCED.equalsIgnoreCase(channel.getSyncStatus()) && channel.isTransferable() &&
                    !Arrays.asList(
                            YouTubeSyncStatusFragment.TRANSFER_STATE_TRANSFERRED,
                            YouTubeSyncStatusFragment.TRANSFER_STATE_COMPLETED_TRANSFER).contains(channel.getTransferState().toLowerCase())) {
                claimable.add(item);
            }
        }

        return claimable;
    }

    public List<YouTubeSyncItem> getPendingItems() {
        List<YouTubeSyncItem> pending = new ArrayList<>();
        for (YouTubeSyncItem item : items) {
            if (!YouTubeSyncStatusFragment.TRANSFER_STATE_COMPLETED_TRANSFER.equalsIgnoreCase(item.getChannel().getTransferState())) {
                pending.add(item);
            }
        }

        return pending;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final View completedArea;
        protected final View pendingArea;
        protected final View ineligibleArea;

        protected final ImageView thumbnailView;
        protected final View noThumbnailView;
        protected final TextView alphaView;
        protected final TextView textYouTubeChannelName;
        protected final TextView textLbryChannelName;
        protected final TextView textFollowersAndUploads;
        protected final TextView textTransferStatus;
        protected final TextView textIneligible;

        protected final TextView textClaimHandle;
        protected final TextView textPendingStats;

        protected final ImageView waitCheck;
        protected final ProgressBar waitProgress;
        protected final ImageView claimCheck;

        public ViewHolder(View v) {
            super(v);
            completedArea = v.findViewById(R.id.yt_sync_item_completed_area);
            pendingArea = v.findViewById(R.id.yt_sync_item_pending_area);
            ineligibleArea = v.findViewById(R.id.yt_sync_item_ineligible_area);

            thumbnailView = v.findViewById(R.id.yt_sync_item_thumbnail);
            noThumbnailView = v.findViewById(R.id.yt_sync_item_no_thumbnail);
            alphaView = v.findViewById(R.id.yt_sync_item_thumbnail_alpha);
            textYouTubeChannelName = v.findViewById(R.id.yt_sync_text_yt_channel_name);
            textLbryChannelName = v.findViewById(R.id.yt_sync_text_lbry_channel_name);
            textFollowersAndUploads = v.findViewById(R.id.yt_sync_text_followers_uploads);
            textTransferStatus = v.findViewById(R.id.yt_sync_text_transfer_status);
            textIneligible = v.findViewById(R.id.yt_sync_item_ineligible_text);

            textClaimHandle = v.findViewById(R.id.yt_sync_item_claim_handle_text);
            textPendingStats = v.findViewById(R.id.yt_sync_item_stats_text);

            waitCheck = v.findViewById(R.id.yt_sync_item_wait_check);
            waitProgress = v.findViewById(R.id.yt_sync_item_wait_progress);
            claimCheck = v.findViewById(R.id.yt_sync_item_claim_check);
        }
    }
}
