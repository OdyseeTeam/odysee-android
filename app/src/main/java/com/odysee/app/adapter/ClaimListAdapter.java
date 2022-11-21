package com.odysee.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.CompactDecimalFormat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.LbryFile;
import com.odysee.app.ui.other.BlockedAndMutedFragment;
import com.odysee.app.utils.FormatTime;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Utils;
import lombok.Getter;
import lombok.Setter;

public class ClaimListAdapter extends RecyclerView.Adapter<ClaimListAdapter.ViewHolder> {
    private static final int VIEW_TYPE_STREAM = 1;
    private static final int VIEW_TYPE_CHANNEL = 2;
    private static final int VIEW_TYPE_FEATURED = 3; // featured search result
    private static final int VIEW_TYPE_LIVESTREAM = 4; // featured search result

    public static final int STYLE_BIG_LIST = 1;
    public static final int STYLE_SMALL_LIST = 2;
    public static final int STYLE_SMALL_LIST_HORIZONTAL = 3;

    private float scale;

    @Getter
    @Setter
    private boolean inPlaylistOverlay;

    @Getter
    @Setter
    private int contextGroupId;
    @Getter
    @Setter
    private int style;

    private final Map<String, Claim> quickClaimIdMap;
    private final Map<String, Claim> quickClaimUrlMap;
    private final Map<String, Boolean> notFoundClaimIdMap;
    private final Map<String, Boolean> notFoundClaimUrlMap;

    @Setter
    private boolean hideFee;
    @Setter
    private boolean canEnterSelectionMode;
    private final Context context;
    private List<Claim> items;
    private final List<Claim> selectedItems;
    @Setter
    private ClaimListItemListener listener;
    @Setter
    private DelegatedUnblockListener delegatedUnblockListener;
    @Getter
    @Setter
    private boolean inSelectionMode;
    @Setter
    private SelectionModeListener selectionModeListener;
    @Getter
    @Setter
    private int position;
    @Getter
    @Setter
    private int currentPosition;
    @Setter
    private boolean isOwnCollection;

    public ClaimListAdapter(List<Claim> items, Context context) {
        this(items, STYLE_BIG_LIST, context);
    }

    @Getter
    @Setter
    private Map<String, Claim> blockedOnBehalfOfChannelMap;

    public ClaimListAdapter(List<Claim> items, int style, Context context) {
        this.context = context;
        this.style = style;
        List<Claim> sortedItems = Helper.sortingLivestreamingFirst(items);
        this.items = new ArrayList<>();
        this.blockedOnBehalfOfChannelMap = new HashMap<>();
        for (Claim item : sortedItems) {
            if (item != null) {
                this.items.add(item);
            }
        }

        this.selectedItems = new ArrayList<>();
        quickClaimIdMap = new HashMap<>();
        quickClaimUrlMap = new HashMap<>();
        notFoundClaimIdMap = new HashMap<>();
        notFoundClaimUrlMap = new HashMap<>();
    }

    public void setBlockedChannelInfoForClaim(String claimId, Claim blockedChannel) {
        blockedOnBehalfOfChannelMap.put(claimId, blockedChannel);
    }

    public List<Claim> getSelectedItems() {
        return this.selectedItems;
    }
    public int getSelectedCount() {
        return selectedItems != null ? selectedItems.size() : 0;
    }
    public void clearSelectedItems() {
        this.selectedItems.clear();
    }
    public boolean isClaimSelected(Claim claim) {
        return selectedItems.contains(claim);
    }

    public List<Claim> getUnderlyingItems() {
        return this.items;
    }

    public List<Claim> getItems() {
        return new ArrayList<>(this.items);
    }

    public void updateSigningChannelForClaim(Claim resolvedClaim) {
        for (Claim claim : items) {
            if (claim.getClaimId().equalsIgnoreCase(resolvedClaim.getClaimId())) {
                claim.setSigningChannel(resolvedClaim.getSigningChannel());
            }
        }
    }

    public void filterBlockedChannels(List<LbryUri> blockedChannels) {
        if (blockedChannels.size() == 0) {
            return;
        }
        List<String> blockedChannelClaimIds = new ArrayList<>();
        for (LbryUri uri : blockedChannels) {
            blockedChannelClaimIds.add(uri.getClaimId());
        }
        for (Claim claim : items) {
            if (claim.getSigningChannel() != null && blockedChannelClaimIds.contains(claim.getSigningChannel().getClaimId())) {
                int position = items.indexOf(claim);
                items.remove(claim);
                notifyItemRemoved(position);
            }
        }
    }

