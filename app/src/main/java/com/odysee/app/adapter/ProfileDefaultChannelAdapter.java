package com.odysee.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.odysee.app.R;

import java.util.List;

public class ProfileDefaultChannelAdapter extends BaseAdapter {
    private final List<String> list;
    private int defaultItem;
    private final LayoutInflater inflater;

    public ProfileDefaultChannelAdapter(Context ctx, List<String> rows) {
        this.list = rows;
        this.inflater = LayoutInflater.from(ctx);
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.list_item_profile_popup_channel, viewGroup, false);
        }

        ImageView selectedImage = view.findViewById(R.id.selected_image);

        if (defaultItem == i) {
            selectedImage.setVisibility(View.VISIBLE);
        } else {
            selectedImage.setVisibility(View.INVISIBLE);
        }

        TextView channelName = view.findViewById(R.id.popup_channel_name);
        channelName.setText(list.get(i));
        return view;
    }

    public void setDefaultChannelName(String name) {
        defaultItem = list.indexOf(name);
        notifyDataSetChanged();
    }
}
