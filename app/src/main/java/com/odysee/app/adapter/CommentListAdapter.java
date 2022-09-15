package com.odysee.app.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import java.util.stream.Collectors;
import java.util.Optional;

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
import com.odysee.app.utils.FormatTime;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

import com.odysee.app.utils.Utils;
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

    private List<String> childrenToBeShown;

    private final Claim claim;

    private final CommentListListener commentListListener;

    public CommentListAdapter(List<Comment> items, Context context, Claim claim, CommentListListener commentListListener) {
        this.items = new ArrayList<>(items);
        this.childrenToBeShown = new ArrayList<>();
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
        if (!commentsToRemove.isEmpty()) {
            items.removeAll(commentsToRemove);
            notifyDataSetChanged();
        }
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
        protected final View muteChannelView;
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
            muteChannelView = v.findViewById(R.id.comment_mute_channel);
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
                            if (parentComment.getId().equals(commentToRemove.getId())) {
                                remove = true;
                                break;
                            }
                        } else {
                            break;
                        }

                        parentId = parentComment.getParentId();
                    }
                }

                if (remove) {
                    items.remove(i);
                    childrenToBeShown.remove(commentToRemove.getId());
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
        if (collapsed || (comment.getParentId() != null && !childrenToBeShown.contains(comment.getParentId()))) {
            holder.itemView.setVisibility(View.GONE);
            lp.height = 0;
            lp.width = 0;
            holder.itemView.setLayoutParams(lp);
        } else {
            if (comment.getParentId() == null || (comment.getParentId() != null && childrenToBeShown.contains(comment.getParentId()))) {
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
                if (channel != null && context instanceof MainActivity) {
                    ((MainActivity) context).handleBlockChannel(channel, null);
                }
            }
        });

        holder.muteChannelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim channel = comment.getPoster();
                if (channel != null && context instanceof MainActivity) {
                    ((MainActivity) context).handleMuteChannel(channel);
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
        holder.commentTimeView.setText(FormatTime.fromEpochMillis(comment.getTimestamp() * 1000));
        holder.commentText.setText(comment.getText());

        Reactions commentReactions = comment.getReactions();
        if (commentReactions != null) {
            int countTextColor;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                countTextColor = context.getResources().getColor(R.color.foreground, null);
            } else {
                //noinspection deprecation
                countTextColor = context.getResources().getColor(R.color.foreground);
            }

            String likesAmount = String.valueOf(commentReactions.getOthersLikes());
            String dislikesAmount = String.valueOf(commentReactions.getOthersDislikes());

            if (commentReactions.isLiked()) {
                int fireActive;

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    fireActive = context.getResources().getColor(R.color.fireActive, null);
                } else {
                    //noinspection deprecation
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
                    //noinspection deprecation
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
                if (comment.getClaimId() != null && commentListListener != null) {
                    commentListListener.onCommentReactClicked(comment, true);
                }
            }
        });
        holder.dislikesCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (comment.getClaimId() != null && commentListListener != null) {
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
            Glide.with(context.getApplicationContext()).asBitmap().load(comment.getPoster().getThumbnailUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q)).
                    apply(RequestOptions.circleCropTransform()).into(holder.thumbnailView);
        }
        holder.alphaView.setText(comment.getChannelName() != null ? comment.getChannelName().substring(1, 2).toUpperCase() : null);

        holder.channelName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && comment.getPoster() != null) {
                    listener.onClaimClicked(comment.getPoster(), holder.getBindingAdapterPosition());
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

        if (holder.getAbsoluteAdapterPosition() != (items.size() - 1)) {
            String pId = items.get(holder.getAbsoluteAdapterPosition() + 1).getParentId();
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
        if (childrenToBeShown.contains(comment.getId())) {
            vrt.setText(context.getText(R.string.comment_hide_replies));
        } else {
            vrt.setText(context.getText(R.string.comment_view_replies));
        }
    }

    public void switchExpandedStateUI() {
        switchExpandedStateUI(!collapsed);
    }

    /**
     * Switches from displaying a single comment into showing comment form a full list of level 0 comments
     * @param toCollapsed Set to false if form and list of comments should be displayed, true to show only the single comment
     */
    public void switchExpandedStateUI(boolean toCollapsed) {
        if (collapsed != toCollapsed) {
            collapsed = toCollapsed;
            notifyItemRangeChanged(0, items.size());
        }
    }

    /**
     * Adds the parent ids for the items which should be shown on the recycler view
     * @param parentId IDs of Comments which must be visible
     */
    private void switchRepliesVisibility(String parentId) {
        int firstChild = - 1;
        int lastIndex;

        // By calculating the range of items which will be hidden/displayed and then
        // using it on the notification to the adapter, RecyclerView optimizes the change
        // and also animates it -for free!!!-
        Comment parentComment = items.stream().filter(c -> c.getId().equalsIgnoreCase(parentId)).findFirst().orElse(null);
        int parentPosition = items.indexOf(parentComment);

        List<Comment> directChilds = items.stream().filter(c -> c.getParentId() != null &&  c.getParentId().equalsIgnoreCase(parentId)).collect(Collectors.toList());
        Comment lastChildCandidate = directChilds.get(directChilds.size() - 1);

        lastIndex = items.indexOf(lastChildCandidate);
        if (directChilds.size() > 1) {
            String lastChildCandidateId = lastChildCandidate.getId();
            List<Comment> lastGlobalChildCandidates = items.stream().filter(c -> {
                if (c.getParentId() != null && c.getParentId().equalsIgnoreCase(lastChildCandidateId)) {
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());

            if (lastGlobalChildCandidates.size() > 0) {
                // Last child candidate has more childs
                boolean isLast = false;
                String candidateParentId = lastGlobalChildCandidates.get(lastGlobalChildCandidates.size() - 1).getId();

                while (!isLast) {
                    String finalCandidateParentId = candidateParentId;
                    List<Comment> candidates = items.stream().filter(c -> c.getParentId() != null && c.getParentId().equalsIgnoreCase(finalCandidateParentId)).collect(Collectors.toList());
                    isLast = containsLastChild(candidates);

                    if (!isLast) {
                        candidateParentId = candidates.get(candidates.size() - 1).getId();
                    }
                }

                String finalCandidateId = candidateParentId;
                Comment lastComment = items.stream().filter(c -> c.getId().equalsIgnoreCase(finalCandidateId)).findFirst().orElse(null);
                lastIndex = items.indexOf(lastComment);
            }
        }

        Comment firstChildComment = items.stream().filter(c -> (c.getParentId() != null && c.getParentId().equalsIgnoreCase(parentId))).findFirst().orElse(null);

        if (firstChildComment != null) {
            firstChild = items.indexOf(firstChildComment);
        }

        // childrenToBeShown contains a list of parentIds.
        // RecyclerView will display any item which parentId was contained on childrenToBeShown
        if (!childrenToBeShown.contains(parentId)) {
            childrenToBeShown.add(parentId);
        } else {
            childrenToBeShown.remove(parentId);

            // Also remove child parentIds from the list so child replies are also collapsed
            for (int i = firstChild; i < lastIndex; i++) {
                childrenToBeShown.remove(items.get(i).getParentId());
            }
        }
        notifyItemRangeChanged(parentPosition, lastIndex - parentPosition + 1);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    private boolean containsLastChild(List<Comment> comments) {
        return comments.size() == 0;
    }

    /**
     * Expands only the full path of parent comments to the comment with the specified hash
     * @param commentHash Hash of comment of child which needs to be shown
     */
    public void collapseExceptHash(String commentHash) {
        if (items != null) {
            childrenToBeShown = getParentIdsToExpand(items, commentHash);
            notifyItemRangeChanged(0, items.size());
        }
    }

    private List<String> getParentIdsToExpand(List<Comment> comments, String commentId) {
        List<String> commentsToExpandList = new ArrayList<>();

        if (comments != null) {
            Optional<Comment> opt = comments.stream().filter(p -> p.getId().equalsIgnoreCase(commentId)).findFirst();
            String cid;

            if (opt.isPresent()) {
                cid = opt.get().getParentId();

                while (cid != null) {
                    commentsToExpandList.add(cid);
                    String finalCid = cid;
                    Optional<Comment> optional = comments.stream().filter(p -> p.getId().equalsIgnoreCase(finalCid)).findFirst();
                    if (optional.isPresent()) {
                        Comment comment = optional.get();
                        cid = comment.getParentId();
                    }
                }
            }

        }
        return commentsToExpandList;
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
