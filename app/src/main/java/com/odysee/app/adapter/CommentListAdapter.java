package com.odysee.app.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.Comment;
import com.odysee.app.model.Reactions;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

import lombok.Getter;
import lombok.Setter;

public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {

    protected final List<Comment> items;

    @Getter
    private final Context context;

    @Setter
    private ClaimListAdapter.ClaimListItemListener listener;
    @Getter
    @Setter
    private Boolean collapsed = true;

    private List<String> childsToBeShown;

    private final Claim claim;

    private final CommentListListener commentListListener;

    public CommentListAdapter(List<Comment> items, Context context, Claim claim, CommentListListener commentListListener) {
        this.items = new ArrayList<>(items);
        this.childsToBeShown= new ArrayList<>();
        this.context = context;
        this.commentListListener = commentListListener;

        for (Comment item : this.items) {
            ClaimCacheKey key = new ClaimCacheKey();
            key.setClaimId(item.getChannelId());
            if (Lbry.claimCache.containsKey(key)) {
                item.setPoster(Lbry.claimCache.get(key));
            }
        }

        this.claim = claim;

        this.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                commentListListener.onListChanged();
            }
        });
    }

    public void showError(String message) {
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).showError(message);
        }
    }

    public void clearItems() {
        int previousSize = items.size();
        items.clear();
        notifyItemRangeRemoved(0, previousSize);
    }


    public int getPositionForComment(String commentId) {
        for (int i = 0; i < items.size(); i++) {
            if (commentId.equalsIgnoreCase(items.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private Comment getCommentForId(final String commentId) {
        final int position = getPositionForComment(commentId);

        if ( position >= 0 ) {
            return items.get(position);
        } else {
            return null;
        }
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

    public void filterBlockedChannels(List<LbryUri> blockedChannels) {
        if (blockedChannels.size() == 0) {
            return;
        }
        List<Comment> commentsToRemove = new ArrayList<>();
        List<String> blockedChannelClaimIds = new ArrayList<>();
        for (LbryUri uri : blockedChannels) {
            blockedChannelClaimIds.add(uri.getClaimId());
        }
        for (Comment comment : items) {
            if (comment.getPoster() != null && blockedChannelClaimIds.contains(comment.getPoster().getClaimId())) {
                commentsToRemove.add(comment);
            }
        }
        items.removeAll(commentsToRemove);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
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
        protected final View blockChannelView;
        protected final View moreOptionsView;

        private final CommentListAdapter adapter;
        protected Comment comment = null;

        public ViewHolder (View v, CommentListAdapter adapter) {
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
            blockChannelView = v.findViewById(R.id.comment_block_channel);
            viewReplies = v.findViewById(R.id.textview_view_replies);
            moreOptionsView = v.findViewById(R.id.comment_more_options);

            this.adapter = adapter;

            v.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            if ( this.comment != null ) {
                final int contextGroupId = 0;
                final MenuItem.OnMenuItemClickListener clickListener = item -> {
                    final CommentAction commentAction = CommentAction.fromActionId(item.getItemId());

                    if ( commentAction != null ) {
                        commentAction.performAction(adapter, comment);

                        return true;
                    } else {
                        return false;
                    }
                };

                for ( final CommentAction ithAction : CommentAction.values() ) {
                    if ( ithAction.isAvailable(this.comment, this.adapter.claim) ) {
                        final MenuItem menuItem = contextMenu.add(contextGroupId, ithAction.actionId, Menu.NONE, ithAction.stringId);
                        menuItem.setIcon(ithAction.iconId);
                        menuItem.setOnMenuItemClickListener(clickListener);
                    }
                }
            }
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

    public void updateCommentText(Comment comment, final String text) {
        if (items.contains(comment)) {
            final int position = getPositionForComment(comment.getId());
            items.get(position).setText(text);
            notifyItemChanged(position);
        }
    }

    /**
     * Removes the given comment and any children from the tree.
     */
    public void removeComment(final Comment commentToRemove) {

        if (items.contains(commentToRemove)) {

            for ( int i = items.size()-1; i >= 0; i-- ) {
                final Comment ith = items.get(i);

                boolean remove = false;

                if ( ith.getId().equals(commentToRemove.getId()) ) {
                    remove = true;
                } else {
                    // Have to travel up the potential reply chain to remove all children.
                    String parentId = ith.getParentId();

                    while ( parentId != null ) {
                        final Comment parentComment = getCommentForId(parentId);

                        if ( parentComment != null ) {
                            if ( parentComment.getId() == commentToRemove.getId() ) {
                                remove = true;
                                break;
                            }
                        } else {
                            break;
                        }

                        parentId = parentComment.getParentId();
                    }
                }

                if ( remove == true ) {
                    items.remove(i);
                    childsToBeShown.remove(commentToRemove.getId());
                }
            }

            notifyDataSetChanged();

            /**
             * For some reason getting out of bounds exception in {@link CommentItemDecoration} so doing the blanket {@link #notifyDataSetChanged()}.
             */
//            notifyItemRemoved(indexOfComment);
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
        return new CommentListAdapter.ViewHolder(v, this);
    }

    @Override
    public void onViewRecycled(CommentListAdapter.ViewHolder holder) {
        if (holder.moreOptionsView != null) {
            holder.moreOptionsView.setOnClickListener(null);
        }
        super.onViewRecycled(holder);
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

        holder.comment = comment;

        holder.blockChannelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim channel = comment.getPoster();
                if (channel != null) {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).handleBlockChannel(channel);
                    }
                }
            }
        });

        if ( CommentAction.areAnyActionsAvailable(comment, claim) ) {
            holder.moreOptionsView.setVisibility(collapsed ? View.INVISIBLE : View.VISIBLE);

            holder.moreOptionsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.showContextMenu();
                }
            });
        } else {
            holder.moreOptionsView.setVisibility(View.INVISIBLE);
            holder.moreOptionsView.setOnClickListener(null);
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
                if (odyseeAccount != null && comment.getClaimId() != null && commentListListener != null) {
                    commentListListener.onCommentReactClicked(comment, true);
                }
            }
        });
        holder.dislikesCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager am = AccountManager.get(context);
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                if (odyseeAccount != null && comment.getClaimId() != null && commentListListener != null) {
                    commentListListener.onCommentReactClicked(comment, false);
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
                    listener.onClaimClicked(comment.getPoster(), position);
                }
            }
        });

        holder.replyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (commentListListener != null) {
                    commentListListener.onReplyClicked(comment);
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
        } else {
            holder.viewReplies.setVisibility(View.GONE);
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

    public boolean isCollapsed() {
        return collapsed;
    }

    public interface CommentListListener {
        /**
         * This is fed by {@link RecyclerView.AdapterDataObserver#onChanged()}.
         * Callers could use {@link #registerAdapterDataObserver(RecyclerView.AdapterDataObserver)}
         * directly but since {@link CommentListAdapter} is used in two places I want to be very explicit about the
         * contract that this adapter requires, and enforce things through the constructor.
         */
        void onListChanged();

        void onCommentReactClicked(Comment c, boolean liked);

        void onReplyClicked(Comment comment);
    }
}