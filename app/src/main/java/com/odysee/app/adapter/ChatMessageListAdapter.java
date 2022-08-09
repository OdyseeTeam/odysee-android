package com.odysee.app.adapter;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.odysee.app.R;
import com.odysee.app.model.Comment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ChatMessageListAdapter extends RecyclerView.Adapter<ChatMessageListAdapter.ViewHolder> {
    protected final List<Comment> items;
    @Setter
    private String streamerClaimId;
    private boolean displayMessages;
    private final float scale;

    private static final int CONTAINER_PADDING_REGULAR = 0;
    private static final int CONTAINER_PADDING_HYPERCHAT = 2;

    @Getter
    private final Context context;

    public ChatMessageListAdapter(List<Comment> items, Context context) {
        this.items = new ArrayList<>(items);
        this.context = context;
        this.scale = context.getResources().getDisplayMetrics().density;
        this.displayMessages = true;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void setDisplayMessages(boolean displayMessages) {
        this.displayMessages = displayMessages;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void addMessage(Comment message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final View hyperchatHeader;
        protected final View creditsIcon;
        protected final TextView hyperchatValue;
        protected final View highlightContainer;
        protected final View textContainer;
        protected final TextView textMessage;

        public ViewHolder(View v) {
            super(v);
            hyperchatHeader = v.findViewById(R.id.hyperchat_message_header);
            creditsIcon = v.findViewById(R.id.hyperchat_credits_icon);
            hyperchatValue = v.findViewById(R.id.hyperchat_value);
            highlightContainer = v.findViewById(R.id.chat_message_text_container_highlight);
            textContainer = v.findViewById(R.id.chat_message_text_container);

            textMessage = v.findViewById(R.id.chat_message_text);
            textMessage.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @NonNull
    @Override
    public ChatMessageListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup root, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_chat_message, root, false);
        return new ChatMessageListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageListAdapter.ViewHolder vh, int position) {
        ViewGroup.LayoutParams lp = vh.itemView.getLayoutParams();

        if (displayMessages) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            vh.itemView.setLayoutParams(lp);
            vh.itemView.setVisibility(View.VISIBLE);

            Comment message = items.get(position);
            boolean isHyperchat = message.isHyperchat();

            Helper.setViewVisibility(vh.hyperchatHeader, isHyperchat ? View.VISIBLE : View.GONE);
            vh.highlightContainer.setBackgroundColor(ContextCompat.getColor(context, isHyperchat && displayMessages? R.color.colorPrimary : android.R.color.transparent));
            vh.textContainer.setBackgroundColor(ContextCompat.getColor(context, isHyperchat && displayMessages ? R.color.semiTransparentPageBackground : android.R.color.transparent));

            int containerPadding = Helper.getScaledValue(isHyperchat ? CONTAINER_PADDING_HYPERCHAT : CONTAINER_PADDING_REGULAR,  scale);
            vh.highlightContainer.setPadding(containerPadding, containerPadding, containerPadding, containerPadding);
            vh.textContainer.setPadding(0, containerPadding, 0, containerPadding);

            Helper.setViewVisibility(vh.creditsIcon, message.isFiat() ? View.GONE : View.VISIBLE);
            vh.hyperchatValue.setText(message.getHyperchatValue());

            vh.textMessage.setText(Comments.getChatLine(
                    message,
                    streamerClaimId,
                    message.getHandler(),
                    vh.textMessage,
                    context));
        } else {
            vh.itemView.setVisibility(View.GONE);
            lp.height = 0;
            lp.width = 0;
            vh.itemView.setLayoutParams(lp);
        }
    }
}
