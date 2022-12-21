package com.odysee.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.odysee.app.R;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.ImageCDNUrl;
import com.odysee.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Setter;

public class PlaylistCollectionListAdapter extends RecyclerView.Adapter<PlaylistCollectionListAdapter.ViewHolder> {
    private final Context context;
    private List<OdyseeCollection> items;

    @Setter
    private ClickListener listener;

    public PlaylistCollectionListAdapter(List<OdyseeCollection> collections, Context context) {
        this.context = context;
        this.items = new ArrayList<>(collections);
    }

    public void clear() {
        int itemsSize = items.size();
        items.clear();
        notifyItemRangeRemoved(0, itemsSize);
    }

    public List<OdyseeCollection> getItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateCollectionThumbnailUrls(Map<String, String> collectionIdThumbnailUrlMap) {
        for (int i = 0; i < items.size(); i++) {
            OdyseeCollection collection = items.get(i);
            String collectionId = collection.getId();
            if (collectionIdThumbnailUrlMap.containsKey(collectionId)) {
                collection.setFirstItemThumbnailUrl(collectionIdThumbnailUrlMap.get(collectionId));
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public PlaylistCollectionListAdapter.ViewHolder onCreateViewHolder(ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_playlist, root, false);
        return new PlaylistCollectionListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PlaylistCollectionListAdapter.ViewHolder vh, int position) {
        final OdyseeCollection item = items.get(position);

        if (OdyseeCollection.BUILT_IN_ID_WATCHLATER.equalsIgnoreCase(item.getId())) {
            vh.iconView.setImageResource(R.drawable.ic_watch_later);
        } else if (OdyseeCollection.BUILT_IN_ID_FAVORITES.equalsIgnoreCase(item.getId())) {
            vh.iconView.setImageResource(R.drawable.ic_favorites);
        } else if (item.getVisibility() == OdyseeCollection.VISIBILITY_PUBLIC) {
            vh.iconView.setImageResource(R.drawable.ic_public);
        } else {
            vh.iconView.setImageResource(R.drawable.ic_private);
        }

        vh.titleView.setText(item.getName());

        int videoCount = item.getItems().size();
        vh.videoCountView.setText(context.getResources().getQuantityString(R.plurals.video_count, videoCount, videoCount));

        if (item.isNewPlaceholder()) {
            //vh.thumbnailView.setImageResource(R.drawable.ic_add);
        } else {
            String thumbnailUrl = item.getThumbnailUrl();
            if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                ImageCDNUrl thumbnailCDNUrl = new ImageCDNUrl(
                        Utils.STREAM_THUMBNAIL_WIDTH,
                        Utils.STREAM_THUMBNAIL_HEIGHT, Utils.STREAM_THUMBNAIL_Q, null, thumbnailUrl);
                Glide.with(context.getApplicationContext()).
                        asBitmap().
                        load(thumbnailCDNUrl.toString()).
                        centerCrop().
                        into(vh.thumbnailView);
            }
        }

        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onClick(item, position);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final ImageView thumbnailView;
        protected final ImageView iconView;
        protected final TextView titleView;
        protected final TextView videoCountView;

        public ViewHolder(View v) {
            super(v);
            thumbnailView = v.findViewById(R.id.playlist_item_thumbnail);
            iconView = v.findViewById(R.id.playlist_item_icon);
            titleView = v.findViewById(R.id.playlist_item_title);
            videoCountView = v.findViewById(R.id.playlist_item_video_count);
        }
    }

    public interface ClickListener {
        void onClick(OdyseeCollection collection, int position);
    }
}