    public void clearItems() {
        int itemsSize = items.size();
        clearSelectedItems();
        this.items.clear();
        quickClaimIdMap.clear();
        quickClaimUrlMap.clear();
        notFoundClaimIdMap.clear();
        notFoundClaimUrlMap.clear();
        notifyItemRangeRemoved(0, itemsSize);
    }

    public Claim getLastItem() {
        return items.size() > 0 ? items.get(items.size() - 1) : null;
    }

    public void addFeaturedItem(Claim claim) {
        items.add(0, claim);
        notifyItemInserted(0);
    }

    public void addSingleItem(Claim claim) {
        items.add(claim);
        notifyItemInserted(items.size() - 1);
    }

    public void addItems(List<Claim> claims) {
        for (Claim claim : claims) {
            if (claim != null) {
                boolean alreadyAdded = items.stream().anyMatch(p -> p.getClaimId() != null && p.getClaimId().equalsIgnoreCase(claim.getClaimId()));
                if (claim.getLivestreamUrl() != null) {
                    if (!alreadyAdded) {
                        // Determine first claim which is not livestreaming
                        Claim c = items.stream().filter(v -> v != null && v.getLivestreamUrl() == null).findFirst().orElse(null);

                        // Insert livestreaming one before first item which is not a livestream
                        if (c != null) {
                            int position = items.indexOf(c);
                            int positionToInsert = position > 0 ? position - 1 : 0;
                            items.add(positionToInsert, claim);
                            notifyItemInserted(positionToInsert);
                        } else {
                            // There is no item on the list of items which is not a livestream
                            items.add(claim);
                            notifyItemInserted(items.size());
                        }
                    }
                } else {
                    if (!alreadyAdded) {
                        items.add(claim);
                        notifyItemInserted(items.size() - 1);
                    }
                }
            }
        }

        notFoundClaimUrlMap.clear();
        notFoundClaimIdMap.clear();
    }

    public void setItems(List<Claim> claims) {
        if (items.size() > 0)
            notifyItemRangeRemoved(0, items.size());

        items = new ArrayList<>();
        for (Claim claim : claims) {
            if (claim != null) {
                items.add(claim);
            }
        }
        notifyItemRangeInserted(0, items.size());
    }

    public void setItem(String claimId, Claim claim) {
        if (claim != null) {
            Claim r = this.items.stream().filter(o -> o.getClaimId() != null && o.getClaimId().equalsIgnoreCase(claimId)).findFirst().orElse(null);

            if (r != null) {
                int position = this.items.indexOf(r);
                items.set(this.items.indexOf(r), claim);
                notifyItemChanged(position, claim);
            }
        }
    }
    public void removeItems(List<Claim> claims) {
        items.removeAll(claims);
        notifyDataSetChanged();
    }

