package com.odysee.app.ui.channel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.dialog.CreateSupportDialogFragment;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.listener.FetchChannelsListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.UrlSuggestion;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.tasks.claim.AbandonChannelTask;
import com.odysee.app.tasks.claim.AbandonHandler;
import com.odysee.app.tasks.lbryinc.ChannelSubscribeTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.lbryinc.FetchStatCountTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.controls.OutlineIconView;
import com.odysee.app.ui.controls.SolidIconView;
import com.odysee.app.ui.findcontent.FollowingFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import lombok.SneakyThrows;

public class ChannelFragment extends BaseFragment implements FetchChannelsListener {
    private Claim claim;
    private boolean subscribing;
    private String currentUrl;

    private View layoutResolving;
    private View layoutDisplayArea;
    private ImageView imageCover;
    private ImageView imageThumbnail;
    private View noThumbnailView;
    private TextView textAlpha;
    private TextView textTitle;
    private TextView textFollowerCount;
    private TabLayout tabLayout;
    private ViewPager2 tabPager;

    private View buttonEdit;
    private View buttonDelete;
    private View buttonShare;
    private View buttonTip;
    private View buttonReport;
    private View buttonFollowUnfollow;
    private View buttonBell;
    private SolidIconView iconBell;
    private int subCount;
    private TextView textFollow;
    private OutlineIconView iconFollow;
    private SolidIconView iconUnfollow;
    private View layoutNothingAtLocation;
    private View layoutLoadingState;

    private View blockUnblock;
    private TextView blockUnblockText;

