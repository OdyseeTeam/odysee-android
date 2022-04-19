package com.odysee.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.odysee.app.R;
import com.odysee.app.model.OdyseeCollection;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

public class CollectionListAdapter extends RecyclerView.Adapter<CollectionListAdapter.ViewHolder> {
    private final Context context;
    private List<OdyseeCollection> items;

    @Setter
    private CollectionListItemCheckChangedListener listener;

    public CollectionListAdapter(List<OdyseeCollection> collections, Context context) {
        this.context = context;
        this.items = new ArrayList<>(collections);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public List<OdyseeCollection> getItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public CollectionListAdapter.ViewHolder onCreateViewHolder(ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_playlist_edit, root, false);
        return new CollectionListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(CollectionListAdapter.ViewHolder vh, int position) {
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

        vh.checkBox.setText(item.getName());
        vh.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (listener != null) {
                    listener.onCheckedChanged(item, checked);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final MaterialCheckBox checkBox;
        protected final ImageView iconView;

        public ViewHolder(View v) {
            super(v);
            checkBox = v.findViewById(R.id.playlist_item_checkbox);
            iconView = v.findViewById(R.id.playlist_item_icon);
        }
    }

    public interface CollectionListItemCheckChangedListener {
        void onCheckedChanged(OdyseeCollection collection, boolean checked);
    }
}