    public void removeItem(Claim claim) {
        int position = items.indexOf(claim);
        items.remove(claim);
        selectedItems.remove(claim);
        notifyItemRemoved(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        @Getter
        @Setter
        private int contextGroupId;

        protected final View feeContainer;
        protected final TextView feeView;
        protected final ImageView thumbnailView;
        protected final View playbackProgressView;
        protected final View noThumbnailView;
        protected final TextView alphaView;
        protected final TextView vanityUrlView;
        protected final TextView durationView;
        protected final TextView titleView;
        protected final ImageView publisherThumbnailView;
        protected final TextView publisherView;
        protected final TextView publishTimeView;
        protected final TextView pendingTextView;
        protected final View repostInfoView;
        protected final TextView repostChannelView;
        protected final View repostedLabel;
        protected final View selectedOverlayView;
        protected final TextView viewCountView;
        protected final TextView deviceView;
        protected final ImageButton optionsMenuView;

        protected final View onBehalfOfContainer;
        protected final TextView onBehalfOfTitleView;
        protected final TextView onBehalfOfNameView;
        protected final MaterialButton onBehalfOfUnblockButton;
        protected final ImageView onBehalfOfThumbnailView;
        protected final View onBehalfOfNoThumbnailView;
        protected final TextView onBehalfOfAlphaView;

        protected final View loadingImagePlaceholder;
        protected final View loadingTextPlaceholder1;
        protected final View loadingTextPlaceholder2;
        public ViewHolder(View v) {
            super(v);
            feeContainer = v.findViewById(R.id.claim_fee_container);
            feeView = v.findViewById(R.id.claim_fee);
            alphaView = v.findViewById(R.id.claim_thumbnail_alpha);
            noThumbnailView = v.findViewById(R.id.claim_no_thumbnail);
            thumbnailView = v.findViewById(R.id.claim_thumbnail);
            playbackProgressView = v.findViewById(R.id.playback_progress_view);
            vanityUrlView = v.findViewById(R.id.claim_vanity_url);
            durationView = v.findViewById(R.id.claim_duration);
            titleView = v.findViewById(R.id.claim_title);
            publisherThumbnailView = v.findViewById(R.id.claim_publisher_thumbnail);
            publisherView = v.findViewById(R.id.claim_publisher);
            publishTimeView = v.findViewById(R.id.claim_publish_time);
            pendingTextView = v.findViewById(R.id.claim_pending_text);
            repostInfoView = v.findViewById(R.id.claim_repost_info);
            repostChannelView = v.findViewById(R.id.claim_repost_channel);
            repostedLabel = v.findViewById(R.id.reposted_label);
            selectedOverlayView = v.findViewById(R.id.claim_selected_overlay);
            viewCountView = v.findViewById(R.id.claim_view_count);
            deviceView = v.findViewById(R.id.claim_view_device);
            optionsMenuView = v.findViewById(R.id.claim_overflow_menu_icon);

            loadingImagePlaceholder = v.findViewById(R.id.claim_thumbnail_placeholder);
            loadingTextPlaceholder1 = v.findViewById(R.id.claim_text_loading_placeholder_1);
            loadingTextPlaceholder2 = v.findViewById(R.id.claim_text_loading_placeholder_2);

            onBehalfOfContainer = v.findViewById(R.id.moderator_on_behalf_of_container);
            onBehalfOfTitleView = v.findViewById(R.id.on_behalf_of_title);
            onBehalfOfNameView = v.findViewById(R.id.on_behalf_of_name);
            onBehalfOfThumbnailView = v.findViewById(R.id.on_behalf_of_thumbnail);
            onBehalfOfUnblockButton = v.findViewById(R.id.on_behalf_of_unblock_button);
            onBehalfOfNoThumbnailView = v.findViewById(R.id.on_behalf_of_no_thumbnail);
            onBehalfOfAlphaView = v.findViewById(R.id.on_behalf_of_thumbnail_alpha);

            v.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter = getBindingAdapter();
            boolean isBlocked = false;
            boolean isMuted = false;
            if (adapter instanceof ClaimListAdapter) {
                ClaimListAdapter claimListAdapter = ((ClaimListAdapter) adapter);
                final Claim original = claimListAdapter.getItems().get(getAbsoluteAdapterPosition());
                final Claim item = Claim.TYPE_REPOST.equalsIgnoreCase(original.getValueType()) ?
                        (original.getRepostedClaim() != null ? original.getRepostedClaim() : original): original;

                if (contextGroupId != BlockedAndMutedFragment.BLOCKED_AND_MUTED_CONTEXT_GROUP_ID) {
                    if (claimListAdapter.isOwnCollection) {
                        contextMenu.add(contextGroupId, R.id.action_remove_from_list, Menu.NONE, R.string.remove_from_list);
                    } else if (Claim.TYPE_STREAM.equalsIgnoreCase(item.getValueType()) && item.isPlayable()) {
                        contextMenu.add(contextGroupId, R.id.action_add_to_queue, Menu.NONE, R.string.add_to_queue);
                        contextMenu.add(contextGroupId, R.id.action_add_to_watch_later, Menu.NONE, R.string.watch_later);
                        contextMenu.add(contextGroupId, R.id.action_add_to_favorites, Menu.NONE, R.string.favorites);
                        contextMenu.add(contextGroupId, R.id.action_add_to_lists, Menu.NONE, R.string.add_to_lists);
                    }
                }

                isBlocked =  Lbryio.isChannelBlocked(item.isChannel() ? item : item.getSigningChannel());
                isMuted = Lbryio.isChannelMuted(item.isChannel() ? item : item.getSigningChannel());
            }

            contextMenu.add(contextGroupId, R.id.action_block, Menu.NONE, isBlocked ? R.string.unblock_channel : R.string.block_channel);
            contextMenu.add(contextGroupId, R.id.action_mute, Menu.NONE, isMuted ? R.string.unmute_channel : R.string.mute_channel);
            contextMenu.add(contextGroupId, R.id.action_report, Menu.NONE, R.string.report);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position).isFeatured()) {
            return VIEW_TYPE_FEATURED;
        } else if (items.get(position).isLive() || items.get(position).isHighlightLive()) {
            return VIEW_TYPE_LIVESTREAM;
        }

        Claim claim = items.get(position);
        String valueType = items.get(position).getValueType();
        Claim actualClaim = Claim.TYPE_REPOST.equalsIgnoreCase(valueType) ? claim.getRepostedClaim() : claim;

        return Claim.TYPE_CHANNEL.equalsIgnoreCase(actualClaim.getValueType()) ? VIEW_TYPE_CHANNEL : VIEW_TYPE_STREAM;
    }

