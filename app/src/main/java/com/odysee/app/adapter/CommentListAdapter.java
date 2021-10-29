package com.odysee.app.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import com.odysee.app.R;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.Comment;
import com.odysee.app.model.Reactions;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

import lombok.Setter;

public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {
    protected final List<Comment> items;
    private final Context context;
    @Setter
    private ClaimListAdapter.ClaimListItemListener listener;
    @Setter
    public Boolean collapsed = true;
    @Setter
    private ReplyClickListener replyListener;
    @Setter
    private ReactClickListener reactListener;
    private List<String> childsToBeShown;

    public CommentListAdapter(List<Comment> items, Context context) {
        this.items = new ArrayList<>(items);
        this.childsToBeShown= new ArrayList<>();
        this.context = context;

        for (Comment item : this.items) {
            ClaimCacheKey key = new ClaimCacheKey();
            key.setClaimId(item.getChannelId());
            if (Lbry.claimCache.containsKey(key)) {
                item.setPoster(Lbry.claimCache.get(key));
            }
        }
    }

    public void clearItems() {
        int previousSize = items.size();
        items.clear();
        notifyItemRangeRemoved(0, previousSize);
    }


    public int getPositionForComment(String commentHash) {
        for (int i = 0; i < items.size(); i++) {
            if (commentHash.equalsIgnoreCase(items.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public List<String> getClaimUrlsToResolve() {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Comment item = items.get(i);
            if (item.getPoster() == null) {
                LbryUri url = LbryUri.tryParse(String.format("%s#%s", item.getChannelName(), item.getChannelId()));
                if (url != null && !urls.contains(url.toString())) {
                    urls.add(url.toString());
                }
            }
        }
        return urls;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        protected final TextView channelName;
        protected final TextView commentText;
        protected final ImageView thumbnailView;
        protected final View noThumbnailView;
        protected final TextView alphaView;
        protected final TextView commentTimeView;
        protected final TextView likesCount;
        protected final TextView dislikesCount;
        protected final View replyLink;
        protected final View commentActions;
        protected final View viewReplies;

        public ViewHolder (View v) {
            super(v);
            channelName = v.findViewById(R.id.comment_channel_name);
            commentTimeView = v.findViewById(R.id.comment_time);
            commentText = v.findViewById(R.id.comment_text);
            replyLink = v.findViewById(R.id.comment_reply_link);
            likesCount = v.findViewById(R.id.comment_likes_count);
            dislikesCount = v.findViewById(R.id.comment_dislikes_count);
            thumbnailView = v.findViewById(R.id.comment_thumbnail);
            noThumbnailView = v.findViewById(R.id.comment_no_thumbnail);
            alphaView = v.findViewById(R.id.comment_thumbnail_alpha);
            commentActions = v.findViewById(R.id.comment_actions_area);
            viewReplies = v.findViewById(R.id.textview_view_replies);
        }
    }

    public void insert(int index, Comment comment) {
        if (!items.contains(comment)) {
            items.add(index, comment);
            notifyItemInserted(index);
        }
    }

    public void addReply(Comment comment) {
        Comment c = items.stream().filter(v -> comment.getParentId().equalsIgnoreCase(v.getId())).findFirst().orElse(null);

        if (c != null) {
            int positionToInsert = items.indexOf(c) + 1;
            items.add(positionToInsert, comment);
            notifyItemInserted(positionToInsert);
        }
    }

    public void updateReactions(Comment comment, Reactions reactions) {
        if (items.contains(comment)) {
            items.get(getPositionForComment(comment.getId())).setReactions(reactions);
            notifyItemChanged(getPositionForComment(comment.getId()));
        }
    }

    public void updatePosterForComment(String channelId, Claim channel) {
        for (Comment c : items) {
            if (channelId.equalsIgnoreCase(c.getChannelId())) {
                c.setPoster(channel);
            }
        }
    }

    @NonNull
    @Override
    public  ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_comment, parent, false);
        return new CommentListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = items.get(position);

        switchViewReplies(comment, (TextView) holder.viewReplies);

        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (collapsed || (comment.getParentId() != null && !childsToBeShown.contains(comment.getParentId()))) {
            holder.itemView.setVisibility(View.GONE);
            lp.height = 0;
            lp.width = 0;
            holder.itemView.setLayoutParams(lp);
        } else {
            if (comment.getParentId() == null || (comment.getParentId() != null && childsToBeShown.contains(comment.getParentId()))) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                holder.itemView.setLayoutParams(lp);
                holder.itemView.setVisibility(View.VISIBLE);
            }
        }

        holder.channelName.setText(comment.getChannelName());
        holder.commentTimeView.setText(DateUtils.getRelativeTimeSpanString(
                (comment.getTimestamp() * 1000), System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));
        holder.commentText.setText(comment.getText());

        Reactions commentReactions = comment.getReactions();
        if (commentReactions != null) {
            int countTextColor;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                countTextColor = context.getResources().getColor(R.color.foreground, null);
            } else {
                countTextColor = context.getResources().getColor(R.color.foreground);
            }

            String likesAmount = String.valueOf(commentReactions.getOthersLikes());
            String dislikesAmount = String.valueOf(commentReactions.getOthersDislikes());

            if (commentReactions.isLiked()) {
                int fireActive;

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    fireActive = context.getResources().getColor(R.color.fireActive, null);
                } else {
                    fireActive = context.getResources().getColor(R.color.fireActive);
                }

                holder.likesCount.setText(String.valueOf(Integer.parseInt(likesAmount) + 1));
                holder.likesCount.setTextColor(fireActive);

                for (Drawable d: holder.likesCount.getCompoundDrawablesRelative()) {
                    if (d != null) {
                        d.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(holder.likesCount.getContext(), R.color.fireActive), PorterDuff.Mode.SRC_IN));
                    }
                }
            } else {
                holder.likesCount.setText(likesAmount);
                holder.likesCount.setTextColor(countTextColor);

                for (Drawable d: holder.likesCount.getCompoundDrawablesRelative()) {
                    if (d != null) {
                        d.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(holder.likesCount.getContext(), R.color.foreground), PorterDuff.Mode.SRC_IN));
                    }
                }
            }

            if (commentReactions.isDisliked()) {
                int slimeActive;

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    slimeActive = context.getResources().getColor(R.color.slimeActive, null);
                } else {
                    slimeActive = context.getResources().getColor(R.color.slimeActive);
                }

                holder.dislikesCount.setText(String.valueOf(Integer.parseInt(dislikesAmount) + 1));
                holder.dislikesCount.setTextColor(slimeActive);
                for (Drawable d: holder.dislikesCount.getCompoundDrawablesRelative()) {
                    if (d != null) {
                        d.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(holder.dislikesCount.getContext(), R.color.slimeActive), PorterDuff.Mode.SRC_IN));
                    }
                }
            } else {
                holder.dislikesCount.setText(dislikesAmount);
                holder.dislikesCount.setTextColor(countTextColor);

                for (Drawable d: holder.dislikesCount.getCompoundDrawablesRelative()) {
                    if (d != null) {
                        d.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(holder.dislikesCount.getContext(), R.color.foreground), PorterDuff.Mode.SRC_IN));
                    }
                }
            }
        }

        holder.likesCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager am = AccountManager.get(context);
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                if (odyseeAccount != null && comment.getClaimId() != null && reactListener != null) {
                    reactListener.onCommentReactClicked(comment, true);
                }
            }
        });
        holder.dislikesCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager am = AccountManager.get(context);
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                if (odyseeAccount != null && comment.getClaimId() != null && reactListener != null) {
                    reactListener.onCommentReactClicked(comment, false);
                }
            }
        });

        boolean hasThumbnail = comment.getPoster() != null && !Helper.isNullOrEmpty(comment.getPoster().getThumbnailUrl());
        holder.thumbnailView.setVisibility(hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        holder.noThumbnailView.setVisibility(!hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        int bgColor = Helper.generateRandomColorForValue(comment.getChannelId());
        Helper.setIconViewBackgroundColor(holder.noThumbnailView, bgColor, false, context);
        if (hasThumbnail) {
            Glide.with(context.getApplicationContext()).asBitmap().load(comment.getPoster().getThumbnailUrl(holder.thumbnailView.getLayoutParams().width, holder.thumbnailView.getLayoutParams().height, 85)).
                    apply(RequestOptions.circleCropTransform()).into(holder.thumbnailView);
        }
        holder.alphaView.setText(comment.getChannelName() != null ? comment.getChannelName().substring(1, 2).toUpperCase() : null);

        holder.channelName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && comment.getPoster() != null) {
                    listener.onClaimClicked(comment.getPoster());
                }
            }
        });

        holder.replyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (replyListener != null) {
                    replyListener.onReplyClicked(comment);
                }
            }
        });

        if (position != (items.size() - 1)) {
            String pId = items.get(position + 1).getParentId();
            if (pId != null && pId.equalsIgnoreCase(comment.getId())) {
                holder.viewReplies.setVisibility(View.VISIBLE);
                 holder.viewReplies.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchRepliesVisibility(comment.getId());
                        switchViewReplies(comment, (TextView) holder.viewReplies);
                    }
                 });
            } else {
                holder.viewReplies.setVisibility(View.GONE);
            }
        }
    }

    private void switchViewReplies(Comment comment, TextView vrt) {
        if (childsToBeShown.contains(comment.getId())) {
            vrt.setText(context.getText(R.string.comment_hide_replies));
        } else {
            vrt.setText(context.getText(R.string.comment_view_replies));
        }
    }

    public void switchExpandedState() {
        collapsed = !collapsed;
        notifyItemRangeChanged(0, items.size());
    }

    /**
     * Adds the parent ids for the items which should be shown on the recycler view
     * @param parentId IDs of Comments which must be visible
     */
    private void switchRepliesVisibility(String parentId) {
        int firstChild = - 1;
        int lastIndex = -1;

        // By calculating the range of items which will be hidden/displayed and then
        // using it on the notification to the adapter, RecyclerView optimizes the change
        // and also animates it -for free!!!-
        for (Comment c : items) {
            if (c.getParentId() != null && c.getParentId().equalsIgnoreCase(parentId)) {
                if (firstChild == -1) {
                    firstChild = items.indexOf(c);
                    lastIndex = firstChild + 1;
                } else {
                    lastIndex = items.indexOf(c) + 1;
                }
            }
        }

        if (!childsToBeShown.contains(parentId)) {
            childsToBeShown.add(parentId);
        } else {
            childsToBeShown.remove(parentId);

            // Also remove child parentIds from the list so child replies are also collpased
            for (int i = firstChild; i < lastIndex; i++) {
                childsToBeShown.remove(items.get(i).getParentId());
            }
        }
        notifyItemRangeChanged(firstChild, lastIndex - firstChild);
    }

    public interface ReplyClickListener {
        void onReplyClicked(Comment comment);
    }

    public interface ReactClickListener {
        void onCommentReactClicked(Comment c, boolean liked);
    }
}