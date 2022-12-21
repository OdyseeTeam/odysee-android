package com.odysee.app.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.odysee.app.R;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.lbryinc.LbryNotification;
import com.odysee.app.ui.controls.SolidIconView;
import com.odysee.app.utils.FormatTime;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.ImageCDNUrl;
import com.odysee.app.utils.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {

    private static final String RULE_CREATOR_SUBSCRIBER = "creator_subscriber";
    private static final String RULE_COMMENT = "comment";

    private final Context context;
    private final List<LbryNotification> items;
    private final List<LbryNotification> selectedItems;
    @Setter
    private NotificationClickListener clickListener;
    @Getter
    @Setter
    private boolean inSelectionMode;
    @Setter
    private SelectionModeListener selectionModeListener;

    public NotificationListAdapter(List<LbryNotification> notifications, Context context) {
        this.context = context;
        this.items = new ArrayList<>(notifications);
        this.selectedItems = new ArrayList<>();
        Collections.sort(items, Collections.reverseOrder(new LbryNotification()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final View layoutView;
        protected final TextView titleView;
        protected final TextView bodyView;
        protected final TextView timeView;
        protected final SolidIconView iconView;
        protected final ImageView authorThumbnailView;
        protected final ImageView claimThumbnailView;
        protected final View selectedOverlayView;
        public ViewHolder(View v) {
            super(v);
            layoutView = v.findViewById(R.id.notification_layout);
            titleView = v.findViewById(R.id.notification_title);
            bodyView = v.findViewById(R.id.notification_body);
            timeView = v.findViewById(R.id.notification_time);
            iconView = v.findViewById(R.id.notification_icon);
            authorThumbnailView = v.findViewById(R.id.notification_author_thumbnail);
            claimThumbnailView = v.findViewById(R.id.notification_claim_thumbnail);
            selectedOverlayView = v.findViewById(R.id.notification_selected_overlay);
        }
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    public List<LbryNotification> getSelectedItems() {
        return this.selectedItems;
    }
    public int getSelectedCount() {
        return selectedItems != null ? selectedItems.size() : 0;
    }
    public void clearSelectedItems() {
        this.selectedItems.clear();
    }
    public boolean isNotificationSelected(LbryNotification notification) {
        return selectedItems.contains(notification);
    }

    public void insertNotification(LbryNotification notification, int index) {
        if (!items.contains(notification)) {
            items.add(index, notification);
        }
        notifyDataSetChanged();
    }

    public void addNotification(LbryNotification notification) {
        if (!items.contains(notification)) {
            items.add(notification);
        }
        notifyDataSetChanged();
    }
    public void removeNotifications(List<LbryNotification> notifications) {
        for (LbryNotification notification : notifications) {
            items.remove(notification);
        }
        notifyDataSetChanged();
    }

    public void clearNotifications() {
        // Using a for-each loop will throw an exception, so a simple for should be used
        for (Iterator<LbryNotification> iterator = getItems().iterator(); iterator.hasNext();) {
            LbryNotification notification = iterator.next();
            iterator.remove();
        }

        notifyDataSetChanged();
    }

    public List<String> getAuthorUrls() {
        List<String> urls = new ArrayList<>();
        for (LbryNotification item : items) {
            if (item.getCommentAuthor() != null) {
                String authorUrl = item.getCommentAuthor().getPermanentUrl();
                urls.add(authorUrl);
            }
        }
        return urls;
    }

    public void updateAuthorClaims(List<Claim> claims) {
        for (Claim claim : claims) {
            if (claim != null && claim.getThumbnailUrl() != null) {
                updateClaimForAuthorUrl(claim);
            }
        }
        notifyDataSetChanged();
    }

    private void updateClaimForAuthorUrl(Claim claim) {
        for (LbryNotification item : items) {
            if (claim.getPermanentUrl().equalsIgnoreCase(item.getAuthorThumbnailUrl())) {
                item.setCommentAuthor(claim);
            }
        }
    }

    public void addNotifications(List<LbryNotification> notifications) {
        for (LbryNotification notification : notifications) {
            if (!items.contains(notification)) {
                items.add(notification);
            }
        }
        Collections.sort(items, Collections.reverseOrder(new LbryNotification()));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_notification, root, false);
        return new NotificationListAdapter.ViewHolder(v);
    }

    private int getStringIdForRule(String rule) {
        if (RULE_CREATOR_SUBSCRIBER.equalsIgnoreCase(rule)) {
            return R.string.fa_heart;
        }
        if (RULE_COMMENT.equalsIgnoreCase(rule)) {
            return R.string.fa_comment_alt;
        }
        return R.string.fa_asterisk;
    }

    private int getColorForRule(String rule) {
        if (RULE_CREATOR_SUBSCRIBER.equalsIgnoreCase(rule)) {
            return Color.RED;
        }
        if (RULE_COMMENT.equalsIgnoreCase(rule)) {
            return ContextCompat.getColor(context, R.color.statsImage);
        }

        return ContextCompat.getColor(context, R.color.odyseePink);
    }

    @Override
    public void onBindViewHolder(NotificationListAdapter.ViewHolder vh, int position) {
        LbryNotification notification = items.get(position);
        vh.layoutView.setBackgroundColor(ContextCompat.getColor(context, notification.isRead() ? android.R.color.transparent : R.color.odyseePinkSemiTransparent));
        vh.selectedOverlayView.setVisibility(isNotificationSelected(notification) ? View.VISIBLE : View.GONE);

        vh.titleView.setVisibility(!Helper.isNullOrEmpty(notification.getTitle()) ? View.VISIBLE : View.GONE);
        vh.titleView.setText(notification.getTitle());
        vh.bodyView.setText(notification.getDescription());
        vh.timeView.setText(FormatTime.fromEpochMillis(notification.getTimestamp().getTime()));

        vh.authorThumbnailView.setVisibility(notification.getCommentAuthor() == null || notification.getAuthorThumbnailUrl() == null ? View.INVISIBLE : View.VISIBLE);
        if (notification.getAuthorThumbnailUrl() != null) {
            vh.authorThumbnailView.setVisibility(View.VISIBLE);
        }
        if (notification.getCommentAuthor() != null || notification.getAuthorThumbnailUrl() != null) {
            String turl;

            if (notification.getCommentAuthor() != null) {
                turl = notification.getCommentAuthor().getThumbnailUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q);
            } else {
                ImageCDNUrl imageCDNUrl = new ImageCDNUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q, null, notification.getAuthorThumbnailUrl());
                turl = imageCDNUrl.toString();
            }

            Glide.with(context.getApplicationContext()).load(turl).apply(RequestOptions.circleCropTransform()).into(vh.authorThumbnailView);
        }

        if (notification.getClaimThumbnailUrl() != null) {
            vh.bodyView.getLayoutParams().width = (int) (200 * context.getApplicationContext().getResources().getDisplayMetrics().density);
            Configuration config = context.getApplicationContext().getResources().getConfiguration();
            if (config.smallestScreenWidthDp > 359) {
                ImageCDNUrl imageCDNUrl = new ImageCDNUrl(Utils.STREAM_THUMBNAIL_WIDTH, Utils.STREAM_THUMBNAIL_HEIGHT, Utils.STREAM_THUMBNAIL_Q, null, notification.getClaimThumbnailUrl());

                String turl = imageCDNUrl.toString();

                Glide.with(context.getApplicationContext()).asBitmap().load(turl).into(vh.claimThumbnailView);
                vh.claimThumbnailView.setVisibility(View.VISIBLE);
            } else {
                vh.claimThumbnailView.setVisibility(View.GONE);
            }
        } else {
            vh.bodyView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            vh.claimThumbnailView.setVisibility(View.GONE);
        }

        vh.iconView.setVisibility(notification.getCommentAuthor() != null || notification.getAuthorThumbnailUrl() != null ? View.INVISIBLE : View.VISIBLE);
        vh.iconView.setText(getStringIdForRule(notification.getRule()));
        vh.iconView.setTextColor(getColorForRule(notification.getRule()));

        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inSelectionMode) {
                    toggleSelectedNotification(items.get(vh.getAbsoluteAdapterPosition()));
                } else {
                    if (clickListener != null) {
                        clickListener.onNotificationClicked(items.get(vh.getAbsoluteAdapterPosition()));
                    }
                    items.get(vh.getAbsoluteAdapterPosition()).setRead(true);
                    items.get(vh.getAbsoluteAdapterPosition()).setSeen(true);
                    notifyItemChanged(vh.getAbsoluteAdapterPosition());
                }
            }
        });
        vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!inSelectionMode) {
                    inSelectionMode = true;
                    if (selectionModeListener != null) {
                        selectionModeListener.onEnterSelectionMode();
                    }
                }
                toggleSelectedNotification(items.get(vh.getAbsoluteAdapterPosition()));
                return true;
            }
        });
    }

    private void toggleSelectedNotification(LbryNotification notification) {
        if (selectedItems.contains(notification)) {
            selectedItems.remove(notification);
        } else {
            selectedItems.add(notification);
        }

        if (selectionModeListener != null) {
            selectionModeListener.onItemSelectionToggled();
        }

        if (selectedItems.size() == 0) {
            inSelectionMode = false;
            if (selectionModeListener != null) {
                selectionModeListener.onExitSelectionMode();
            }
        }

        notifyDataSetChanged();
    }

    public interface NotificationClickListener {
        void onNotificationClicked(LbryNotification notification);
    }
}