    public void updateFileForClaimByIdOrUrl(LbryFile file, String claimId, String url) {
        updateFileForClaimByIdOrUrl(file, claimId, url,  false);
    }
    public void updateFileForClaimByIdOrUrl(LbryFile file, String claimId, String url, boolean skipNotFound) {
        if (!skipNotFound) {
            if (notFoundClaimIdMap.containsKey(claimId) && notFoundClaimUrlMap.containsKey(url)) {
                return;
            }
        }
        if (quickClaimIdMap.containsKey(claimId)) {
            quickClaimIdMap.get(claimId).setFile(file);
            notifyDataSetChanged();
            return;
        }
        if (quickClaimUrlMap.containsKey(claimId)) {
            quickClaimUrlMap.get(claimId).setFile(file);
            notifyDataSetChanged();
            return;
        }

        boolean claimFound = false;
        for (int i = 0; i < items.size(); i++) {
            Claim claim = items.get(i);
            if (claimId.equalsIgnoreCase(claim.getClaimId()) || url.equalsIgnoreCase(claim.getPermanentUrl())) {
                quickClaimIdMap.put(claimId, claim);
                quickClaimUrlMap.put(url, claim);
                claim.setFile(file);
                notifyItemChanged(i);
                claimFound = true;
                break;
            }
        }

        if (!claimFound) {
            notFoundClaimIdMap.put(claimId, true);
            notFoundClaimUrlMap.put(url, true);
        }
    }
    public void clearFileForClaimOrUrl(String outpoint, String url) {
        clearFileForClaimOrUrl(outpoint, url, false);
        notifyDataSetChanged();
    }


    public void clearFileForClaimOrUrl(String outpoint, String url, boolean remove) {
        int claimIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            Claim claim = items.get(i);
            if (outpoint.equalsIgnoreCase(claim.getOutpoint()) || url.equalsIgnoreCase(claim.getPermanentUrl())) {
                claimIndex = i;
                claim.setFile(null);
                break;
            }
        }
        if (remove && claimIndex > -1) {
            Claim removed = items.remove(claimIndex);
            selectedItems.remove(removed);
        }