    // if this is set, scroll to the specific comment on load
    private String commentHash;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel, container, false);

        layoutLoadingState = root.findViewById(R.id.channel_view_loading_state);
        layoutNothingAtLocation = root.findViewById(R.id.container_nothing_at_location);
        layoutDisplayArea = root.findViewById(R.id.channel_view_claim_display_area);
        layoutResolving = root.findViewById(R.id.channel_view_loading_container);

        imageCover = root.findViewById(R.id.channel_view_cover_image);
        imageThumbnail = root.findViewById(R.id.channel_view_thumbnail);
        noThumbnailView = root.findViewById(R.id.channel_view_no_thumbnail);
        textAlpha = root.findViewById(R.id.channel_view_icon_alpha);
        textTitle = root.findViewById(R.id.channel_view_title);
        textFollowerCount = root.findViewById(R.id.channel_view_follower_count);

        buttonEdit = root.findViewById(R.id.channel_view_edit);
        buttonDelete = root.findViewById(R.id.channel_view_delete);
        buttonShare = root.findViewById(R.id.channel_view_share);
        buttonTip = root.findViewById(R.id.channel_view_tip);
        buttonReport = root.findViewById(R.id.channel_view_report);
        buttonFollowUnfollow = root.findViewById(R.id.channel_view_follow_unfollow);
        textFollow = root.findViewById(R.id.channel_view_text_follow);
        iconFollow = root.findViewById(R.id.channel_view_icon_follow);
        iconUnfollow = root.findViewById(R.id.channel_view_icon_unfollow);
        buttonBell = root.findViewById(R.id.channel_view_subscribe_notify);
        iconBell = root.findViewById(R.id.channel_view_icon_bell);

        blockUnblock = root.findViewById(R.id.channel_view_block_unblock);
        blockUnblockText = root.findViewById(R.id.channel_view_block_unblock_text);

        blockUnblock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean blocked = Lbryio.isChannelBlocked(claim);
                Context context = getContext();
                if (context instanceof MainActivity) {
                    if (blocked) {
                        // handle unblock
                        ((MainActivity) context).handleUnblockChannel(claim);
                    } else {
                        ((MainActivity) context).handleBlockChannel(claim);
                    }
                }
            }
        });

        tabPager = root.findViewById(R.id.channel_view_pager);
        tabLayout = root.findViewById(R.id.channel_view_tabs);
        tabPager.setSaveEnabled(false);

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (claim != null) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).openChannelForm(claim);
                    }
                }
            }
        });
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (claim != null) {
                    Context c = getContext();
                    if (c != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(c).
                                setTitle(R.string.delete_channel).
                                setMessage(R.string.confirm_delete_channel)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        deleteCurrentClaim();
                                    }
                                }).setNegativeButton(R.string.no, null);
                        builder.show();
                    }
                }
            }
        });

        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (claim != null) {
                    try {
                        String shareUrl = LbryUri.parse(
                                !Helper.isNullOrEmpty(claim.getCanonicalUrl()) ? claim.getCanonicalUrl() :
                                        (!Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl())).toOdyseeString();
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);

                        MainActivity.startingShareActivity = true;
                        Intent shareUrlIntent = Intent.createChooser(shareIntent, getString(R.string.share_lbry_content));
                        shareUrlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(shareUrlIntent);
                    } catch (LbryUriException ex) {
                        // pass
                    }
                }
            }
        });

        buttonTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();

                if (activity != null && activity.isSignedIn()) {
                    if (claim != null) {
                        CreateSupportDialogFragment dialog = CreateSupportDialogFragment.newInstance(claim, (amount, isTip) -> {
                            double sentAmount = amount.doubleValue();
                            View view1 = getView();
                            if (view1 != null) {
                                String message = getResources().getQuantityString(
                                        isTip ? R.plurals.you_sent_a_tip : R.plurals.you_sent_a_support, sentAmount == 1.0 ? 1 : 2,
                                        new DecimalFormat("#,###.##").format(sentAmount));
                                Snackbar.make(view1, message, Snackbar.LENGTH_LONG).show();
                            }
                        });
                        Context context = getContext();
                        if (context instanceof MainActivity) {
                            dialog.show(((MainActivity) context).getSupportFragmentManager(), CreateSupportDialogFragment.TAG);
                        }
                    }
                } else {
                    if (activity != null) {
                        activity.simpleSignIn(0);
                    }
                }
            }
        });

        buttonReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (claim != null) {
                    Context context = getContext();
                    CustomTabColorSchemeParams.Builder ctcspb = new CustomTabColorSchemeParams.Builder();
                    ctcspb.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    CustomTabColorSchemeParams ctcsp = ctcspb.build();

                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder().setDefaultColorSchemeParams(ctcsp);
                    CustomTabsIntent intent = builder.build();
                    intent.launchUrl(context, Uri.parse(String.format("https://odysee.com/$/report_content?claimId=%s", claim.getClaimId())));
                }
            }
        });

        buttonBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (claim != null) {
                    boolean isNotificationsDisabled = Lbryio.isNotificationsDisabled(claim);
                    final Subscription subscription = Subscription.fromClaim(claim);
                    subscription.setNotificationsDisabled(!isNotificationsDisabled);
                    view.setEnabled(false);
                    Context context = getContext();
                    new ChannelSubscribeTask(context, claim.getClaimId(), subscription, false, new ChannelSubscribeTask.ChannelSubscribeHandler() {
                        @Override
                        public void onSuccess() {
                            view.setEnabled(true);
                            Lbryio.updateSubscriptionNotificationsDisabled(subscription);

                            Context context = getContext();
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).showMessage(subscription.isNotificationsDisabled() ?
                                        R.string.receive_no_notifications : R.string.receive_all_notifications);
                            }
                            checkIsFollowing();

                            if (context != null) {
                                context.sendBroadcast(new Intent(MainActivity.ACTION_SAVE_SHARED_USER_STATE));
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            view.setEnabled(true);
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        buttonFollowUnfollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();

                if (activity != null && activity.isSignedIn()) {
                    if (claim != null) {
                        if (subscribing) {
                            return;
                        }

                        boolean isFollowing = Lbryio.isFollowing(claim);
                        if (isFollowing) {
                            Context context = getContext();
                            if (context != null) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context).
                                        setTitle(R.string.confirm_unfollow).
                                        setMessage(R.string.confirm_unfollow_message)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                doFollowUnfollow(isFollowing, view);
                                            }
                                        }).setNegativeButton(R.string.no, null);
                                builder.show();
                            }
                        } else {
                            doFollowUnfollow(isFollowing, view);
                        }
                    }
                } else {
                    if (activity != null) {
                        activity.simpleSignIn(0);
                    }
                }
            }
        });

        return root;
    }

    private void doFollowUnfollow(boolean isFollowing, View view) {
        subscribing = true;
        Subscription subscription = Subscription.fromClaim(claim);
        view.setEnabled(false);
        new ChannelSubscribeTask(getContext(), claim.getClaimId(), subscription, isFollowing, new ChannelSubscribeTask.ChannelSubscribeHandler() {
            @Override
            public void onSuccess() {
                if (isFollowing) {
                    Lbryio.removeSubscription(subscription);
                    Lbryio.removeCachedResolvedSubscription(claim);
                } else {
                    Lbryio.addSubscription(subscription);
                    Lbryio.addCachedResolvedSubscription(claim);
                }
                buttonFollowUnfollow.setEnabled(true);
                subscribing = false;
                checkIsFollowing();
                FollowingFragment.resetClaimSearchContent = true;

                Context context = getContext();
                if (context != null) {
                    context.sendBroadcast(new Intent(MainActivity.ACTION_SAVE_SHARED_USER_STATE));
                }
            }

            @Override
            public void onError(Exception exception) {
                buttonFollowUnfollow.setEnabled(true);
                subscribing = false;
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void deleteCurrentClaim() {
        if (claim != null) {
            Helper.setViewVisibility(layoutDisplayArea, View.GONE);
            Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
            AbandonChannelTask task = new AbandonChannelTask(Collections.singletonList(claim.getClaimId()), layoutResolving, Lbryio.AUTH_TOKEN, new AbandonHandler() {
                @Override
                public void onComplete(List<String> successfulClaimIds, List<String> failedClaimIds, List<Exception> errors) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        if (failedClaimIds.size() == 0) {
                            MainActivity activity = (MainActivity) context;
                            activity.showMessage(R.string.channel_deleted);
                            activity.onBackPressed();
                        } else {
                            View root = getView();
                            if (root != null) {
                                Snackbar.make(root, R.string.channel_failed_delete, Snackbar.LENGTH_LONG).
                                        setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                            }
                        }
                    }
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void checkIsFollowing() {
        if (claim != null) {
            boolean isFollowing = Lbryio.isFollowing(claim);
            boolean notificationsDisabled = Lbryio.isNotificationsDisabled(claim);
            Helper.setViewVisibility(iconFollow, !isFollowing ? View.VISIBLE : View.GONE);
            Helper.setViewVisibility(textFollow, !isFollowing ? View.VISIBLE : View.GONE);
            Helper.setViewVisibility(iconUnfollow, isFollowing ? View.VISIBLE : View.GONE);
            Helper.setViewVisibility(buttonBell, isFollowing ? View.VISIBLE : View.GONE);

            if (iconBell != null) {
                iconBell.setText(notificationsDisabled ? R.string.fa_bell : R.string.fa_bell_slash);
            }
        }
    }

    public void onChannelsFetched(List<Claim> channels) {
        checkOwnChannel();
    }

    private void checkOwnChannel() {
        if (claim != null) {
            boolean isOwnChannel = Lbry.ownChannels.contains(claim);
            Helper.setViewVisibility(buttonEdit, isOwnChannel ? View.VISIBLE : View.GONE);
            Helper.setViewVisibility(buttonDelete, isOwnChannel ? View.VISIBLE : View.GONE);
        }
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        Map<String, Object> params = getParams();
        String url = params != null && params.containsKey("url") ? (String) params.get("url") : null;
        Helper.setWunderbarValue(url, context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.updateCurrentDisplayFragment(this);
            activity.addFetchChannelsListener(this);
            activity.updateMiniPlayerMargins(false);
            LbryAnalytics.setCurrentScreen(activity, "Channel", "Channel");
        }

        checkParams();
        checkOwnChannel();
        checkChannelBlocked();
    }

    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.updateMiniPlayerMargins(true);
            activity.removeFetchChannelsListener(this);
        }
        super.onPause();
    }

    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.resetCurrentDisplayFragment();
        }
        super.onStop();
    }

    private void checkParams() {
        boolean updateRequired = false;
        Map<String, Object> params = getParams();
        String newUrl;
        if (params != null) {
            if (params.containsKey("claim")) {
                Claim claim = (Claim) params.get("claim");
                if (claim != null && !claim.equals(this.claim)) {
                    this.claim = claim;
                    updateRequired = true;
                }
            }
            if (params.containsKey("url")) {
                Object o = params.get("url");
                String urlString = "";
                if (o != null) {
                    urlString = o.toString();
                }
                LbryUri newLbryUri = LbryUri.tryParse(urlString);
                if (newLbryUri != null) {
                    newUrl = newLbryUri.toString();
                    String qs = newLbryUri.getQueryString();
                    if (!Helper.isNullOrEmpty(qs)) {
                        String[] qsPairs = qs.split("&");
                        for (String pair : qsPairs) {
                            String[] parts = pair.split("=");
                            if (parts.length < 2) {
                                continue;
                            }
                            if ("comment_hash".equalsIgnoreCase(parts[0])) {
                                commentHash = parts[1];
                                break;
                            }
                        }
                    }

                    if (claim == null || !newUrl.equalsIgnoreCase(currentUrl)) {
                        this.claim = null;
                        this.currentUrl = newUrl;
                        updateRequired = true;
                    }
                }
            }
        }

        if (updateRequired) {
            resetSubCount();
            if (!Helper.isNullOrEmpty(currentUrl)) {
                // check if the claim is already cached
                ClaimCacheKey key = new ClaimCacheKey();
                key.setUrl(currentUrl);
                if (Lbry.claimCache.containsKey(key)) {
                    claim = Lbry.claimCache.get(key);
                } else {
                    resolveUrl();
                }
            } else if (claim == null) {
                // nothing at this location
                renderNothingAtLocation();
            }
        }

        if (!Helper.isNullOrEmpty(currentUrl)) {
            Helper.saveUrlHistory(currentUrl, claim != null ? claim.getTitle() : null, UrlSuggestion.TYPE_CHANNEL);
        }

        if (claim != null) {
            renderClaim();
        }
    }

    private void resolveUrl() {
        Helper.setViewVisibility(layoutDisplayArea, View.INVISIBLE);
        Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
        ResolveTask task = new ResolveTask(currentUrl, Lbry.API_CONNECTION_STRING, layoutResolving, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                if (claims.size() > 0 && !Helper.isNullOrEmpty(claims.get(0).getClaimId())) {
                    claim = claims.get(0);
                    if (!Helper.isNullOrEmpty(currentUrl)) {
                        Helper.saveUrlHistory(currentUrl, claim.getTitle(), UrlSuggestion.TYPE_CHANNEL);
                    }

                    renderClaim();
                    checkOwnChannel();
                } else {
                    renderNothingAtLocation();
                }
            }

            @Override
            public void onError(Exception error) {
                renderNothingAtLocation();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void renderNothingAtLocation() {
        layoutLoadingState.setVisibility(View.VISIBLE);
        layoutNothingAtLocation.setVisibility(View.VISIBLE);
        layoutDisplayArea.setVisibility(View.INVISIBLE);
    }

    public void setParams(Map<String, Object> params) {
        super.setParams(params);
        if (getView() != null) {
            checkParams();
        }
    }

    private void renderClaim() {
        if (claim == null) {
            renderNothingAtLocation();
            return;
        }

        loadSubCount();
        checkIsFollowing();
        layoutLoadingState.setVisibility(View.GONE);
        layoutDisplayArea.setVisibility(View.VISIBLE);

        if (claim.getTags().contains("disable-support"))
            buttonTip.setVisibility(View.GONE);
        else
            buttonTip.setVisibility(View.VISIBLE);

        String thumbnailUrl = "";
        String coverUrl = claim.getCoverUrl();
        textTitle.setText(Helper.isNullOrEmpty(claim.getTitle()) ? claim.getName() : claim.getTitle());

        Context context = getContext();

        if (context != null) {
            thumbnailUrl = claim.getThumbnailUrl(imageThumbnail.getLayoutParams().width, imageThumbnail.getLayoutParams().height, 85);
        }
        if (context != null && !Helper.isNullOrEmpty(coverUrl)) {
            Glide.with(context.getApplicationContext()).load(coverUrl).centerCrop().into(imageCover);
        }
        if (context != null && !Helper.isNullOrEmpty(thumbnailUrl)) {
            Glide.with(context.getApplicationContext()).load(thumbnailUrl).apply(RequestOptions.circleCropTransform()).into(imageThumbnail);
            noThumbnailView.setVisibility(View.GONE);
        } else {
            imageThumbnail.setVisibility(View.GONE);

            int bgColor = Helper.generateRandomColorForValue(claim.getClaimId());
            Helper.setIconViewBackgroundColor(noThumbnailView, bgColor, false, getContext());
            noThumbnailView.setVisibility(View.VISIBLE);
            if (claim.getName() != null) {
                textAlpha.setText(claim.getName().substring(1, 2));
            }
        }

        try {
            if (tabPager.getAdapter() == null && context instanceof MainActivity) {
                tabPager.setAdapter(new ChannelPagerAdapter(claim, commentHash, (MainActivity) context));
                if (!Helper.isNullOrEmpty(commentHash)) {
                    // set the Comments tab active if a comment hash is set
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tabPager.setCurrentItem(3);
                        }
                    }, 500);
                }
            }
            new TabLayoutMediator(tabLayout, tabPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    switch (position) {
                        case 0: tab.setText(R.string.content); break;
                        case 1: tab.setText(R.string.playlists); break;
                        case 2: tab.setText(R.string.about); break;
                        case 3: tab.setText(R.string.comments); break;
                    }
                }
            }).attach();
        } catch (IllegalStateException ex) {
            // TODO: Fix why this is happening
            // pass
        }
    }

    public void checkChannelBlocked() {
        if (claim != null) {
            boolean channelBlocked = Lbryio.isChannelBlocked(claim);
            if (blockUnblockText != null) {
                blockUnblockText.setText(channelBlocked ? R.string.unblock_channel : R.string.block_channel);
            }
        }
    }

    private void resetSubCount() {
        subCount = -1;
        Helper.setViewText(textFollowerCount, null);
        Helper.setViewVisibility(textFollowerCount, View.INVISIBLE);
    }

    private void loadSubCount() {
        if (claim != null) {
            FetchStatCountTask task = new FetchStatCountTask(
                    FetchStatCountTask.STAT_SUB_COUNT, claim.getClaimId(), null, new FetchStatCountTask.FetchStatCountHandler() {
                @Override
                public void onSuccess(int count) {
                    try {
                        String displayText = getResources().getQuantityString(R.plurals.follower_count, count, NumberFormat.getInstance().format(count));
                        Helper.setViewText(textFollowerCount, displayText);
                        Helper.setViewVisibility(textFollowerCount, View.VISIBLE);
                    } catch (IllegalStateException ex) {
                        // pass
                    }
                }

                @Override
                public void onError(Exception error) {
                    // pass
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void applyFilterForBlockedChannels(List<LbryUri> blockedChannels) {
        if (tabPager != null && tabPager.getAdapter() != null) {
            Fragment commentsFragment = ((ChannelPagerAdapter) tabPager.getAdapter()).getCommentsFragment();
            if (commentsFragment instanceof ChannelCommentsFragment) {
                ((ChannelCommentsFragment) commentsFragment).applyFilterForBlockedChannels(blockedChannels);
            }
        }

        checkChannelBlocked();
    }

    private static class ChannelPagerAdapter extends FragmentStateAdapter {
        private final Claim channelClaim;
        private final String commentHash;
        private Fragment commentsFragmentInstance;
        public ChannelPagerAdapter(Claim channelClaim, String commentHash, FragmentActivity activity) {
            super(activity);
            this.channelClaim = channelClaim;
            this.commentHash = commentHash;
        }

        @SneakyThrows
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    ChannelContentFragment contentFragment = ChannelContentFragment.class.newInstance();
                    if (channelClaim != null) {
                        contentFragment.setChannelId(channelClaim.getClaimId());
                    }
                    return contentFragment;

                case 1:
                    ChannelPlaylistsFragment playlistsFragment = ChannelPlaylistsFragment.class.newInstance();
                    if (channelClaim != null) {
                        playlistsFragment.setChannelId(channelClaim.getClaimId());
                    }
                    return playlistsFragment;

                case 2:
                    ChannelAboutFragment aboutFragment = ChannelAboutFragment.class.newInstance();
                    try {
                        Claim.ChannelMetadata metadata = (Claim.ChannelMetadata) channelClaim.getValue();
                        if (metadata != null) {
                            aboutFragment.setDescription(metadata.getDescription());
                            aboutFragment.setEmail(metadata.getEmail());
                            aboutFragment.setWebsite(metadata.getWebsiteUrl());
                        }
                    } catch (ClassCastException ex) {
                        // pass
                    }
                    return aboutFragment;

                case 3:
                    ChannelCommentsFragment commentsFragment = ChannelCommentsFragment.class.newInstance();
                    if (channelClaim != null) {
                        commentsFragment.setClaim(channelClaim);
                    }
                    if (!Helper.isNullOrEmpty(commentHash)) {
                        commentsFragment.setCommentHash(commentHash);
                    }
                    commentsFragmentInstance = commentsFragment;
                    return commentsFragment;
            }

            // TODO: createFragment is defined as a @NonNull and should never be able to return null.
            return null;
        }

        public Fragment getCommentsFragment() {
            return commentsFragmentInstance;
        }

        public long getItemId(int position) {
            return String.format("%s-%d", channelClaim.getClaimId(), position).hashCode();
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
