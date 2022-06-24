package com.odysee.app.adapter;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.model.Comment;
import com.odysee.app.utils.LbryUri;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ChatMessageListAdapter extends RecyclerView.Adapter<ChatMessageListAdapter.ViewHolder> {
    protected final List<Comment> items;

    @Getter
    private final Context context;

    public ChatMessageListAdapter(List<Comment> items, Context context) {
        this.items = new ArrayList<>(items);
        this.context = context;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void addMessage(Comment message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final TextView textMessage;

        public ViewHolder(View v) {
            super(v);
            textMessage = v.findViewById(R.id.chat_message_text);
            textMessage.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public ChatMessageListAdapter.ViewHolder onCreateViewHolder(ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_chat_message, root, false);
        return new ChatMessageListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ChatMessageListAdapter.ViewHolder vh, int position) {
        Comment message = items.get(position);
        vh.textMessage.setText(message.getChatLine(context));
    }
}
