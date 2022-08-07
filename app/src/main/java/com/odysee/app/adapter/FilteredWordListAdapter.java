package com.odysee.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.odysee.app.R;
import com.odysee.app.model.lbryinc.Invitee;

import lombok.Setter;

public class FilteredWordListAdapter extends RecyclerView.Adapter<FilteredWordListAdapter.ViewHolder> {
    private final Context context;
    private final List<String> items;
    @Setter
    private RemoveItemListener removeItemListener;

    public FilteredWordListAdapter(List<String> words, Context context) {
        this.context = context;
        this.items = new ArrayList<>(words);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    public void addWords(List<String> words) {
        for (String word : words) {
            if (!items.contains(word)) {
                items.add(word);
            }
        }
        notifyDataSetChanged();
    }

    public void removeWord(String word) {
        items.remove(word);
        notifyDataSetChanged();
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public FilteredWordListAdapter.ViewHolder onCreateViewHolder(ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_text_deletable, root, false);
        return new FilteredWordListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FilteredWordListAdapter.ViewHolder vh, int position) {
        String item = items.get(position);
        vh.textView.setText(item);
        vh.closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (removeItemListener != null) {
                    removeItemListener.onRemoveItem(item);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final TextView textView;
        protected final View closeView;

        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.item_name);
            closeView = v.findViewById(R.id.action_delete);
        }
    }

    public interface RemoveItemListener {
        void onRemoveItem(String value);
    }
}