        notifyDataSetChanged();
    }

    @Override
    public ClaimListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int viewResourceId = -1;
        switch (viewType) {
            case VIEW_TYPE_FEATURED: viewResourceId = R.layout.list_item_featured_search_result; break;
            case VIEW_TYPE_CHANNEL: viewResourceId = R.layout.list_item_channel; break;
            case VIEW_TYPE_STREAM:
                default:
                    switch (style) {
                        case STYLE_BIG_LIST: default: viewResourceId = R.layout.list_item_stream; break;
                        case STYLE_SMALL_LIST: viewResourceId = R.layout.list_item_small_stream; break;
                        case STYLE_SMALL_LIST_HORIZONTAL: viewResourceId = R.layout.list_item_small_stream_horizontal; break;
                    }
        }

        View v = LayoutInflater.from(context).inflate(viewResourceId, parent, false);
        return new ClaimListAdapter.ViewHolder(v);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        if (holder.optionsMenuView != null) {
            holder.optionsMenuView.setOnClickListener(null);
        }
        super.onViewRecycled(holder);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(ClaimListAdapter.ViewHolder vh, int position) {
        int type = getItemViewType(position);

        if (style == STYLE_SMALL_LIST) {
            int paddingTop = vh.getAbsoluteAdapterPosition() == 0 ? 16 : 8;
            int paddingBottom = vh.getAbsoluteAdapterPosition() == getItemCount() - 1 ? 16 : 8;
            int paddingTopScaled = Helper.getScaledValue(paddingTop, scale);
            int paddingBottomScaled = Helper.getScaledValue(paddingBottom, scale);
            vh.itemView.setPadding(vh.itemView.getPaddingStart(), paddingTopScaled, vh.itemView.getPaddingEnd(), paddingBottomScaled);
        } else if (style == STYLE_SMALL_LIST_HORIZONTAL) {
            /*int paddingStart = vh.getAbsoluteAdapterPosition() == 0 ? 16 : 8;
            int paddingEnd = vh.getAbsoluteAdapterPosition() == getItemCount() - 1 ? 16 : 8;
            int paddingStartScaled = Helper.getScaledValue(paddingStart, scale);
            int paddingEndScaled = Helper.getScaledValue(paddingEnd, scale);*/
            vh.itemView.setPadding(vh.itemView.getPaddingStart(), vh.itemView.getPaddingTop(), vh.itemView.getPaddingEnd(), vh.itemView.getPaddingBottom());
        }

        Claim original = items.get(vh.getAbsoluteAdapterPosition());
        boolean isRepost = Claim.TYPE_REPOST.equalsIgnoreCase(original.getValueType());
        final Claim item = Claim.TYPE_REPOST.equalsIgnoreCase(original.getValueType()) ?
                (original.getRepostedClaim() != null ? original.getRepostedClaim() : original): original;
        Claim.GenericMetadata metadata = item.getValue();
        Claim signingChannel = item.getSigningChannel();
        Claim.StreamMetadata streamMetadata = null;
        if (metadata instanceof Claim.StreamMetadata) {
            streamMetadata = (Claim.StreamMetadata) metadata;
        }

        int thumbnailWidth;
        int thumbnailHeight;
        int thumbnailQ;
        if (Claim.TYPE_CHANNEL.equalsIgnoreCase(item.getValueType())) {
            thumbnailWidth = Utils.CHANNEL_THUMBNAIL_WIDTH;
            thumbnailHeight = Utils.CHANNEL_THUMBNAIL_HEIGHT;
            thumbnailQ = Utils.CHANNEL_THUMBNAIL_Q;
        } else {
            thumbnailWidth = Utils.STREAM_THUMBNAIL_WIDTH;
            thumbnailHeight = Utils.STREAM_THUMBNAIL_HEIGHT;
            thumbnailQ = Utils.STREAM_THUMBNAIL_Q;
        }
        String thumbnailUrl = item.getThumbnailUrl(thumbnailWidth, thumbnailHeight, thumbnailQ);
        long publishTime = (streamMetadata != null && streamMetadata.getReleaseTime() > 0) ? streamMetadata.getReleaseTime() * 1000 : item.getTimestamp() * 1000;
        int bgColor = Helper.generateRandomColorForValue(item.getClaimId());
        if (bgColor == 0) {
            bgColor = Helper.generateRandomColorForValue(item.getName());
        }

        boolean isPending = item.getConfirmations() == 0;
        boolean isSelected = isClaimSelected(original);
        vh.itemView.setSelected(isSelected);
        vh.setContextGroupId(contextGroupId);
        vh.selectedOverlayView.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inSelectionMode) {
                    toggleSelectedClaim(original);
                } else if (isPending) {
                    Snackbar snackbar = Snackbar.make(vh.itemView, R.string.item_pending_blockchain, Snackbar.LENGTH_LONG);
                    TextView snackbarText = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                    snackbarText.setMaxLines(5);
                    snackbar.show();
                } else {
                    if (listener != null) {
                        listener.onClaimClicked(item, vh.getAbsoluteAdapterPosition());
                    }
                }
            }
        });
        vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                setCurrentPosition(vh.getAbsoluteAdapterPosition());

                if (!canEnterSelectionMode) {
                    return false;
                }

                if (!inSelectionMode) {
                    inSelectionMode = true;
                    if (selectionModeListener != null) {
                        selectionModeListener.onEnterSelectionMode();
                    }
                }

                toggleSelectedClaim(original);
                return true;
            }
        });

        vh.publisherView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null && signingChannel != null) {
                    listener.onClaimClicked(signingChannel, vh.getAbsoluteAdapterPosition());
                }
            }
        });

        vh.publishTimeView.setVisibility(!isPending && style != STYLE_SMALL_LIST_HORIZONTAL ? View.VISIBLE : View.GONE);
        vh.pendingTextView.setVisibility(isPending && !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        vh.repostInfoView.setVisibility(isRepost ? View.VISIBLE : View.GONE);
        if (type != VIEW_TYPE_FEATURED) {
            vh.repostChannelView.setText(isRepost && original.getSigningChannel() != null ? original.getSigningChannel().getName() : null);
            vh.repostChannelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onClaimClicked(original.getSigningChannel(), vh.getAbsoluteAdapterPosition());
                    }
                }
            });
        } else {
            vh.repostedLabel.setVisibility(View.GONE);
            vh.repostChannelView.setText(original.getShortUrl());
        }

        if (vh.optionsMenuView != null) {
            vh.optionsMenuView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setPosition(vh.getAbsoluteAdapterPosition());
                    setCurrentPosition(vh.getAbsoluteAdapterPosition());
                    view.showContextMenu();
                }
            });
        }

        vh.titleView.setText(Helper.isNullOrEmpty(item.getTitle()) ? item.getName() : item.getTitle());
        if (type == VIEW_TYPE_FEATURED) {
            LbryUri vanityUrl = new LbryUri();
            vanityUrl.setClaimName(item.getName());
            vh.vanityUrlView.setText(vanityUrl.toString());
        }

        vh.feeContainer.setVisibility(item.isUnresolved() || !Claim.TYPE_STREAM.equalsIgnoreCase(item.getValueType()) ? View.GONE : View.VISIBLE);
        vh.noThumbnailView.setVisibility(Helper.isNullOrEmpty(thumbnailUrl) ? View.VISIBLE : View.GONE);
        Helper.setIconViewBackgroundColor(vh.noThumbnailView, bgColor, false, context);

        Helper.setViewVisibility(vh.loadingImagePlaceholder, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.loadingTextPlaceholder1, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.loadingTextPlaceholder2, item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.titleView, !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.publisherView, !item.isLoadingPlaceholder() ? View.VISIBLE : View.GONE);
        Helper.setViewVisibility(vh.publishTimeView, !item.isLoadingPlaceholder() && !isPending && style != STYLE_SMALL_LIST_HORIZONTAL ? View.VISIBLE : View.GONE);

        if (type == VIEW_TYPE_FEATURED && item.isUnresolved()) {
            vh.durationView.setVisibility(View.GONE);
            vh.publisherView.setVisibility(View.GONE);
            vh.publishTimeView.setVisibility(View.GONE);
            vh.thumbnailView.setVisibility(View.GONE);
            vh.titleView.setText("Nothing here. Publish something!");
            String name = item.getName();
            if (!Helper.isNullOrEmpty(name)) {
                vh.alphaView.setText(name.substring(0, Math.min(5, name.length() - 1)));
            }
        } else {
            ViewGroup.LayoutParams lp = vh.itemView.getLayoutParams();
            if (Claim.TYPE_STREAM.equalsIgnoreCase(item.getValueType()) || Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) {
                if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                    Glide.with(context.getApplicationContext()).
                            asBitmap().
                            load(thumbnailUrl).
                            placeholder(R.drawable.bg_thumbnail_placeholder).
                            into(vh.thumbnailView);
                    vh.thumbnailView.setVisibility(View.VISIBLE);
                } else {
                    vh.thumbnailView.setVisibility(View.GONE);
                }

                if (vh.publisherThumbnailView != null) {
                    if (item.getSigningChannel() != null) {
                        String publisherThumbnailUrl = item.getSigningChannel().getThumbnailUrl(Utils.CHANNEL_THUMBNAIL_WIDTH, Utils.CHANNEL_THUMBNAIL_HEIGHT, Utils.CHANNEL_THUMBNAIL_Q);
                        if (!Helper.isNullOrEmpty(publisherThumbnailUrl)) {
                            Glide.with(context.getApplicationContext())
                                    .load(publisherThumbnailUrl)
                                    .centerCrop()
                                    .placeholder(R.drawable.bg_thumbnail_placeholder)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(vh.publisherThumbnailView);
                        } else {
                            vh.publisherThumbnailView.setVisibility(View.GONE);
                        }
                    } else {
                        vh.publisherThumbnailView.setVisibility(View.GONE);
                    }
                }

                BigDecimal cost = item.getActualCost(Lbryio.LBCUSDRate);
                vh.feeContainer.setVisibility(cost.doubleValue() > 0 && !hideFee ? View.VISIBLE : View.GONE);
                vh.feeView.setText(cost.doubleValue() > 0 ? Helper.shortCurrencyFormat(cost.doubleValue()) : "Paid");
                vh.alphaView.setText(item.getName().substring(0, Math.min(5, item.getName().length() - 1)));
                vh.publisherView.setText(signingChannel != null ? signingChannel.getTitleOrName() : context.getString(R.string.anonymous));
                vh.publishTimeView.setText(FormatTime.fromEpochMillis(publishTime));
                if (vh.viewCountView != null) {
                    vh.viewCountView.setVisibility((item.getViews() != null && item.getViews() != 0) ? View.VISIBLE : View.GONE);
                    vh.viewCountView.setText(item.getViews() != null ? context.getResources().getQuantityString(
                                                                   R.plurals.view_count, item.getViews(), compactNumber(item.getViews())) + " •" : null);
                }
                long duration = item.getDuration();
                vh.durationView.setVisibility((duration > 0 || item.isHighlightLive() || Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) ? View.VISIBLE : View.GONE);
                long lastPlaybackPosition = loadLastPlaybackPosition(item);
                if (lastPlaybackPosition != -1 && duration > 0) {
                    long lastPlaybackPositionSeconds = lastPlaybackPosition / 1000;
                    vh.thumbnailView.getViewTreeObserver()
                            .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    int width = vh.thumbnailView.getMeasuredWidth();
                                    if (width > 0) {
                                        Helper.setViewWidth(vh.playbackProgressView,
                                                (int) (((double) lastPlaybackPositionSeconds / duration) * width));
                                        vh.thumbnailView.getViewTreeObserver().removeOnPreDrawListener(this);
                                    }
                                    return true;
                                }
                            });
                } else {
                    Helper.setViewWidth(vh.playbackProgressView, 0);
                }
                if (type == VIEW_TYPE_LIVESTREAM) {
                    vh.durationView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));

                    Date ct = new Date();
                    Calendar cal = GregorianCalendar.getInstance(); // locale-specific
                    cal.setTime(ct);

                    long nowTime = cal.getTimeInMillis() / 1000L;
                    String liveText;
                    if (((Claim.StreamMetadata) item.getValue()).getReleaseTime() > nowTime) {
                        liveText = context.getResources().getString(R.string.soon).toUpperCase();
                        vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    } else {
                        int livestreamViewers = item.getLivestreamViewers();

                        if (livestreamViewers > 0) {
                            liveText = String.valueOf(livestreamViewers);
                            vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_viewerscount, 0);
                            vh.durationView.setCompoundDrawablePadding(8);
                        } else {
                            liveText = context.getResources().getString(R.string.live).toUpperCase();
                            vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                        }
                    }

                    vh.durationView.setText(liveText);
                } else {
                    vh.durationView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black));
                    if (!Claim.TYPE_COLLECTION.equalsIgnoreCase(item.getValueType())) {
                        vh.durationView.setText(Helper.formatDuration(duration));
                        vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    } else {
                        List<String> claimIds = item.getClaimIds() == null ? new ArrayList<>() : item.getClaimIds();
                        vh.durationView.setText(String.valueOf(claimIds.size()));
                        vh.durationView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_list_icon, 0, 0, 0);
                        vh.durationView.setCompoundDrawablePadding(8);
                    }

                    Helper.setViewText(vh.deviceView, item.getDevice());

                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    vh.itemView.setLayoutParams(lp);
                    vh.itemView.setVisibility(View.VISIBLE);
                    if (style != STYLE_SMALL_LIST_HORIZONTAL) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    }
                }
            } else if (Claim.TYPE_CHANNEL.equalsIgnoreCase(item.getValueType())) {
                if (!Helper.isNullOrEmpty(thumbnailUrl)) {
                    Glide.with(context.getApplicationContext()).
                            load(thumbnailUrl).
                            centerCrop().
                            placeholder(R.drawable.bg_thumbnail_placeholder).
                            apply(RequestOptions.circleCropTransform()).
                            into(vh.thumbnailView);
                }
                vh.alphaView.setText(item.getName().substring(1, 2).toUpperCase());
                vh.publisherView.setText(item.getName());
                vh.publishTimeView.setText(FormatTime.fromEpochMillis(publishTime));
                if (vh.getItemViewType() == VIEW_TYPE_FEATURED) {
                    vh.durationView.setVisibility(View.GONE);
                }

                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                vh.itemView.setLayoutParams(lp);
                vh.itemView.setVisibility(View.VISIBLE);
            }
        }

        if (contextGroupId == BlockedAndMutedFragment.BLOCKED_AND_MUTED_CONTEXT_GROUP_ID &&
                type == VIEW_TYPE_CHANNEL &&
                blockedOnBehalfOfChannelMap.containsKey(item.getClaimId())) {
            Claim onBehalfOf = blockedOnBehalfOfChannelMap.get(item.getClaimId());
            String obThumbnailUrl = onBehalfOf.getThumbnailUrl(thumbnailWidth, thumbnailHeight, thumbnailQ);
            int obBgColor = Helper.generateRandomColorForValue(onBehalfOf.getClaimId());
            if (obBgColor == 0) {
                obBgColor = Helper.generateRandomColorForValue(onBehalfOf.getName());
            }

            Helper.setViewVisibility(vh.onBehalfOfContainer, View.VISIBLE);
            if (!Helper.isNullOrEmpty(obThumbnailUrl)) {
                Glide.with(context.getApplicationContext()).
                        load(obThumbnailUrl).
                        centerCrop().
                        placeholder(R.drawable.bg_thumbnail_placeholder).
                        apply(RequestOptions.circleCropTransform()).
                        into(vh.onBehalfOfThumbnailView);
            }

            Helper.setViewText(vh.onBehalfOfAlphaView, item.getName().substring(1, 2).toUpperCase());
            Helper.setViewVisibility(vh.onBehalfOfNoThumbnailView, Helper.isNullOrEmpty(obThumbnailUrl) ? View.VISIBLE : View.GONE);
            Helper.setIconViewBackgroundColor(vh.onBehalfOfNoThumbnailView, obBgColor, false, context);
            Helper.setViewText(vh.onBehalfOfTitleView, !Helper.isNullOrEmpty(onBehalfOf.getTitle()) ? onBehalfOf.getTitle() : null);
            Helper.setViewText(vh.onBehalfOfNameView, onBehalfOf.getName());

            vh.onBehalfOfUnblockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (delegatedUnblockListener != null) {
                        delegatedUnblockListener.onUnblockClicked(item, onBehalfOf);
                    }
                }
            });
        } else {
            Helper.setViewVisibility(vh.onBehalfOfContainer, View.GONE);
            if (vh.onBehalfOfUnblockButton != null) {
                vh.onBehalfOfUnblockButton.setOnClickListener(null);
            }
        }
    }

    private void toggleSelectedClaim(Claim claim) {
        if (selectedItems.contains(claim)) {
            selectedItems.remove(claim);
        } else {
            selectedItems.add(claim);
        }

        if (selectionModeListener != null) {
            selectionModeListener.onItemSelectionToggled();
        }

        if (selectedItems.size() == 0) {
            inSelectionMode = false;
            if (selectionModeListener != null) {
                selectionModeListener.onExitSelectionMode();
            }
        }

        notifyItemChanged(items.indexOf(claim));
    }

    private long loadLastPlaybackPosition(Claim claim) {
        long position = -1;
        try {
            String url = !Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl();
            String key = String.format("PlayPos_%s", LbryUri.normalize(url));
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            position = sp.getLong(key, -1);
        } catch (LbryUriException ex) {
            ex.printStackTrace();
        }
        return position;
    }

    private String compactNumber(long number) {
        return CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(number);
    }

    /**
     * This method should be called after drag/drop or user reordering in the recycler view.
     */
    public void recalculateItemOrders() {
        int itemOrder = 0;
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setItemOrder(++itemOrder);
        }
    }

    /**
     * Modified from NewPipe <a href="https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/java/org/schabi/newpipe/util/Localization.java">Localization.java</a>
     */
    private double round(double value, int places) {
        return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    public interface ClaimListItemListener {
        void onClaimClicked(Claim claim, int position);
    }

    public interface DelegatedUnblockListener {
        void onUnblockClicked(Claim channel, Claim onBehalfOfChannel);
    }
}
