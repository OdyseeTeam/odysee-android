package com.odysee.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.odysee.app.R;
import com.odysee.app.listener.ChannelItemSelectionListener;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ChannelWithThumbnailListSpinnerAdapter extends ArrayAdapter<Claim> {
    private List<Claim> items;
    @Setter
    private ChannelItemSelectionListener listener;

    private final int layoutResourceId;
    private final LayoutInflater inflater;

    public ChannelWithThumbnailListSpinnerAdapter(Context context, int resource, List<Claim> channels) {
        super(context, resource, 0, channels);
        inflater = LayoutInflater.from(context);
        layoutResourceId = resource;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final ImageView thumbnailView;
        protected final TextView titleView;

        public ViewHolder(View v) {
            super(v);
            thumbnailView = v.findViewById(R.id.channel_name);
            titleView = v.findViewById(R.id.channel_name);
        }
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void clearClaims() {
        items = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addClaims(List<Claim> claims) {
        for (Claim claim : claims) {
            if (!items.contains(claim)) {
                items.add(claim);
            }
        }
        notifyDataSetChanged();
    }

    public int getItemPosition(Claim item) {
        for (int i = 0; i < items.size(); i++) {
            Claim channel = items.get(i);
            if (item.getClaimId() != null && item.getClaimId().equalsIgnoreCase(channel.getClaimId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        return createView(position, view, parent);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        return createView(position, view, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(layoutResourceId, parent, false);

        Context context = getContext();
        Claim claim = getItem(position);

        TextView label = view.findViewById(R.id.channel_name);
        ImageView thumbnailView = view.findViewById(R.id.channel_thumbnail);

        label.setText(Helper.isNullOrEmpty(claim.getTitle()) ? claim.getName() : claim.getTitle());
        String thumbnailUrl = claim.getThumbnailUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q);
        if (context != null) {
            if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                Glide.with(context.getApplicationContext()).
                        load(thumbnailUrl).
                        apply(RequestOptions.circleCropTransform()).
                        into(thumbnailView);
            } else {
                Glide.with(context.getApplicationContext()).
                        load(R.drawable.spaceman).
                        apply(RequestOptions.circleCropTransform()).
                        into(thumbnailView);
            }
        }
        int bgColor = Helper.generateRandomColorForValue(claim.getClaimId());
        thumbnailView.setColorFilter(Helper.isNullOrEmpty(thumbnailUrl) ? bgColor : android.R.color.transparent);

        return view;
    }
}

