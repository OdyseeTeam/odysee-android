package com.odysee.app.ui.findcontent;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.TracksInfo.TrackGroupInfo;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
//import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides.TrackSelectionOverride;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.commonmark.node.Code;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.AttributeProviderFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.adapter.CommentItemDecoration;
import com.odysee.app.adapter.CommentListAdapter;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.adapter.TagListAdapter;
import com.odysee.app.callable.LighthouseSearch;
import com.odysee.app.callable.BuildCommentReactOptions;
import com.odysee.app.exceptions.LbryRequestException;
import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.runnable.ReactToComment;
import com.odysee.app.callable.Search;
import com.odysee.app.dialog.RepostClaimDialogFragment;
import com.odysee.app.dialog.CreateSupportDialogFragment;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.FetchClaimsListener;
import com.odysee.app.listener.PIPModeListener;
import com.odysee.app.listener.ScreenOrientationListener;
import com.odysee.app.listener.StoragePermissionListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.Comment;
import com.odysee.app.model.Fee;
import com.odysee.app.model.LbryFile;
import com.odysee.app.model.Reactions;
import com.odysee.app.model.Tag;
import com.odysee.app.model.UrlSuggestion;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.tasks.BufferEventTask;
import com.odysee.app.tasks.CommentCreateTask;
import com.odysee.app.tasks.CommentListHandler;
import com.odysee.app.tasks.CommentListTask;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.tasks.ReadTextFileTask;
import com.odysee.app.tasks.claim.AbandonHandler;
import com.odysee.app.tasks.claim.AbandonStreamTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.claim.ClaimSearchResultHandler;
import com.odysee.app.tasks.claim.ClaimSearchTask;
import com.odysee.app.tasks.claim.PurchaseListTask;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.file.DeleteFileTask;
import com.odysee.app.tasks.file.FileListTask;
import com.odysee.app.tasks.file.GetFileTask;
import com.odysee.app.tasks.lbryinc.ChannelSubscribeTask;
import com.odysee.app.tasks.lbryinc.ClaimRewardTask;
import com.odysee.app.tasks.lbryinc.FetchStatCountTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.channel.ChannelCreateDialogFragment;
import com.odysee.app.ui.controls.SolidIconView;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Predefined;
import com.odysee.app.checkers.CommentEnabledCheck;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileViewFragment extends BaseFragment implements
        MainActivity.BackPressInterceptor,
        ClaimListAdapter.ClaimListItemListener,
        DownloadActionListener,
        FetchClaimsListener,
        PIPModeListener,
        ScreenOrientationListener,
        StoragePermissionListener,
        ChannelCreateDialogFragment.ChannelCreateListener {
    private static final String TAG = "OdyseeFile";
    public static int FILE_CONTEXT_GROUP_ID = 2;
    private static final int RELATED_CONTENT_SIZE = 16;
    private static final String DEFAULT_PLAYBACK_SPEED = "1x";
    @StringRes
    private static final int AUTO_QUALITY_STRING = R.string.auto_quality;
    private static final int AUTO_QUALITY_ID = 0;
    public static final String CDN_PREFIX = "https://cdn.lbryplayer.xyz";

//    private PlayerControlView castControlView;
    private Player currentPlayer;
    private boolean loadingNewClaim;
    private boolean loadingQualityChanged = false;
//    private boolean startDownloadPending;
//    private boolean fileGetPending;
    private boolean downloadInProgress;
    private boolean downloadRequested;
//    private boolean loadFilePending;
    private boolean isPlaying;
//    private boolean resolving;
//    private boolean initialFileLoadDone;
    private Claim fileClaim;
    private Claim collectionClaimItem;
    private String currentPlaylistTitle;
    private String currentUrl;
    private String currentMediaSourceUrl;
    private TextView relatedContentTitle;
    private ClaimListAdapter relatedContentAdapter;
    private CommentEnabledCheck commentEnabledCheck;
    private CommentListAdapter commentListAdapter;
    private Player.Listener fileViewPlayerListener;
    private View commentLoadingArea;

    private NestedScrollView scrollView;
    private long elapsedDuration = 0;
    private long totalDuration = 0;
    private boolean elapsedPlaybackScheduled;
    private ScheduledExecutorService elapsedPlaybackScheduler;
    private boolean playbackStarted;
    private long startTimeMillis;
    private GetFileTask getFileTask;
    private Handler seekOverlayHandler;

    private boolean storagePermissionRefusedOnce;
    private View buttonPublishSomething;
    private View layoutLoadingState;
    private View layoutNothingAtLocation;
    private View layoutDisplayArea;
    private View layoutResolving;
    private int lastPositionSaved;

    private View tipButton;
    private boolean playlistResolved;
    private List<Claim> playlistClaims = new ArrayList<>();

    private WebView webView;
    private boolean webViewAdded;

    private ViewGroup singleCommentRoot;
    private ImageButton expandButton;
    private Comment replyToComment;
    private View containerReplyToComment;
    private View containerCommentForm;
    private TextView textReplyingTo;
    private TextView textReplyToBody;
    private View buttonClearReplyToComment;
    private TextView textNothingAtLocation;

    private TextView likeReactionAmount;
    private TextView dislikeReactionAmount;
    private ImageView likeReactionIcon;
    private ImageView dislikeReactionIcon;
    private ScheduledExecutorService scheduledExecutor;
    ScheduledFuture<?> futureReactions;
    Reactions reactions;

    private boolean postingComment;
    private boolean fetchingChannels;
    private View progressLoadingChannels;
    private View progressPostComment;
    private InlineChannelSpinnerAdapter commentChannelSpinnerAdapter;
    private AppCompatSpinner commentChannelSpinner;
    private TextInputEditText inputComment;
    private TextView textCommentLimit;
    private MaterialButton buttonPostComment;
    private MaterialButton buttonCreateChannel;
    private ImageView commentPostAsThumbnail;
    private View commentPostAsNoThumbnail;
    private TextView commentPostAsAlpha;
    private MaterialButton buttonCommentSignedInUserRequired;

    // if this is set, scroll to the specific comment on load
    private String commentHash;

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commentEnabledCheck = new CommentEnabledCheck();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_file_view, container, false);

        layoutLoadingState = root.findViewById(R.id.file_view_loading_state);
        layoutNothingAtLocation = root.findViewById(R.id.container_nothing_at_location);
        layoutResolving = root.findViewById(R.id.file_view_loading_container);
        layoutDisplayArea = root.findViewById(R.id.file_view_claim_display_area);
        buttonPublishSomething = root.findViewById(R.id.nothing_at_location_publish_button);

        tipButton = root.findViewById(R.id.file_view_action_tip);

        expandButton = root.findViewById(R.id.expand_commentarea_button);
        singleCommentRoot = root.findViewById(R.id.collapsed_comment);
        relatedContentTitle = root.findViewById(R.id.related_or_playlist);

        containerCommentForm = root.findViewById(R.id.container_comment_form);
        containerReplyToComment = root.findViewById(R.id.comment_form_reply_to_container);
        textReplyingTo = root.findViewById(R.id.comment_form_replying_to_text);
        textReplyToBody = root.findViewById(R.id.comment_form_reply_to_body);
        buttonClearReplyToComment = root.findViewById(R.id.comment_form_clear_reply_to);

        commentChannelSpinner = root.findViewById(R.id.comment_form_channel_spinner);
        progressLoadingChannels = root.findViewById(R.id.comment_form_channels_loading);
        progressPostComment = root.findViewById(R.id.comment_form_post_progress);
        inputComment = root.findViewById(R.id.comment_form_body);
        textCommentLimit = root.findViewById(R.id.comment_form_text_limit);
        buttonPostComment = root.findViewById(R.id.comment_form_post);
        buttonCreateChannel = root.findViewById(R.id.create_channel_button);
        commentPostAsThumbnail = root.findViewById(R.id.comment_form_thumbnail);
        commentPostAsNoThumbnail = root.findViewById(R.id.comment_form_no_thumbnail);
        commentPostAsAlpha = root.findViewById(R.id.comment_form_thumbnail_alpha);
        buttonCommentSignedInUserRequired = root.findViewById(R.id.sign_in_user_button);
        textNothingAtLocation = root.findViewById(R.id.nothing_at_location_text);
        commentLoadingArea = root.findViewById(R.id.file_comments_loading);

        likeReactionAmount = root.findViewById(R.id.likes_amount);
        dislikeReactionAmount = root.findViewById(R.id.dislikes_amount);
        likeReactionIcon = root.findViewById(R.id.like_icon);
        dislikeReactionIcon = root.findViewById(R.id.dislike_icon);

        initUi(root);

        fileViewPlayerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    elapsedDuration = MainActivity.appPlayer.getCurrentPosition();
                    totalDuration = MainActivity.appPlayer.getDuration() < 0 ? 0 : MainActivity.appPlayer.getDuration();
                    if (!playbackStarted) {
                        logPlay(currentUrl, startTimeMillis);
                        playbackStarted = true;

                        long lastPosition = loadLastPlaybackPosition();
                        if (lastPosition > -1) {
                            MainActivity.appPlayer.seekTo(lastPosition);
                        }
                    }
                    renderTotalDuration();
                    scheduleElapsedPlayback();
                    hideBuffering();

                    if (loadingNewClaim) {
                        setPlaybackSpeedToDefault();
                        setPlayerQualityToDefault();
                        MainActivity.appPlayer.setPlayWhenReady(true);
                        loadingNewClaim = false;
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                    if (!loadingQualityChanged) {
                        Context ctx = getContext();
                        boolean sendBufferingEvents = true;

                        if (ctx != null) {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
                            sendBufferingEvents = sp.getBoolean(MainActivity.PREFERENCE_KEY_SEND_BUFFERING_EVENTS, true);
                        }

                        if (MainActivity.appPlayer != null && MainActivity.appPlayer.getCurrentPosition() > 0 && sendBufferingEvents) {
                            // we only want to log a buffer event after the media has already started playing
                            if (!Helper.isNullOrEmpty(currentMediaSourceUrl)) {
                                long duration = MainActivity.appPlayer.getDuration();
                                long position = MainActivity.appPlayer.getCurrentPosition();
                                String userIdHash = Lbryio.currentUser != null ? String.valueOf(Lbryio.currentUser.getId()) : "0";
                                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;

                                BufferEventTask bufferEvent = new BufferEventTask(actualClaim.getPermanentUrl(), duration, position, 1, userIdHash);
                                bufferEvent.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                            }
                        }
                    }

                    loadingQualityChanged = false;

                    showBuffering();
                } else if (playbackState == Player.STATE_ENDED) {
                    playNextItemInPlaylist();
                } else {
                    hideBuffering();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlayng) {
                isPlaying = isPlayng;
            }
        };

        scrollView = root.findViewById(R.id.file_view_scroll_view);
        return root;
    }

    private void playNextItemInPlaylist() {
        if (playlistClaims.size() > 0 && collectionClaimItem != null) {
            int collectionClaimIndex = playlistClaims.indexOf(collectionClaimItem);
            if (collectionClaimIndex > -1) {
                int nextIndex = collectionClaimIndex + 1;
                if (nextIndex < playlistClaims.size() - 1) {
                    playClaimFromCollection(playlistClaims.get(nextIndex), nextIndex);
                }
            }
        }
    }

    public void onStart() {
        super.onStart();
        Context context = getContext();

        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.hideToolbar();
            activity.setBackPressInterceptor(this);
            activity.addDownloadActionListener(this);
            activity.addFetchClaimsListener(this);
            activity.addPIPModeListener(this);
            activity.addScreenOrientationListener(this);
            if (!MainActivity.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) {
                activity.addStoragePermissionListener(this);
            }
        }
    }

    private void handlePlayCollection(Map<String, Object> params) {
        OdyseeCollection collection = (OdyseeCollection) params.get("collection");
        playlistClaims = new ArrayList<>(collection.getClaims());
        playlistResolved = true;
        currentPlaylistTitle = collection.getName();

        relatedContentAdapter = new ClaimListAdapter(playlistClaims, getContext());
        relatedContentAdapter.setListener(FileViewFragment.this);

        View root = getView();
        if (root != null) {
            RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
            relatedContentList.setAdapter(relatedContentAdapter);
        }

        if (playlistClaims.size() > 0) {
            if (params.containsKey("item") && params.containsKey("itemIndex")) {
                int index = (int) params.get("itemIndex");
                playClaimFromCollection(playlistClaims.get(index), index);
            } else {
                playClaimFromCollection(playlistClaims.get(0), 0);
            }
        }
    }

    private void updatePlaylistContentDisplay(int index) {
        if (playlistClaims != null) {
            String value = getString(R.string.playlist_position_tracker, currentPlaylistTitle, index + 1, playlistClaims.size());
            Helper.setViewText(relatedContentTitle, value);
        }
    }

    private void checkParams() {
        boolean updateRequired = false;
        Context context = getContext();
//        claim.setClaimId(FileViewFragmentArgs.fromBundle(getArguments()).getClaimId());
        try {
            Map<String, Object> params = getParams();
            Claim newClaim = null;
            String newUrl = null;
            if (params != null) {
                if (params.containsKey("collection")) {
                    handlePlayCollection(params);
                    return;
                }


                if (params.containsKey("claim")) {
                    newClaim = (Claim) params.get("claim");
                    // Only update fragment if new claim is different than currently being played
                    if (newClaim != null && !newClaim.equals(this.fileClaim)) {
                        updateRequired = true;
                    }
                }

                if (params.containsKey("url")) {
                    LbryUri newLbryUri = LbryUri.tryParse(params.get("url").toString());
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

                        if (fileClaim == null || !newUrl.equalsIgnoreCase(currentUrl)) {
                            updateRequired = true;
                        }
                    }
                }
            } else if (currentUrl != null) {
                updateRequired = true;
            } else if (context instanceof MainActivity) {
                ((MainActivity) context).onBackPressed();
            }

            boolean invalidRepost = false;
            if (updateRequired) {
                resetViewCount();
                resetFee();
                checkNewClaimAndUrl(newClaim, newUrl);

                // This is required to recycle current fragment with new claim from related content
                fileClaim = null;

                if (newClaim != null) {
                    fileClaim = newClaim;
                }

                if (fileClaim == null && !Helper.isNullOrEmpty(newUrl)) {
                    // check if the claim is already cached
                    currentUrl = newUrl;
                    ClaimCacheKey key = new ClaimCacheKey();
                    key.setUrl(currentUrl);
                    onNewClaim(currentUrl);
                    if (Lbry.claimCache.containsKey(key)) {
                        fileClaim = Lbry.claimCache.get(key);
                    }
                }
                if (fileClaim != null && Claim.TYPE_REPOST.equalsIgnoreCase(fileClaim.getValueType())) {
                    fileClaim = fileClaim.getRepostedClaim();
                    if (fileClaim == null || Helper.isNullOrEmpty(fileClaim.getClaimId())) {
                        // Invalid repost, probably
                        invalidRepost = true;
                        renderNothingAtLocation();
                    } else if (fileClaim.getName().startsWith("@")) {
                        // this is a reposted channel, so launch the channel url
                        if (context instanceof MainActivity) {
                            MainActivity activity = (MainActivity) context;
                            //activity.onBackPressed(); // remove the reposted url page from the back stack
                            activity.getSupportFragmentManager().popBackStack();
                            activity.openChannelUrl(!Helper.isNullOrEmpty(fileClaim.getShortUrl()) ? fileClaim.getShortUrl() : fileClaim.getPermanentUrl());
                        }
                        return;
                    }
                }
                if (fileClaim == null) {
                    resolveUrl(currentUrl);
                }
            } else {
                checkAndResetNowPlayingClaim();
            }

            if (!Helper.isNullOrEmpty(currentUrl)) {
                Helper.saveUrlHistory(currentUrl, fileClaim != null ? fileClaim.getTitle() : null, UrlSuggestion.TYPE_FILE);
            }

            if (fileClaim != null && !invalidRepost) {
                if (Claim.TYPE_COLLECTION.equalsIgnoreCase(fileClaim.getValueType()) &&
                        fileClaim.getClaimIds() != null && fileClaim.getClaimIds().size() > 0) {
                    resolvePlaylistClaimsAndPlayFirst();
                    return;
                }

                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                // TODO: Determine if we should save the playlist in the view history? Or save the first claim?
                Helper.saveViewHistory(currentUrl, fileClaim);

                if (Helper.isClaimBlocked(actualClaim)) {
                    renderClaimBlocked();
                } else {
                    checkAndLoadRelatedContent();
                    checkAndLoadComments();
                    renderClaim();
                    if (actualClaim.getFile() == null) {
                        loadFile();
                    } else {
//                    initialFileLoadDone = true;
                    }
                }
            }

            checkIsFileComplete();
        } catch (Exception ex){
            android.util.Log.e(TAG, ex.getMessage(), ex);
        }
    }

    private void renderNothingAtLocation() {
        Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
        Helper.setViewVisibility(layoutNothingAtLocation, View.VISIBLE);
        Helper.setViewVisibility(buttonPublishSomething, View.VISIBLE);
        Helper.setViewVisibility(layoutResolving, View.GONE);
        Helper.setViewVisibility(layoutDisplayArea, View.INVISIBLE);
        if (textNothingAtLocation != null) {
            textNothingAtLocation.setText(R.string.nothing_at_this_location);
        }
    }

    private void renderClaimBlocked() {
        Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
        Helper.setViewVisibility(layoutNothingAtLocation, View.VISIBLE);
        Helper.setViewVisibility(buttonPublishSomething, View.INVISIBLE);
        Helper.setViewVisibility(layoutResolving, View.GONE);
        Helper.setViewVisibility(layoutDisplayArea, View.INVISIBLE);
        if (textNothingAtLocation != null) {
            textNothingAtLocation.setMovementMethod(LinkMovementMethod.getInstance());
            textNothingAtLocation.setText(HtmlCompat.fromHtml(getString(R.string.dmca_complaint_blocked), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    private void checkNewClaimAndUrl(Claim newClaim, String newUrl) {
        boolean shouldResetNowPlaying = false;
        if (newClaim != null &&
                MainActivity.nowPlayingClaim != null &&
                !MainActivity.nowPlayingClaim.getClaimId().equalsIgnoreCase(newClaim.getClaimId())) {
            shouldResetNowPlaying = true;
        }
        if (!shouldResetNowPlaying &&
                newUrl != null &&
                MainActivity.nowPlayingClaim != null &&
                !newUrl.equalsIgnoreCase(MainActivity.nowPlayingClaimUrl) &&
                !newUrl.equalsIgnoreCase(MainActivity.nowPlayingClaim.getShortUrl()) &&
                !newUrl.equalsIgnoreCase(MainActivity.nowPlayingClaim.getPermanentUrl()) &&
                !newUrl.equalsIgnoreCase(MainActivity.nowPlayingClaim.getCanonicalUrl())) {
            shouldResetNowPlaying = true;
        }

        if (shouldResetNowPlaying) {
            if (MainActivity.appPlayer != null) {
                MainActivity.appPlayer.setPlayWhenReady(false);
            }
            Context context = getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).clearNowPlayingClaim();
                resetPlayer();
            }
        }
    }

    @Override
    public void onViewCreated(@NotNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString("url");
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("url", currentUrl);
    }

    private void initWebView(View root) {
        Context ctx = getContext();
        if (ctx != null) {
            if (webView == null) {
                webView = new WebView(ctx);
                webView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                webView.setWebViewClient(new LbryWebViewClient(ctx));
                WebSettings webSettings = webView.getSettings();
                webSettings.setAllowFileAccess(true);
                webSettings.setJavaScriptEnabled(false);
            }

            if (!webViewAdded && root != null) {
                ((RelativeLayout) root.findViewById(R.id.file_view_webview_container)).addView(webView);
                webViewAdded = true;
            }
        }
    }

    private void applyThemeToWebView() {
        Context context = getContext();
        if (context instanceof MainActivity && webView != null && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int defaultNight = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            MainActivity activity = (MainActivity) context;
            WebSettingsCompat.setForceDark(webView.getSettings(), (activity.getDarkModeAppSetting().equals(MainActivity.APP_SETTING_DARK_MODE_NIGHT) || (activity.getDarkModeAppSetting().equals(MainActivity.APP_SETTING_DARK_MODE_SYSTEM) && defaultNight == Configuration.UI_MODE_NIGHT_YES)) ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF);
        }
    }

    private void logUrlEvent(String url) {
        Bundle bundle = new Bundle();
        bundle.putString("uri", url);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_OPEN_FILE_PAGE, bundle);
    }

    private void checkAndResetNowPlayingClaim() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (MainActivity.nowPlayingClaim != null
                && actualClaim != null &&
                !MainActivity.nowPlayingClaim.getClaimId().equalsIgnoreCase(actualClaim.getClaimId())) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.clearNowPlayingClaim();
                if (actualClaim != null && !actualClaim.isPlayable()) {
                    MainActivity.stopExoplayer();
                }
            }
        }
    }

    private void onNewClaim(String url) {
        loadingNewClaim = true;
//        initialFileLoadDone = false;
        playbackStarted = false;
        currentUrl = url;
        logUrlEvent(url);
        resetViewCount();
        resetFee();

        View root = getView();
        if (root != null) {
            if (relatedContentAdapter != null) {
                relatedContentAdapter.clearItems();
            }
            if (commentListAdapter != null) {
                commentListAdapter.clearItems();
            }
            ((RecyclerView) root.findViewById(R.id.file_view_related_content_list)).setAdapter(null);
            ((RecyclerView) root.findViewById(R.id.file_view_comments_list)).setAdapter(null);
        }
    }

    private void loadFile() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        String claimId = actualClaim.getClaimId();
        FileListTask task = new FileListTask(claimId, null, new FileListTask.FileListResultHandler() {
            @Override
            public void onSuccess(List<LbryFile> files, boolean hasReachedEnd) {
                if (files.size() > 0) {
                    actualClaim.setFile(files.get(0));
                    checkIsFileComplete();
                    if (!actualClaim.isPlayable() && !actualClaim.isViewable()) {
                        showUnsupportedView();
                    }
                } else {
                    if (!actualClaim.isPlayable() && !actualClaim.isViewable()) {
                        restoreMainActionButton();
                    }
                }

//                initialFileLoadDone = true;
            }

            @Override
            public void onError(Exception error) {
//                initialFileLoadDone = true;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
/*
    public void openClaimUrl(String url) {
        resetViewCount();
        resetFee();
        currentUrl = url;

        ClaimCacheKey key = new ClaimCacheKey();
        key.setUrl(currentUrl);
        Claim oldClaim = claim;
        claim = null;
        if (Lbry.claimCache.containsKey(key)) {
            claim = Lbry.claimCache.get(key);
            if (oldClaim != null && oldClaim.getClaimId().equalsIgnoreCase(claim.getClaimId())) {
                // same claim
                return;
            }
        } else {
            resolveUrl(currentUrl);
        }

        resetMedia();
        onNewClaim(currentUrl);
        Helper.setWunderbarValue(currentUrl, getContext());

        if (claim != null) {
            Helper.saveViewHistory(url, claim);
            if (Helper.isClaimBlocked(claim)) {
                renderClaimBlocked();
            } else {
                checkAndLoadRelatedContent();
                checkAndLoadComments();
                renderClaim();
            }
        }
    }

    public void resetMedia() {
        View root = getView();
        if (root != null) {
            PlayerView view = root.findViewById(R.id.file_view_exoplayer_view);
            view.setShutterBackgroundColor(Color.BLACK);
            root.findViewById(R.id.file_view_exoplayer_container).setVisibility(View.GONE);
        }
        if (MainActivity.appPlayer != null) {
            MainActivity.appPlayer.stop();
        }
        resetPlayer();
    }
*/
    public void onResume() {
        super.onResume();
        checkParams();

        Context context = getContext();
        Helper.setWunderbarValue(currentUrl, context);
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "File", "File");
            activity.updateCurrentDisplayFragment(this);
            if (actualClaim != null && actualClaim.isPlayable() && activity.isInFullscreenMode()) {
                enableFullScreenMode();
            }
            activity.findViewById(R.id.appbar).setFitsSystemWindows(false);

            activity.refreshChannelCreationRequired(getView());
        }

        if (MainActivity.appPlayer != null) {
            if (MainActivity.playerReassigned) {
                setPlayerForPlayerView();
                MainActivity.playerReassigned = false;
            }

            View root = getView();
            if (root != null) {
                PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
                if (playerView.getPlayer() == null) {
                    playerView.setPlayer(MainActivity.appPlayer);
                }
            }

            updatePlaybackSpeedView(root);
            updateQualityView(root);
            loadAndScheduleDurations();
        }

        checkOwnClaim();
        fetchChannels();
        applyFilterForBlockedChannels(Lbryio.blockedChannels);
    }

    public void onPause() {
        if (MainActivity.appPlayer != null) {
            MainActivity.nowPlayingSource = MainActivity.SOURCE_NOW_PLAYING_FILE;
        }
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).updateMiniPlayerMargins(true);
        }
        super.onPause();
    }

    public void onStop() {
        super.onStop();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.removeDownloadActionListener(this);
            activity.removeFetchClaimsListener(this);
            activity.removePIPModeListener(this);
            activity.removeScreenOrientationListener(this);
            activity.removeStoragePermissionListener(this);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            activity.showAppBar();
            activity.checkNowPlaying();

            activity.resetCurrentDisplayFragment();
        }

        closeWebView();

        // Tasks on the scheduled executor needs to be really terminated to avoid
        // crashes if user presses back after going to a related content from here
        purgeLoadingReactionsTask();
    }

    private void purgeLoadingReactionsTask() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown() && futureReactions != null) {
            try {
                // .cancel() will not remove the task, so it is needed to .purge()
                futureReactions.cancel(true);
                ((ScheduledThreadPoolExecutor) scheduledExecutor).purge();
                scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeWebView() {
        if (webView != null) {
            webView.removeAllViews();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        webViewAdded = false;
    }

    private void setPlayerForPlayerView() {
        View root = getView();
        if (root != null) {
            PlayerView view = root.findViewById(R.id.file_view_exoplayer_view);
            view.setVisibility(View.VISIBLE);
            view.setPlayer(null);
            view.setPlayer(MainActivity.appPlayer);
        }
    }

    private final View.OnClickListener bellIconListener = new View.OnClickListener()  {
        @Override
        public void onClick(View view) {
            Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
            // View is not displayed when user is not signed in, so no need to check for it
            if (actualClaim != null && actualClaim.getSigningChannel() != null) {
                Claim publisher = actualClaim.getSigningChannel();
                boolean isNotificationsDisabled = Lbryio.isNotificationsDisabled(publisher);
                final Subscription subscription = Subscription.fromClaim(publisher);
                subscription.setNotificationsDisabled(!isNotificationsDisabled);
                view.setEnabled(false);
                Context context = getContext();
                new ChannelSubscribeTask(context, publisher.getClaimId(), subscription, false, new ChannelSubscribeTask.ChannelSubscribeHandler() {
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
    };

    private final View.OnClickListener followUnfollowListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            // TODO Extract this code to MainActivity and update views state from there for any currently visible fragments
            MainActivity activity = (MainActivity) getActivity();

            if (activity != null && activity.isSignedIn()) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null && actualClaim.getSigningChannel() != null) {
                    Claim publisher = actualClaim.getSigningChannel();
                    boolean isFollowing = Lbryio.isFollowing(publisher);
                    if (isFollowing) {
                        // show unfollow confirmation
                        Context context = getContext();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).
                                setTitle(R.string.confirm_unfollow).
                                setMessage(R.string.confirm_unfollow_message)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        doFollowUnfollow(true, view);
                                    }
                                }).setNegativeButton(R.string.no, null);
                        builder.show();
                    } else {
                        doFollowUnfollow(false, view);
                    }
                }
            } else {
                if (activity != null) {
                    activity.simpleSignIn(0);
                }
            }
        }
    };

    private void doFollowUnfollow(boolean isFollowing, View view) {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null && actualClaim.getSigningChannel() != null) {
            Claim publisher = actualClaim.getSigningChannel();
            Subscription subscription = Subscription.fromClaim(publisher);
            view.setEnabled(false);
            Context context = getContext();
            new ChannelSubscribeTask(context, publisher.getClaimId(), subscription, isFollowing, new ChannelSubscribeTask.ChannelSubscribeHandler() {
                @Override
                public void onSuccess() {
                    if (isFollowing) {
                        Lbryio.removeSubscription(subscription);
                        Lbryio.removeCachedResolvedSubscription(publisher);
                    } else {
                        Lbryio.addSubscription(subscription);
                        Lbryio.addCachedResolvedSubscription(publisher);
                    }
                    view.setEnabled(true);
                    checkIsFollowing();
                    FollowingFragment.resetClaimSearchContent = true;

                    // Save shared user state
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

    private void resolveUrl(String url) {
//        resolving = true;
        Helper.setViewVisibility(layoutDisplayArea, View.INVISIBLE);
        Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
        Helper.setViewVisibility(layoutNothingAtLocation, View.GONE);
        ResolveTask task = new ResolveTask(url, Lbry.API_CONNECTION_STRING, layoutResolving, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                if (claims.size() > 0 && !Helper.isNullOrEmpty(claims.get(0).getClaimId())) {
                    fileClaim = claims.get(0);
                    if (Claim.TYPE_REPOST.equalsIgnoreCase(fileClaim.getValueType())) {
                        fileClaim = fileClaim.getRepostedClaim();
                        // cache the reposted claim too for subsequent loads
                        Lbry.addClaimToCache(fileClaim);
                        if (fileClaim.getName().startsWith("@")) {
                            // this is a reposted channel, so finish this activity and launch the channel url
                            Context context = getContext();
                            if (context instanceof  MainActivity) {
                                MainActivity activity = (MainActivity) context;
                                activity.getSupportFragmentManager().popBackStack();
                                activity.openChannelUrl(!Helper.isNullOrEmpty(fileClaim.getShortUrl()) ? fileClaim.getShortUrl() : fileClaim.getPermanentUrl());
                            }
                            return;
                        }
                    } else {
                        Lbry.addClaimToCache(fileClaim);
                    }

                    if (Claim.TYPE_COLLECTION.equalsIgnoreCase(fileClaim.getValueType()) &&  fileClaim.getClaimIds() != null && fileClaim.getClaimIds().size() > 0) {
                        collectionClaimItem = null;
                    }

                    Helper.saveUrlHistory(url, fileClaim.getTitle(), UrlSuggestion.TYPE_FILE);

                    // also save view history
                    Helper.saveViewHistory(url, fileClaim);

                    checkAndResetNowPlayingClaim();

                    if (Helper.isClaimBlocked(fileClaim)) {
                        renderClaimBlocked();
                    } else {
                        loadFile();
                        checkAndLoadRelatedContent();
                        checkAndLoadComments();
                        renderClaim();
                    }
                } else {
                    // render nothing at location
                    renderNothingAtLocation();
                }
            }

            @Override
            public void onError(Exception error) {
//                resolving = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void initUi(View root) {
        buttonPublishSomething.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (!Helper.isNullOrEmpty(currentUrl) && context instanceof MainActivity) {
                    LbryUri uri = LbryUri.tryParse(currentUrl);
                    if (uri != null) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("suggestedUrl", uri.getStreamName());
//                        ((MainActivity) context).openFragment(PublishFragment.class, true, NavMenuItem.ID_ITEM_NEW_PUBLISH, params);
                    }
                }
            }
        });

        root.findViewById(R.id.file_view_title_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView descIndicator = root.findViewById(R.id.file_view_desc_toggle_arrow);
                View descriptionArea = root.findViewById(R.id.file_view_description_area);

                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                boolean hasDescription = actualClaim != null && !Helper.isNullOrEmpty(actualClaim.getDescription());
                boolean hasTags = actualClaim != null && actualClaim.getTags() != null && actualClaim.getTags().size() > 0;

                if (descriptionArea.getVisibility() != View.VISIBLE) {
                    if (hasDescription || hasTags) {
                        descriptionArea.setVisibility(View.VISIBLE);
                    }
                    descIndicator.setImageResource(R.drawable.ic_arrow_dropup);
                } else {
                    descriptionArea.setVisibility(View.GONE);
                    descIndicator.setImageResource(R.drawable.ic_arrow_dropdown);
                }
            }
        });

        root.findViewById(R.id.file_view_action_like).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager am = AccountManager.get(root.getContext());
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null && odyseeAccount != null) {
                    react(actualClaim, true);
                }
            }
        });
        root.findViewById(R.id.file_view_action_dislike).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager am = AccountManager.get(root.getContext());
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null && odyseeAccount != null) {
                    react(actualClaim, false);
                }
            }
        });
        root.findViewById(R.id.file_view_action_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    try {
                        String shareUrl = LbryUri.parse(
                                !Helper.isNullOrEmpty(actualClaim.getCanonicalUrl()) ? actualClaim.getCanonicalUrl() :
                                        (!Helper.isNullOrEmpty(actualClaim.getShortUrl()) ? actualClaim.getShortUrl() : actualClaim.getPermanentUrl())).toOdyseeString();
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

        root.findViewById(R.id.file_view_action_tip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();

                if (activity != null && activity.isSignedIn()) {
                    Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                    if (actualClaim != null) {
                        CreateSupportDialogFragment dialog = CreateSupportDialogFragment.newInstance(actualClaim, (amount, isTip) -> {
                            double sentAmount = amount.doubleValue();
                            String message = getResources().getQuantityString(
                                    isTip ? R.plurals.you_sent_a_tip : R.plurals.you_sent_a_support, sentAmount == 1.0 ? 1 : 2,
                                    new DecimalFormat("#,###.##").format(sentAmount));
                            Snackbar.make(root.findViewById(R.id.file_view_claim_display_area), message, Snackbar.LENGTH_LONG).show();
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

        root.findViewById(R.id.file_view_action_repost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    RepostClaimDialogFragment dialog = RepostClaimDialogFragment.newInstance(actualClaim, claim -> {
                        Context context = getContext();
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).showMessage(R.string.content_successfully_reposted);
                        }
                    });
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        dialog.show(((MainActivity) context).getSupportFragmentManager(), RepostClaimDialogFragment.TAG);
                    }
                }
            }
        });

        root.findViewById(R.id.file_view_action_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null && context instanceof MainActivity) {
                    ((MainActivity) context).openPublishForm(actualClaim);
                }
            }
        });

        root.findViewById(R.id.file_view_action_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).
                        setTitle(R.string.delete_file).
                        setMessage(R.string.confirm_delete_file_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteClaimFile();
                            }
                        }).setNegativeButton(R.string.no, null);
                    builder.show();
                }
            }
        });

        root.findViewById(R.id.file_view_action_unpublish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).
                        setTitle(R.string.delete_content).
                        setMessage(R.string.confirm_delete_content_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteCurrentClaim();
                            }
                        }).setNegativeButton(R.string.no, null);
                    builder.show();
                }
            }
        });

        root.findViewById(R.id.file_view_action_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    if (downloadInProgress) {
                        onDownloadAborted();
                    } else {
                        checkStoragePermissionAndStartDownload();
                    }
                }
            }
        });

        root.findViewById(R.id.file_view_action_report).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null) {
                    Context context = getContext();
                    CustomTabColorSchemeParams.Builder ctcspb = new CustomTabColorSchemeParams.Builder();
                    ctcspb.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    CustomTabColorSchemeParams ctcsp = ctcspb.build();

                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder().setDefaultColorSchemeParams(ctcsp);
                    CustomTabsIntent intent = builder.build();
                    intent.launchUrl(context, Uri.parse(String.format("https://odysee.com/$/report_content?claimId=%s", actualClaim.getClaimId())));
                }
            }
        });

        root.findViewById(R.id.player_toggle_cast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCast();
            }
        });

        PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
        View playbackSpeedContainer = playerView.findViewById(R.id.player_playback_speed);
        TextView textPlaybackSpeed = playerView.findViewById(R.id.player_playback_speed_label);
        View qualityContainer = playerView.findViewById(R.id.player_quality);
        TextView textQuality = playerView.findViewById(R.id.player_quality_label);
        textPlaybackSpeed.setText(DEFAULT_PLAYBACK_SPEED);
        textQuality.setText(AUTO_QUALITY_STRING);

        playbackSpeedContainer.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                Helper.buildPlaybackSpeedMenu(contextMenu);
            }
        });
        playbackSpeedContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openContextMenu(playbackSpeedContainer);
                }
            }
        });

        qualityContainer.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                if (MainActivity.appPlayer != null) {
                    Helper.buildQualityMenu(contextMenu, MainActivity.appPlayer, MainActivity.videoIsTranscoded);
                }
            }
        });
        qualityContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openContextMenu(qualityContainer);
                }
            }
        });

        playerView.findViewById(R.id.player_toggle_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check full screen mode
                if (isInFullscreenMode()) {
                    disableFullScreenMode();
                } else {
                    enableFullScreenMode();
                }
            }
        });
        playerView.findViewById(R.id.player_skip_back_10).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.appPlayer != null) {
                    MainActivity.appPlayer.seekTo(Math.max(0, MainActivity.appPlayer.getCurrentPosition() - 10000));
                }
            }
        });
        playerView.findViewById(R.id.player_skip_forward_10).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.appPlayer != null) {
                    MainActivity.appPlayer.seekTo(MainActivity.appPlayer.getCurrentPosition() + 10000);
                }
            }
        });

        root.findViewById(R.id.file_view_publisher_info_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
                if (actualClaim != null && actualClaim.getSigningChannel() != null) {
                    removeNotificationAsSource();

                    Claim publisher = actualClaim.getSigningChannel();
                    Context context = getContext();
                    if (context instanceof  MainActivity) {
                        ((MainActivity) context).openChannelClaim(publisher);
                    }
                }
            }
        });

        View buttonFollow = root.findViewById(R.id.file_view_icon_follow);
        View buttonUnfollow = root.findViewById(R.id.file_view_icon_unfollow);
        View buttonBell = root.findViewById(R.id.file_view_icon_bell);
        buttonFollow.setOnClickListener(followUnfollowListener);
        buttonUnfollow.setOnClickListener(followUnfollowListener);
        buttonBell.setOnClickListener(bellIconListener);

        commentChannelSpinnerAdapter = new InlineChannelSpinnerAdapter(getContext(), R.layout.spinner_item_channel, new ArrayList<>());
        commentChannelSpinnerAdapter.addPlaceholder(false);

        initCommentForm(root);
        expandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Prevents crash for when comment list isn't loaded yet but user tries to expand.
                if (commentListAdapter != null) {
                    switchCommentListVisibility(commentListAdapter.isCollapsed());
                    commentListAdapter.switchExpandedState();
                }
            }
        });

        singleCommentRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandButton.performClick();
            }
        });

        RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
        RecyclerView commentsList = root.findViewById(R.id.file_view_comments_list);
        relatedContentList.setNestedScrollingEnabled(false);
        commentsList.setNestedScrollingEnabled(false);
        LinearLayoutManager relatedContentListLLM = new LinearLayoutManager(getContext());
        LinearLayoutManager commentsListLLM = new LinearLayoutManager(getContext());
        relatedContentList.setLayoutManager(relatedContentListLLM);
        commentsList.setLayoutManager(commentsListLLM);

        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                ImageView seekOverlay = root.findViewById(R.id.seek_overlay);

                int width = playerView.getWidth();
                float eventX = e.getX();
                if (eventX < width / 3.0) {
                    if (MainActivity.appPlayer != null) {
                        MainActivity.appPlayer.seekTo(Math.max(0, MainActivity.appPlayer.getCurrentPosition() - 10000));

                        seekOverlay.setVisibility(View.VISIBLE);
                        seekOverlay.setImageResource(R.drawable.ic_rewind);
                    }
                } else if (eventX > width * 2.0 / 3.0) {
                    if (MainActivity.appPlayer != null) {
                        MainActivity.appPlayer.seekTo(MainActivity.appPlayer.getCurrentPosition() + 10000);

                        seekOverlay.setVisibility(View.VISIBLE);
                        seekOverlay.setImageResource(R.drawable.ic_forward);
                    }
                } else {
                    return true;
                }

                if (seekOverlayHandler == null) {
                    seekOverlayHandler = new Handler();
                } else {
                    seekOverlayHandler.removeCallbacksAndMessages(null); // Clear pending messages
                }
                seekOverlayHandler.postDelayed(() -> {
                    seekOverlay.setVisibility(View.GONE);
                }, 500);

                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (playerView.isControllerVisible()) {
                    playerView.hideController();
                } else {
                    playerView.showController();
                }
                return true;
            }
        };
        GestureDetector detector = new GestureDetector(getContext(), gestureListener);
        playerView.setOnTouchListener((view, motionEvent) -> {
            detector.onTouchEvent(motionEvent);
            return true;
        });
    }

    private void removeNotificationAsSource() {
        // If we arrived here from a notification, navigating to a channel
        // will show the notifications panel and a very weird layout after
        // it.
        // So let's avoid it by removing that flag. User will need to re-open
        // the panel again to keep visiting content from notifications
        Map<String, Object> params = getParams();
        if (params != null && params.containsKey("source")) {
            String notificationSource = (String) params.get("source");

            if ("notification".equalsIgnoreCase(notificationSource)) {
                params.remove("source");
                setParams(params);
            }
        }
    }

    private void updatePlaybackSpeedView(View root) {
        if (root != null) {
            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            TextView textPlaybackSpeed = playerView.findViewById(R.id.player_playback_speed_label);
            textPlaybackSpeed.setText(MainActivity.appPlayer != null && MainActivity.appPlayer.getPlaybackParameters() != null ?
                    Helper.getDisplayValueForPlaybackSpeed((double) MainActivity.appPlayer.getPlaybackParameters().speed) :
                    DEFAULT_PLAYBACK_SPEED);
        }
    }

    private void updateQualityView(View root) {
        if (root != null) {
            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            TextView textQuality = playerView.findViewById(R.id.player_quality_label);
            if (MainActivity.videoQuality == AUTO_QUALITY_ID) {
                textQuality.setText(AUTO_QUALITY_STRING);
            } else {
                textQuality.setText(String.format("%sp", MainActivity.videoQuality));
            }
        }
    }

    private void deleteCurrentClaim() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            Helper.setViewVisibility(layoutDisplayArea, View.INVISIBLE);
            Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
            Helper.setViewVisibility(layoutNothingAtLocation, View.GONE);
            AbandonStreamTask task = new AbandonStreamTask(Arrays.asList(actualClaim.getClaimId()), layoutResolving, new AbandonHandler() {
                @Override
                public void onComplete(List<String> successfulClaimIds, List<String> failedClaimIds, List<Exception> errors) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        if (failedClaimIds.size() == 0) {
                            MainActivity activity = (MainActivity) context;
                            activity.showMessage(R.string.content_deleted);
                            activity.onBackPressed();
                        } else {
                            showError(getString(R.string.content_failed_delete));
                        }
                    }
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void checkStoragePermissionAndStartDownload() {
        Context context = getContext();
        if (MainActivity.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) {
            startDownload();
        } else {
            if (storagePermissionRefusedOnce) {
                showStoragePermissionRefusedError();
                restoreMainActionButton();
                return;
            }

//            startDownloadPending = true;
            MainActivity.requestPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    MainActivity.REQUEST_STORAGE_PERMISSION,
                    getString(R.string.storage_permission_rationale_download),
                    context,
                    true);
        }
    }

    private void checkStoragePermissionAndFileGet() {
        Context context = getContext();
        if (!MainActivity.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) {
            if (storagePermissionRefusedOnce) {
                showStoragePermissionRefusedError();
                restoreMainActionButton();
                return;
            }

            try {
                if (context != null) {
//                    fileGetPending = true;
                    MainActivity.requestPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            MainActivity.REQUEST_STORAGE_PERMISSION,
                            getString(R.string.storage_permission_rationale_download),
                            context,
                            true);
                }
            } catch (IllegalStateException ex) {
                // pass
            }
        } else {
            fileGet(true);
        }
    }

    public void onStoragePermissionGranted() {

    }
    public void onStoragePermissionRefused() {
        storagePermissionRefusedOnce = true;
//        fileGetPending = false;
//        startDownloadPending = false;
        onDownloadAborted();

        showStoragePermissionRefusedError();
    }

    public void startDownload() {
        downloadInProgress = true;

        View root = getView();
        if (root != null) {
            Helper.setViewVisibility(root.findViewById(R.id.file_view_download_progress), View.VISIBLE);
            ((ImageView) root.findViewById(R.id.file_view_action_download_icon)).setImageResource(R.drawable.ic_stop);
        }

        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (!actualClaim.isFree()) {
            downloadRequested = true;
            onMainActionButtonClicked();
        } else {
            // download the file
            fileGet(true);
        }
    }

    private void deleteClaimFile() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            View actionDelete = getView().findViewById(R.id.file_view_action_delete);
            DeleteFileTask task = new DeleteFileTask(actualClaim.getClaimId(), new GenericTaskHandler() {
                @Override
                public void beforeStart() {
                    actionDelete.setEnabled(false);
                }

                @Override
                public void onSuccess() {
                    Helper.setViewVisibility(actionDelete, View.GONE);
                    View root = getView();
                    if (root != null) {
//                        root.findViewById(R.id.file_view_action_download).setVisibility(View.VISIBLE);
                        root.findViewById(R.id.file_view_unsupported_container).setVisibility(View.GONE);
                    }
                    Helper.setViewEnabled(actionDelete, true);

                    actualClaim.setFile(null);
                    Lbry.unsetFilesForCachedClaims(Arrays.asList(actualClaim.getClaimId()));

                    restoreMainActionButton();
                }

                @Override
                public void onError(Exception error) {
                    actionDelete.setEnabled(true);
                    if (error != null) {
                        showError(error.getMessage());
                    }
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void renderClaim() {
        Claim claimToRender = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (claimToRender == null) {
            return;
        }
        if (!claimToRender.hasSource()) {
            // TODO See if the "publisher is not live yet" UI must be shown
        }
        if (claimToRender.isPlayable() && MainActivity.appPlayer != null) {
            MainActivity.appPlayer.setPlayWhenReady(isPlaying);
        }

        Helper.setViewVisibility(layoutLoadingState, View.GONE);
        Helper.setViewVisibility(layoutNothingAtLocation, View.GONE);

/*
        if (claim.getTags().contains("disable-support") || claim.getSigningChannel().getTags().contains("disable-support"))
            Helper.setViewVisibility(tipButton, View.GONE);
        else
            Helper.setViewVisibility(tipButton, View.VISIBLE);
*/

        loadViewCount();
        loadReactions(claimToRender);
        checkIsFollowing();

        View root = getView();
        if (root != null) {
            Context context = getContext();

            root.findViewById(R.id.file_view_scroll_view).scrollTo(0, 0);
            Helper.setViewVisibility(layoutDisplayArea, View.VISIBLE);

            ImageView descIndicator = root.findViewById(R.id.file_view_desc_toggle_arrow);
            descIndicator.setImageResource(R.drawable.ic_arrow_dropdown);

            boolean hasDescription = !Helper.isNullOrEmpty(claimToRender.getDescription());
            boolean hasTags = claimToRender.getTags() != null && claimToRender.getTags().size() > 0;

            root.findViewById(R.id.file_view_description).setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.file_view_tag_area).setVisibility(hasTags ? View.VISIBLE : View.GONE);
            if (hasTags && !hasDescription) {
                root.findViewById(R.id.file_view_tag_area).setPadding(0, 0, 0, 0);
            }

            root.findViewById(R.id.file_view_description_area).setVisibility(View.GONE);
            ((TextView) root.findViewById(R.id.file_view_title)).setText(claimToRender.getTitle());
            ((TextView) root.findViewById(R.id.file_view_description)).setText(claimToRender.getDescription());
            ((TextView) root.findViewById(R.id.file_view_publisher_name)).setText(
                    Helper.isNullOrEmpty(claimToRender.getPublisherName()) ? getString(R.string.anonymous) : claimToRender.getPublisherName());

            Claim signingChannel = claimToRender.getSigningChannel();
            boolean hasPublisher = signingChannel != null;
            boolean hasPublisherThumbnail = hasPublisher && !Helper.isNullOrEmpty(signingChannel.getThumbnailUrl());
            root.findViewById(R.id.file_view_publisher_avatar).setVisibility(hasPublisher ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.file_view_publisher_thumbnail).setVisibility(hasPublisherThumbnail ? View.VISIBLE : View.INVISIBLE);
            root.findViewById(R.id.file_view_publisher_no_thumbnail).setVisibility(!hasPublisherThumbnail ? View.VISIBLE : View.INVISIBLE);
            if (hasPublisher) {
                int bgColor = Helper.generateRandomColorForValue(signingChannel.getClaimId());
                Helper.setIconViewBackgroundColor(root.findViewById(R.id.file_view_publisher_no_thumbnail), bgColor, false, context);
                if (hasPublisherThumbnail && context != null) {
                    ViewGroup.LayoutParams lp = root.findViewById(R.id.file_view_publisher_thumbnail).getLayoutParams();
                    Glide.with(context.getApplicationContext()).load(signingChannel.getThumbnailUrl(lp.width, lp.height, 85)).
                            apply(RequestOptions.circleCropTransform()).into((ImageView) root.findViewById(R.id.file_view_publisher_thumbnail));
                }
                ((TextView) root.findViewById(R.id.file_view_publisher_thumbnail_alpha)).
                        setText(signingChannel.getName() != null ? signingChannel.getName().substring(1, 2).toUpperCase() : null);

            }

            String publisherTitle = signingChannel != null ? signingChannel.getTitle() : null;
            TextView textPublisherTitle = root.findViewById(R.id.file_view_publisher_title);
            textPublisherTitle.setVisibility(Helper.isNullOrEmpty(publisherTitle) ? View.GONE : View.VISIBLE);
            textPublisherTitle.setText(publisherTitle);

            RecyclerView descTagsList = root.findViewById(R.id.file_view_tag_list);
            FlexboxLayoutManager flm = new FlexboxLayoutManager(context);
            descTagsList.setLayoutManager(flm);

            List<Tag> tags = claimToRender.getTagObjects();
            TagListAdapter tagListAdapter = new TagListAdapter(tags, context);
            tagListAdapter.setClickListener(new TagListAdapter.TagClickListener() {
                @Override
                public void onTagClicked(Tag tag, int customizeMode) {
                    if (customizeMode == TagListAdapter.CUSTOMIZE_MODE_NONE) {
                        Context ctx = getContext();
                        if (ctx instanceof MainActivity) {
                            ((MainActivity) ctx).openAllContentFragmentWithTag(tag.getName());
                        }
                    }
                }
            });
            descTagsList.setAdapter(tagListAdapter);
            root.findViewById(R.id.file_view_tag_area).setVisibility(tags.size() > 0 ? View.VISIBLE : View.GONE);

            root.findViewById(R.id.file_view_exoplayer_container).setVisibility(View.GONE);
            root.findViewById(R.id.file_view_unsupported_container).setVisibility(View.GONE);
            root.findViewById(R.id.file_view_media_meta_container).setVisibility(View.VISIBLE);

            Claim.GenericMetadata metadata = claimToRender.getValue();
            if (!Helper.isNullOrEmpty(claimToRender.getThumbnailUrl())) {
                ImageView thumbnailView = root.findViewById(R.id.file_view_thumbnail);
                Glide.with(context.getApplicationContext()).asBitmap().load(
                        claimToRender.getThumbnailUrl(context.getResources().getDisplayMetrics().widthPixels, thumbnailView.getLayoutParams().height, 85)).centerCrop().into(thumbnailView);
            } else {
                // display first x letters of claim name, with random background
            }

            root.findViewById(R.id.file_view_main_action_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onMainActionButtonClicked();
                }
            });
            root.findViewById(R.id.file_view_media_meta_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onMainActionButtonClicked();
                }
            });
            root.findViewById(R.id.file_view_open_external_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openClaimExternally(claimToRender, claimToRender.getMediaType());
                }
            });

            if (metadata instanceof Claim.StreamMetadata) {
                Claim.StreamMetadata streamMetadata = (Claim.StreamMetadata) metadata;
                long publishTime = streamMetadata.getReleaseTime() > 0 ? streamMetadata.getReleaseTime() * 1000 : claimToRender.getTimestamp() * 1000;
                ((TextView) root.findViewById(R.id.file_view_publish_time)).setText(DateUtils.getRelativeTimeSpanString(
                        publishTime, System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_RELATIVE));

                Fee fee = streamMetadata.getFee();
                if (fee != null && Helper.parseDouble(fee.getAmount(), 0) > 0) {
                    root.findViewById(R.id.file_view_fee_container).setVisibility(View.VISIBLE);
                    ((TextView) root.findViewById(R.id.file_view_fee)).setText(
                            Helper.shortCurrencyFormat(claimToRender.getActualCost(Lbryio.LBCUSDRate).doubleValue()));
                }
            }

            boolean isAnonymous = claimToRender.getSigningChannel() == null;
            View iconFollow = root.findViewById(R.id.file_view_icon_follow);
            View iconUnfollow = root.findViewById(R.id.file_view_icon_unfollow);
            if (isAnonymous) {
                if (iconFollow.getVisibility() == View.VISIBLE) {
                    iconFollow.setVisibility(View.INVISIBLE);
                }
                if (iconUnfollow.getVisibility() == View.VISIBLE) {
                    iconUnfollow.setVisibility(View.INVISIBLE);
                }
            }

            MaterialButton mainActionButton = root.findViewById(R.id.file_view_main_action_button);
            if (claimToRender.isPlayable()) {
                mainActionButton.setText(R.string.play);
            } else if (claimToRender.isViewable()) {
                mainActionButton.setText(R.string.view);
            } else {
                mainActionButton.setText(R.string.download);
            }
        }

        if (claimToRender.isFree() && Helper.isNullOrEmpty(commentHash)) {
            if (claimToRender.isPlayable() || (!Lbry.SDK_READY && Lbryio.isSignedIn())) {
                if (MainActivity.nowPlayingClaim != null && MainActivity.nowPlayingClaim.getClaimId().equalsIgnoreCase(claimToRender.getClaimId())) {
                    // claim already playing
                    showExoplayerView();
                    playMedia();
                } else {
                    onMainActionButtonClicked();
                }
            } else if (claimToRender.isViewable() && Lbry.SDK_READY) {
                onMainActionButtonClicked();
            } else if (!Lbry.SDK_READY) {
                restoreMainActionButton();
            }
        } else {
            restoreMainActionButton();
        }

        /*if (Lbry.SDK_READY && !claimToRender.isPlayable() && !claimToRender.isViewable() && Helper.isNullOrEmpty(commentHash)) {
            if (claimToRender.getFile() == null) {
                loadFile();
            } else {
                // file already loaded, but it's unsupported
                showUnsupportedView();
            }
        }*/

        checkRewardsDriver();
        checkOwnClaim();
    }

    private void checkAndLoadRelatedContent() {
        if (playlistResolved) {
            return;
        }

        View root = getView();
        if (root != null) {
            RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
            if (relatedContentList == null || relatedContentList.getAdapter() == null || relatedContentList.getAdapter().getItemCount() == 0) {
                loadRelatedContent();
            }
        }
    }

    private void checkAndLoadComments() {
        checkAndLoadComments(false);
    }

    private void checkAndLoadComments(boolean forceReload) {
        View root = getView();
        if (root != null) {
            Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;

            View expandCommentArea = root.findViewById(R.id.expand_commentarea_button);
            View commentsDisabledText = root.findViewById(R.id.file_view_disabled_comments);
            RecyclerView commentsList = root.findViewById(R.id.file_view_comments_list);

            commentLoadingArea.setVisibility(View.VISIBLE);
            commentEnabledCheck.checkCommentStatus(
                    actualClaim.getSigningChannel().getClaimId(), actualClaim.getSigningChannel().getName(), (CommentEnabledCheck.CommentStatus) isEnabled -> {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (isEnabled) {
                            showComments(expandCommentArea, commentsDisabledText, commentsList);
                            if ((commentsList != null && forceReload) || (commentsList == null || commentsList.getAdapter() == null || commentsList.getAdapter().getItemCount() == 0)) {
                                loadComments();
                            }
                        } else {
                            hideComments(expandCommentArea, commentsDisabledText, commentsList);
                        }
                        commentLoadingArea.setVisibility(View.GONE);
                    });
                }
            });
        }
    }

    private void showComments(View root, View commentsDisabledText, View commentsList) {
        root.findViewById(R.id.expand_commentarea_button).setVisibility(View.VISIBLE);
        Helper.setViewVisibility(commentsDisabledText, View.GONE);
        Helper.setViewVisibility(commentsList, View.VISIBLE);
    }

    private void hideComments(View root, View commentsDisabledText, View commentsList) {
        root.findViewById(R.id.expand_commentarea_button).setVisibility(View.GONE);
        Helper.setViewVisibility(commentsDisabledText, View.VISIBLE);
        Helper.setViewVisibility(commentsList, View.GONE);
    }

    private void showUnsupportedView() {
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.file_view_exoplayer_container).setVisibility(View.GONE);
            root.findViewById(R.id.file_view_unsupported_container).setVisibility(View.VISIBLE);
            String fileNameString = "";
            Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
            if (actualClaim.getFile() != null && !Helper.isNullOrEmpty(actualClaim.getFile().getDownloadPath())) {
                LbryFile lbryFile = actualClaim.getFile();
                File file = new File(lbryFile.getDownloadPath());
                fileNameString = String.format("\"%s\" ", file.getName());
            }
            ((TextView) root.findViewById(R.id.file_view_unsupported_text)).setText(getString(R.string.unsupported_content_desc, fileNameString));
        }
    }

    private void showExoplayerView() {
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.file_view_unsupported_container).setVisibility(View.GONE);
            root.findViewById(R.id.file_view_exoplayer_container).setVisibility(View.VISIBLE);
        }
    }

    private void playMedia() {
        boolean newPlayerCreated = false;

        Context context = getContext();
        if (MainActivity.appPlayer == null && context != null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MOVIE)
                    .build();

            MainActivity.appPlayer = new SimpleExoPlayer.Builder(context).build();
            MainActivity.appPlayer.setWakeMode(C.WAKE_MODE_NETWORK);

            MainActivity.appPlayer.setAudioAttributes(audioAttributes, true);
            MainActivity.playerCache =
                    new SimpleCache(context.getCacheDir(),
                            new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 256), new ExoDatabaseProvider(context));
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.initMediaSession();
                activity.initPlaybackNotification();
            }

            newPlayerCreated = true;
        }

        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.initPlaybackNotification();
        }

        Claim claimToPlay = collectionClaimItem != null ? collectionClaimItem : fileClaim;

        View root = getView();
        if (root != null) {
            PlayerView view = root.findViewById(R.id.file_view_exoplayer_view);
            view.setShutterBackgroundColor(Color.TRANSPARENT);
            view.setPlayer(MainActivity.appPlayer);
            view.getPlayer().addListener(new Player.Listener() {
                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    isPlaying = playWhenReady;
                    Player.Listener.super.onPlayWhenReadyChanged(playWhenReady, reason);
                }
            });
            view.setUseController(true);
            if (context instanceof MainActivity) {
                ((MainActivity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            if (MainActivity.nowPlayingClaim != null &&
                    MainActivity.nowPlayingClaim.getClaimId().equalsIgnoreCase(claimToPlay.getClaimId()) &&
                    !newPlayerCreated) {
                // if the claim is already playing, we don't need to reload the media source
                if (MainActivity.appPlayer != null) {
                    MainActivity.appPlayer.setPlayWhenReady(true);
                    playbackStarted = true;

                    // reconnect the app player
                    if (fileViewPlayerListener != null) {
                        MainActivity.appPlayer.addListener(fileViewPlayerListener);
                    }
                    setPlayerForPlayerView();
                    loadAndScheduleDurations();
                }
                return;
            }

            if (MainActivity.appPlayer != null) {
                showBuffering();
                if (fileViewPlayerListener != null) {
                    MainActivity.appPlayer.addListener(fileViewPlayerListener);
                }
                if (context instanceof MainActivity) {
                    ((MainActivity) context).setNowPlayingClaim(claimToPlay, currentUrl);
                }

                MainActivity.appPlayer.setPlayWhenReady(Objects.requireNonNull((MainActivity) (getActivity())).isMediaAutoplayEnabled());

                if (claimToPlay.hasSource()) {
                    getStreamingUrlAndInitializePlayer(claimToPlay);
                } else {
                    String mediaSourceUrl = getLivestreamUrl();
                    DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
                    if (context != null) {
                        dataSourceFactory.setUserAgent(Util.getUserAgent(context, getString(R.string.app_name)));
                    }
                    MediaSource mediaSource = null;

                    if (mediaSourceUrl != null) {
                        if (!mediaSourceUrl.equals("notlive")) {
                            Map<String, String> defaultRequestProperties = new HashMap<>(1);
                            defaultRequestProperties.put("Referer", "https://bitwave.tv");
                            dataSourceFactory.setDefaultRequestProperties(defaultRequestProperties);
                            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mediaSourceUrl));
                        } else {
                            if (claimToPlay.getThumbnailUrl() != null && context != null) {
                                ImageView thumbnailView = root.findViewById(R.id.file_view_livestream_thumbnail);
                                Glide.with(context.getApplicationContext()).
                                        asBitmap().
                                        load(claimToPlay.getThumbnailUrl()).
                                        apply(RequestOptions.circleCropTransform()).
                                        into(thumbnailView);
                            }

                            root.findViewById(R.id.file_view_livestream_not_live).setVisibility(View.VISIBLE);
                            TextView userNotStreaming = root.findViewById(R.id.user_not_streaming);
                            userNotStreaming.setText(getString(R.string.user_not_live_yet, claimToPlay.getPublisherName()));
                            userNotStreaming.setVisibility(View.VISIBLE);
                        }
                    }

                    if (mediaSource != null) {
                        MainActivity.appPlayer.setMediaSource(mediaSource, true);
                        MainActivity.appPlayer.prepare();
                    }
                }
            }
        }
    }

    private void getStreamingUrlAndInitializePlayer(Claim theClaim) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Get the streaming URL
                Map<String, Object> params = new HashMap<>();
                params.put("uri", theClaim.getPermanentUrl());
                JSONObject result = (JSONObject) Lbry.parseResponse(Lbry.apiCall(Lbry.METHOD_GET, params));
                String sourceUrl = (String) result.get("streaming_url");
                currentMediaSourceUrl = sourceUrl;

                // Get the stream type
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(sourceUrl)
                        .head()
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String contentType = response.header("Content-Type");
                    MainActivity.videoIsTranscoded = contentType.equals("application/x-mpegurl"); // m3u8
                }

                new Handler(Looper.getMainLooper()).post(() -> initializePlayer(sourceUrl));
            } catch (LbryRequestException | LbryResponseException | JSONException | IOException ex) {
                // TODO: How does error handling work here
                ex.printStackTrace();
            }
        });
        executor.shutdown();
    }
    private void initializePlayer(String sourceUrl) {
        Context context = getContext();
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        if (context != null) {
            dataSourceFactory.setUserAgent(Util.getUserAgent(context, getString(R.string.app_name)));
        }

        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory();
        cacheDataSourceFactory.setUpstreamDataSourceFactory(dataSourceFactory);
        cacheDataSourceFactory.setCache(MainActivity.playerCache);

        MediaSource mediaSource;
        if (MainActivity.videoIsTranscoded) {
            mediaSource = new HlsMediaSource.Factory(cacheDataSourceFactory)
                    .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(sourceUrl));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(
                    cacheDataSourceFactory, new DefaultExtractorsFactory()
            )
                    .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(sourceUrl));
        }

        MainActivity.appPlayer.setMediaSource(mediaSource, true);
        MainActivity.appPlayer.prepare();
    }

    /**
     * @return The URL to connect to get the video stream, usually a .M3U8
     */
    @AnyThread
    private String getLivestreamUrl() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<JSONObject> task = new Supplier<JSONObject>() {
                @Override
                public JSONObject get() {
                    return getLivestreamData(actualClaim);
                }
            };
            CompletableFuture<JSONObject> completableFuture = CompletableFuture.supplyAsync(task);
            CompletableFuture<String> cf = completableFuture.thenApply(jsonData -> getLivestreamUrl(jsonData));
            try {
                return cf.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<JSONObject> callable = () -> getLivestreamData(actualClaim);
            Future<JSONObject> future = executor.submit(callable);

            for (;;) {
                if (future.isDone()) {
                    try {
                        JSONObject jsonData = future.get();
                        return getLivestreamUrl(jsonData);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private String getLivestreamUrl(JSONObject jsonData) {
        if (jsonData != null && jsonData.has("live")) {
            try {
                if (jsonData.getBoolean("live") && jsonData.has("url")) {
                    return jsonData.getString("url");
                } else {
                    return "notlive";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private JSONObject getLivestreamData(Claim claim) {
        String urlLivestream = String.format("https://api.live.odysee.com/v1/odysee/live/%s", claim.getSigningChannel().getClaimId());

        Request.Builder builder = new Request.Builder().url(urlLivestream);
        Request request = builder.build();

        OkHttpClient client = new OkHttpClient.Builder().build();

        try {
            Response resp = client.newCall(request).execute();
            String responseString = resp.body().string();
            resp.close();
            JSONObject json = new JSONObject(responseString);
            if (resp.code() >= 200 && resp.code() < 300) {
                if (json.isNull("data") || (json.has("success") && !json.getBoolean("success"))) {
                    return null;
                }

                return (JSONObject) json.get("data");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setCurrentPlayer(Player currentPlayer) {
        if (this.currentPlayer == currentPlayer) {
            return;
        }

        // View management.
//        if (currentPlayer == MainActivity.appPlayer) {
//            //localPlayerView.setVisibility(View.VISIBLE);
//            castControlView.hide();
//            ((ImageView) getView().findViewById(R.id.player_image_cast_toggle)).setImageResource(R.drawable.ic_cast);
//        } else /* currentPlayer == castPlayer */ {
//            castControlView.show();
//            ((ImageView) getView().findViewById(R.id.player_image_cast_toggle)).setImageResource(R.drawable.ic_cast_connected);
//        }

        // Player state management.
        long playbackPositionMs = C.TIME_UNSET;

        Player previousPlayer = this.currentPlayer;
        if (previousPlayer != null) {
            // Save state from the previous player.
            int playbackState = previousPlayer.getPlaybackState();
            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = previousPlayer.getCurrentPosition();
            }
            previousPlayer.stop();
            previousPlayer.clearMediaItems();
        }

        this.currentPlayer = currentPlayer;

        // Media queue management.
        /*if (currentPlayer == exoPlayer) {
            exoPlayer.prepare(concatenatingMediaSource);
        }*/
        currentPlayer.seekTo(playbackPositionMs);
        currentPlayer.setPlayWhenReady(true);
    }

    private void setPlaybackSpeedToDefault() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            int speed = ((MainActivity) context).playbackDefaultSpeed();
            setPlaybackSpeed(MainActivity.appPlayer, speed);
        }
    }

    private void setPlaybackSpeed(Player player, int speedId) {
        float speed = speedId / 100.0f;
        PlaybackParameters params = new PlaybackParameters(speed);
        player.setPlaybackParameters(params);

        updatePlaybackSpeedView(getView());
    }

    private void setPlayerQualityToDefault() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            boolean isOnMobileNetwork = isMeteredNetwork(context);
            int quality = isOnMobileNetwork ?
                    ((MainActivity) context).mobileDefaultQuality() :
                    ((MainActivity) context).wifiDefaultQuality();
            setPlayerQuality(MainActivity.appPlayer, quality);
        }
    }

    // Taken from NewPipe: https://github.com/TeamNewPipe/NewPipe/blob/5459a55406ae72783584f84c1a8410e10903ba8a/app/src/main/java/org/schabi/newpipe/util/ListHelper.java#L552-L566
    private boolean isMeteredNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetworkInfo() == null) {
            return false;
        }
        return connectivityManager.isActiveNetworkMetered();
    }

    private void setPlayerQuality(Player player, int quality) {
        TracksInfo tracksInfo = player.getCurrentTracksInfo();
        int selectedQuality = 0;

        for (TrackGroupInfo groupInfo : tracksInfo.getTrackGroupInfos()) {
            if (groupInfo.getTrackType() != C.TRACK_TYPE_VIDEO) continue;
            TrackGroup group = groupInfo.getTrackGroup();

            TrackSelectionOverrides overrides;
            if (quality == AUTO_QUALITY_ID || !MainActivity.videoIsTranscoded) {
                overrides = new TrackSelectionOverrides.Builder()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .build();
                // Force it to AUTO_QUALITY_ID to override the default quality setting on non-transcoded videos
                selectedQuality = AUTO_QUALITY_ID;
            } else {
                ArrayList<Integer> availableQualities = new ArrayList<>();
                for (int i = 0; i < group.length; i++) {
                    availableQualities.add(group.getFormat(i).height);
                }
                int selectedQualityIndex;
                // Check if the chosen quality is lower than the lowest available quality
                int lowestQuality = Collections.min(availableQualities);
                if (quality <= lowestQuality) { // <= short path for when the quality matches the lowest
                    selectedQuality = lowestQuality;
                    selectedQualityIndex = availableQualities.indexOf(lowestQuality);
                } else {
                    // Otherwise find the highest available quality that is less than the chosen quality
                    for (int i = 0; i < availableQualities.size(); i++) {
                        int q = availableQualities.get(i);
                        if (q <= quality && groupInfo.isTrackSupported(i) && q > selectedQuality) {
                            selectedQuality = q;
                        }
                    }
                    selectedQualityIndex = availableQualities.indexOf(selectedQuality);
                }

                if (selectedQualityIndex != -1) {
                    TrackSelectionOverride override = new TrackSelectionOverride(
                            group, Collections.singletonList(selectedQualityIndex));
                    overrides = new TrackSelectionOverrides.Builder()
                            .addOverride(override)
                            .build();
                } else { // If quality can't be found use Auto quality
                    overrides = new TrackSelectionOverrides.Builder()
                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                            .build();
                }
            }
            player.setTrackSelectionParameters(
                    player
                            .getTrackSelectionParameters()
                            .buildUpon()
                            .setTrackSelectionOverrides(overrides)
                            .build()
            );

            MainActivity.videoQuality = selectedQuality;
            updateQualityView(getView());

            break;
        }
    }

    private void resetViewCount() {
        View root = getView();
        if (root != null) {
            TextView textViewCount = root.findViewById(R.id.file_view_view_count);
            Helper.setViewText(textViewCount, null);
            Helper.setViewVisibility(textViewCount, View.GONE);
        }
    }
    private void resetFee() {
        View root = getView();
        if (root != null) {
            TextView feeView = root.findViewById(R.id.file_view_fee);
            feeView.setText(null);
            Helper.setViewVisibility(root.findViewById(R.id.file_view_fee_container), View.GONE);
        }
    }

    private void loadViewCount() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            FetchStatCountTask task = new FetchStatCountTask(
                    FetchStatCountTask.STAT_VIEW_COUNT, actualClaim.getClaimId(), null, new FetchStatCountTask.FetchStatCountHandler() {
                @Override
                public void onSuccess(int count) {
                    try {
                        String displayText = getResources().getQuantityString(R.plurals.view_count, count, NumberFormat.getInstance().format(count));
                        View root = getView();
                        if (root != null) {
                            TextView textViewCount = root.findViewById(R.id.file_view_view_count);
                            Helper.setViewText(textViewCount, displayText);
                            Helper.setViewVisibility(textViewCount, View.VISIBLE);
                        }
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

    private void loadReactions(Claim c) {
        if (scheduledExecutor == null) {
            scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        }
        if (futureReactions != null)
            futureReactions.cancel(true);

        if (reactions == null)
            reactions = new Reactions();

        Runnable runnable = () -> {
            Map<String, String> options = new HashMap<>();
            options.put("claim_ids", c.getClaimId());

            JSONObject data;
            try {
                data = (JSONObject) Lbryio.parseResponse(Lbryio.call("reaction", "list", options, Helper.METHOD_POST, getContext()));

                if (data != null && data.has("others_reactions")) {
                    JSONObject othersReactions = (JSONObject) data.get("others_reactions");
                    if (othersReactions.has(c.getClaimId())) {
                        int likesFromOthers = ((JSONObject) othersReactions.get(c.getClaimId())).getInt("like");
                        int dislikesFromOthers = ((JSONObject) othersReactions.get(c.getClaimId())).getInt("dislike");
                        reactions.setOthersLikes(likesFromOthers);
                        reactions.setOthersDislikes(dislikesFromOthers);
                    }
                }
                if (data != null && data.has("my_reactions")) {
                    JSONObject othersReactions = (JSONObject) data.get("my_reactions");
                    if (othersReactions.has(c.getClaimId())) {
                        int likes = ((JSONObject) othersReactions.get(c.getClaimId())).getInt("like");
                        reactions.setLiked(likes > 0);
                        c.setLiked(likes > 0);
                        int dislikes = ((JSONObject) othersReactions.get(c.getClaimId())).getInt("dislike");
                        reactions.setDisliked(dislikes > 0);
                        c.setDisliked(dislikes > 0);
                    }
                }
                updateContentReactions();
            } catch (LbryioRequestException | LbryioResponseException | JSONException e) {
                e.printStackTrace();
            }
        };

        futureReactions = scheduledExecutor.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
    }

    private void updateContentReactions() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int likes = reactions.isLiked() ? reactions.getOthersLikes() + 1 : reactions.getOthersLikes();
                    int dislikes = reactions.isDisliked() ? reactions.getOthersDislikes() + 1 : reactions.getOthersDislikes();
                    likeReactionAmount.setText(String.valueOf(likes));
                    dislikeReactionAmount.setText(String.valueOf(dislikes));

                    int inactiveColor = 0;
                    int fireActive = 0;
                    int slimeActive = 0;
                    Context context = getContext();
                    if (context != null) {
                        inactiveColor = ContextCompat.getColor(context, R.color.darkForeground);
                        fireActive = ContextCompat.getColor(context, R.color.fireActive);
                        slimeActive = ContextCompat.getColor(context, R.color.slimeActive);
                    }
                    if (reactions.isLiked()) {
                        likeReactionIcon.setColorFilter(fireActive, PorterDuff.Mode.SRC_IN);
                        likeReactionAmount.setTextColor(fireActive);
                    } else {
                        likeReactionIcon.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN);
                        likeReactionAmount.setTextColor(inactiveColor);
                    }

                    if (reactions.isDisliked()) {
                        dislikeReactionIcon.setColorFilter(slimeActive, PorterDuff.Mode.SRC_IN);
                        dislikeReactionAmount.setTextColor(slimeActive);
                    } else {
                        dislikeReactionIcon.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN);
                        dislikeReactionAmount.setTextColor(inactiveColor);
                    }
                }
            });
        }
    }

    private Map<String, Reactions> loadReactions(List<Comment> comments) {
        List<String> commentIds = new ArrayList<>();

        for (Comment c: comments) {
            commentIds.add(c.getId());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map<String, Reactions>> future = executor.submit(() -> {
            Comments.checkCommentsEndpointStatus();
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("comment_ids", TextUtils.join(",", commentIds));

            AccountManager am = AccountManager.get(getContext());
            Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
            if (odyseeAccount != null) {
                jsonParams.put("auth_token", am.peekAuthToken(odyseeAccount, "auth_token_type"));
            }

            if (Lbry.ownChannels.size() > 0) {
                jsonParams.put("channel_id", Lbry.ownChannels.get(0).getClaimId());
                jsonParams.put("channel_name", Lbry.ownChannels.get(0).getName());

                try {
                    JSONObject jsonChannelSign = Comments.channelSignName(jsonParams, jsonParams.getString("channel_id"), jsonParams.getString("channel_name"));

                    if (jsonChannelSign.has("signature") && jsonChannelSign.has("signing_ts")) {
                        jsonParams.put("signature", jsonChannelSign.getString("signature"));
                        jsonParams.put("signing_ts", jsonChannelSign.getString("signing_ts"));
                    }
                } catch (ApiCallException | JSONException e) {
                    e.printStackTrace();
                }
            }

            Map<String, Reactions> result = new HashMap<>();
            try {
                Response response = Comments.performRequest(jsonParams, "reaction.List");
                String responseString = response.body().string();
                response.close();

                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.has("result")) {
                    JSONObject jsonResult = jsonResponse.getJSONObject("result");
                    if (jsonResult.has("others_reactions")) {
                        JSONObject responseOthersReactions = jsonResult.getJSONObject("others_reactions");
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                            responseOthersReactions.keys().forEachRemaining(key -> {
                                try {
                                    result.put(key, getMyReactions(jsonResult, responseOthersReactions, key));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else { // Android versions prior to API 24 lack forEachRemaining()
                            Iterator<String> itr = responseOthersReactions.keys();
                            while (itr.hasNext()) {
                                try {
                                    String nextKey = itr.next();
                                    result.put(nextKey, getMyReactions(jsonResult, responseOthersReactions, nextKey));
                                } catch (JSONException e) {
                                    Log.e(TAG, "loadReactions for Comment: ".concat(e.getLocalizedMessage()));
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e ) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private Reactions getMyReactions(JSONObject jsonResult, JSONObject responseOthersReactions, String key) throws JSONException {
        JSONObject value = (JSONObject) responseOthersReactions.get(key);
        Reactions reactions = getReactionsForValue(value);

        if (jsonResult.has("my_reactions")) {
            JSONObject responseMyReactions = jsonResult.getJSONObject("my_reactions");
            if (responseMyReactions.has(key) && reactions != null) {
                JSONObject myReaction = (JSONObject) responseMyReactions.get(key);
                reactions.setLiked(myReaction.getInt("like") > 0);
                reactions.setDisliked(myReaction.getInt("dislike") > 0);
            }
        }
        return reactions;
    }


    private Reactions getReactionsForValue(JSONObject value) {
        try {
            return new Reactions(value.getInt("like"), value.getInt("dislike"));
        } catch (JSONException e) {
            Log.e(TAG, "getReactionsForValue: ".concat(e.getLocalizedMessage()));
            return null;
        }
    }




    private void onMainActionButtonClicked() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;

        // Check if the claim is free
        Claim.GenericMetadata metadata = actualClaim.getValue();
        if (metadata instanceof Claim.StreamMetadata) {
            View root = getView();
            if (root != null) {
                root.findViewById(R.id.file_view_main_action_button).setVisibility(View.INVISIBLE);
                root.findViewById(R.id.file_view_main_action_loading).setVisibility(View.VISIBLE);
            }
            if (actualClaim.getFile() == null && !actualClaim.isFree()) {
                checkAndConfirmPurchaseUrl();
            } else {
                handleMainActionForClaim();
            }
        } else {
            showError(getString(R.string.cannot_view_claim));
        }
    }

    private void checkAndConfirmPurchaseUrl() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            PurchaseListTask task = new PurchaseListTask(actualClaim.getClaimId(), null, new ClaimSearchResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                    boolean purchased = false;
                    if (claims.size() == 1) {
                        Claim purchasedClaim = claims.get(0);
                        if (actualClaim.getClaimId().equalsIgnoreCase(purchasedClaim.getClaimId())) {
                            // already purchased
                            purchased = true;
                        }
                    }

                    if (purchased) {
                        handleMainActionForClaim();
                    } else {
                        restoreMainActionButton();
                        confirmPurchaseUrl();
                    }
                }

                @Override
                public void onError(Exception error) {
                    // pass
                    restoreMainActionButton();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void confirmPurchaseUrl() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            Fee fee = ((Claim.StreamMetadata) actualClaim.getValue()).getFee();
            double cost = actualClaim.getActualCost(Lbryio.LBCUSDRate).doubleValue();
            String formattedCost = Helper.LBC_CURRENCY_FORMAT.format(cost);
            Context context = getContext();
            if (context != null) {
                try {
                    String message = getResources().getQuantityString(
                            R.plurals.confirm_purchase_message,
                            cost == 1 ? 1 : 2,
                            actualClaim.getTitle(),
                            formattedCost.equals("0") ? Helper.FULL_LBC_CURRENCY_FORMAT.format(cost) : formattedCost);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context).
                            setTitle(R.string.confirm_purchase).
                            setMessage(message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("uri", currentUrl);
                                    bundle.putString("paid", "true");
                                    bundle.putDouble("amount", Helper.parseDouble(fee.getAmount(), 0));
                                    bundle.putDouble("lbc_amount", cost);
                                    bundle.putString("currency", fee.getCurrency());
                                    LbryAnalytics.logEvent(LbryAnalytics.EVENT_PURCHASE_URI, bundle);

                                    View root = getView();
                                    if (root != null) {
                                        root.findViewById(R.id.file_view_main_action_button).setVisibility(View.INVISIBLE);
                                        root.findViewById(R.id.file_view_main_action_loading).setVisibility(View.VISIBLE);
                                    }
                                    handleMainActionForClaim();
                                }
                            }).setNegativeButton(R.string.no, null);
                    builder.show();
                } catch (IllegalStateException ex) {
                    // pass
                }
            }
        }
    }

    private void tryOpenFileOrFileGet() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            String claimId = actualClaim.getClaimId();
            FileListTask task = new FileListTask(claimId, null, new FileListTask.FileListResultHandler() {
                @Override
                public void onSuccess(List<LbryFile> files, boolean hasReachedEnd) {
                    if (files.size() > 0) {
                        actualClaim.setFile(files.get(0));
                        handleMainActionForClaim();
                        checkIsFileComplete();
                    } else {
                        checkStoragePermissionAndFileGet();
                    }
                }

                @Override
                public void onError(Exception error) {
                    checkStoragePermissionAndFileGet();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void resolvePlaylistClaimsAndPlayFirst() {
        if (playlistResolved) {
            return;
        }

        Helper.setViewVisibility(layoutLoadingState, View.VISIBLE);
        Helper.setViewVisibility(layoutResolving, View.VISIBLE);
        Helper.setViewVisibility(layoutNothingAtLocation, View.GONE);
        Helper.setViewVisibility(layoutDisplayArea, View.GONE);

        Map<String, Object> options = new HashMap<>();
        options.put("claim_type", "stream");
        options.put("page", 1);
        options.put("page_size", 999);
        options.put("claim_ids", fileClaim.getClaimIds());
        ClaimSearchTask task = new ClaimSearchTask(options, Lbry.API_CONNECTION_STRING, null, new ClaimSearchResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                playlistResolved = true;

                // reorder the claims based on the order in the list, TODO: find a more efficient way to do this
                Map<String, Claim> playlistClaimMap = new LinkedHashMap<>();
                List<String> claimIds = fileClaim.getClaimIds();
                for (String id : claimIds) {
                    for (Claim claim : claims) {
                        if (id.equalsIgnoreCase(claim.getClaimId())) {
                            playlistClaimMap.put(id, claim);
                            break;
                        }
                    }
                }

                playlistClaims = new ArrayList<>(playlistClaimMap.values());
                if (playlistClaims.size() > 0) {
                    playClaimFromCollection(playlistClaims.get(0), 0);
                }

                relatedContentAdapter = new ClaimListAdapter(playlistClaims, getContext());
                relatedContentAdapter.setListener(FileViewFragment.this);

                View root = getView();
                if (root != null) {
                    RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
                    relatedContentList.setAdapter(relatedContentAdapter);
                }
            }

            @Override
            public void onError(Exception error) {
                showError(getString(R.string.comment_error));
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleMainActionForClaim() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim.isPlayable()) {
            startTimeMillis = System.currentTimeMillis();
            showExoplayerView();
            playMedia();
        }
    }

    private void fileGet(boolean save) {
        if (getFileTask != null && getFileTask.getStatus() != AsyncTask.Status.FINISHED) {
            return;
        }

        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        getFileTask = new GetFileTask(actualClaim.getPermanentUrl(), save, null, new GetFileTask.GetFileHandler() {
            @Override
            public void beforeStart() {

            }

            @Override
            public void onSuccess(LbryFile file, boolean saveFile) {
                // queue the download
                if (actualClaim != null) {
                    if (actualClaim.isFree()) {
                        // paid is handled differently
                        Bundle bundle = new Bundle();
                        bundle.putString("uri", currentUrl);
                        bundle.putString("paid", "false");
                        LbryAnalytics.logEvent(LbryAnalytics.EVENT_PURCHASE_URI, bundle);
                    }

                    if (!actualClaim.isPlayable()) {
                        logFileView(actualClaim.getPermanentUrl(), 0);
                    }

                    actualClaim.setFile(file);
                    playOrViewMedia();
                }
            }

            @Override
            public void onError(Exception error, boolean saveFile) {
                try {
                    showError(getString(R.string.unable_to_view_url, currentUrl));
                    if (saveFile) {
                        onDownloadAborted();
                    }
                    restoreMainActionButton();
                } catch (IllegalStateException ex) {
                    // pass
                }
            }
        });
        getFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void playOrViewMedia() {
        boolean handled = false;
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        String mediaType = actualClaim.getMediaType();
        if (!Helper.isNullOrEmpty(mediaType)) {
            if (actualClaim.isPlayable()) {
                startTimeMillis = System.currentTimeMillis();
                showExoplayerView();
                playMedia();
                handled = true;
            } else if (actualClaim.isViewable()) {
                // check type and display
                boolean fileExists = false;
                LbryFile claimFile = actualClaim.getFile();
                Uri fileUri  = null;
                if (claimFile != null && !Helper.isNullOrEmpty(claimFile.getDownloadPath())) {
                    File file = new File(claimFile.getDownloadPath());
                    fileUri = Uri.fromFile(file);
                    fileExists = file.exists();
                }
                if (!fileExists) {
                    showError(getString(R.string.claim_file_not_found, claimFile != null ? claimFile.getDownloadPath() : ""));
                } else if (fileUri != null) {
                    View root = getView();
                    Context context = getContext();
                    if (root != null) {
                        if (mediaType.startsWith("image")) {
                            // display the image
                            View container = root.findViewById(R.id.file_view_imageviewer_container);
                            PhotoView photoView = root.findViewById(R.id.file_view_imageviewer);

                            if (context != null) {
                                Glide.with(context.getApplicationContext()).load(fileUri).centerInside().into(photoView);
                            }
                            container.setVisibility(View.VISIBLE);
                        } else if (mediaType.startsWith("text")) {
                            // show web view (and parse markdown too)
                            View container = root.findViewById(R.id.file_view_webview_container);
                            initWebView(root);
                            applyThemeToWebView();

                            if (Arrays.asList("text/markdown", "text/md").contains(mediaType.toLowerCase())) {
                                loadMarkdownFromFile(claimFile.getDownloadPath());
                            } else {
                                webView.loadUrl(fileUri.toString());
                            }
                            container.setVisibility(View.VISIBLE);
                        }
                    }
                    handled = true;
                }
            } else {
                openClaimExternally(actualClaim, mediaType);
            }
        }

        if (!handled) {
            showUnsupportedView();
        }
    }

    private long loadLastPlaybackPosition() {
        long position = -1;
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            String key = String.format("PlayPos_%s", !Helper.isNullOrEmpty(actualClaim.getShortUrl()) ? actualClaim.getShortUrl() : actualClaim.getPermanentUrl());
            Context context = getContext();
            if (context != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                position = sp.getLong(key, -1);
            }
        }
        return position;
    }

    private void savePlaybackPosition() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (MainActivity.appPlayer != null && actualClaim != null) {
            String key = String.format("PlayPos_%s", !Helper.isNullOrEmpty(actualClaim.getShortUrl()) ? actualClaim.getShortUrl() : actualClaim.getPermanentUrl());
            long position = MainActivity.appPlayer.getCurrentPosition();
            Context context = getContext();
            if (context != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                sp.edit().putLong(key, position).apply();
            }
        }
    }

    private void loadMarkdownFromFile(String filePath) {
        ReadTextFileTask task = new ReadTextFileTask(filePath, new ReadTextFileTask.ReadTextFileHandler() {
            @Override
            public void onSuccess(String text) {
                if (webView != null) {
                    String html = buildMarkdownHtml(text);
                    webView.loadData(Base64.encodeToString(html.getBytes(), Base64.NO_PADDING), "text/html", "base64");
                }
            }

            @Override
            public void onError(Exception error) {
                showError(error.getMessage());
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String buildMarkdownHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .attributeProviderFactory(new AttributeProviderFactory() {
                    @Override
                    public AttributeProvider create(AttributeProviderContext context) {
                        return new CodeAttributeProvider();
                    }
                })
                .build();
        String markdownHtml = renderer.render(document);

        return "<!doctype html>\n" +
                "        <html>\n" +
                "          <head>\n" +
                "            <meta charset=\"utf-8\"/>\n" +
                "            <meta name=\"viewport\" content=\"width=device-width, user-scalable=no\"/>\n" +
                "            <style type=\"text/css\">\n" +
                "              body { font-family: 'Inter', sans-serif; margin: 16px }\n" +
                "              img { width: 100%; }\n" +
                "              pre { white-space: pre-wrap; word-wrap: break-word }\n" +
                "            </style>\n" +
                "          </head>\n" +
                "          <body>\n" +
                "            <div id=\"content\">\n" +
                markdownHtml +
                "            </div>\n" +
                "          </body>\n" +
                "        </html>";
    }

    private void openClaimExternally(Claim claim, String mediaType) {
        Uri fileUri = Uri.parse(claim.getFile().getDownloadPath());

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mediaType.toLowerCase());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(intent, getString(R.string.choose_app));
        startActivityForResult(chooser, 419);
    }

    private void playClaimFromCollection(Claim theClaim, int index) {
        updatePlaylistContentDisplay(index);
        collectionClaimItem = theClaim;
        renderClaim();
        checkAndLoadComments(true);
    }

    public void showError(String message) {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
        }
    }

    private void loadRelatedContent() {
        // reset the list view
        View root = getView();
        if (fileClaim != null && root != null) {
            Context context = getContext();

            List<Claim> loadingPlaceholders = new ArrayList<>();
            int loadingPlaceholdersLength = Claim.TYPE_COLLECTION.equalsIgnoreCase(fileClaim.getValueType()) ? fileClaim.getClaimIds().size() : 15;
            for (int i = 0; i < loadingPlaceholdersLength; i++) {
                Claim placeholder = new Claim();
                placeholder.setLoadingPlaceholder(true);
                loadingPlaceholders.add(placeholder);
            }
            relatedContentAdapter = new ClaimListAdapter(loadingPlaceholders, context);
            relatedContentAdapter.setContextGroupId(FILE_CONTEXT_GROUP_ID);

            RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
            relatedContentList.setAdapter(relatedContentAdapter);

            ProgressBar relatedLoading = root.findViewById(R.id.file_view_related_content_progress);
            boolean canShowMatureContent = false;
            if (context != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                canShowMatureContent = sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_MATURE_CONTENT, false);
            }

            if (!Claim.TYPE_COLLECTION.equalsIgnoreCase(fileClaim.getValueType())) {
                String title = fileClaim.getTitle();
                String claimId = fileClaim.getClaimId();

                final boolean nsfw = canShowMatureContent;
                relatedLoading.setVisibility(View.VISIBLE);

                // Making a request which explicitly uses a certain value form the amount of results needed
                // and no processing any possible exception, so using a callable instead of an AsyncTask
                // makes sense for all Android API Levels
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        LighthouseSearch callable = new LighthouseSearch(title, RELATED_CONTENT_SIZE, 0, nsfw, claimId);
                        Future<List<Claim>> future = executor.submit(callable);

                        try {
                            List<Claim> result = future.get();

                            if (executor != null && !executor.isShutdown()) {
                                executor.shutdown();
                            }
                            MainActivity a = (MainActivity) getActivity();

                            if (a != null) {
                                a.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        relatedContentRequestSuccedded(result);
                                        relatedLoading.setVisibility(View.GONE);
                                    }
                                });
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            if (executor != null && !executor.isShutdown()) {
                                executor.shutdown();
                            }

                            e.printStackTrace();
                        }
                    }
                });
                t.start();
            } else {
                TextView relatedOrPlayList = root.findViewById(R.id.related_or_playlist);
                relatedOrPlayList.setText(fileClaim.getTitle());
                relatedOrPlayList.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_cast_connected, 0, 0, 0);
                relatedOrPlayList.setPadding(0, 0, 0, 16);
                relatedOrPlayList.setTypeface(null, Typeface.BOLD);

                Map<String, Object> claimSearchOptions = new HashMap<>(3);

                claimSearchOptions.put("claim_ids", fileClaim.getClaimIds());
                claimSearchOptions.put("not_tags", canShowMatureContent ? null : new ArrayList<>(Predefined.MATURE_TAGS));
                claimSearchOptions.put("page_size", fileClaim.getClaimIds().size());

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<List<Claim>> future = executor.submit(new Search(claimSearchOptions));

                try {
                    List<Claim> playlistClaimItems = future.get();

                    if (playlistClaimItems != null) {
                        relatedContentAdapter.setItems(playlistClaimItems);
                        relatedContentAdapter.setListener(FileViewFragment.this);

                        View v = getView();
                        if (v != null) {
                            relatedContentList.setAdapter(relatedContentAdapter);
                            relatedContentAdapter.notifyDataSetChanged();

                            Helper.setViewVisibility(
                                    v.findViewById(R.id.file_view_no_related_content),
                                    relatedContentAdapter == null || relatedContentAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                        }

                        scrollToCommentHash();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClaimClicked(Claim claimItem, int position) {
        if (claimItem.isLoadingPlaceholder()) {
            return;
        }

        if ((fileClaim != null && Claim.TYPE_COLLECTION.equalsIgnoreCase(fileClaim.getValueType())) ||
                (collectionClaimItem != null && playlistClaims.size() > 0)) {
            playClaimFromCollection(claimItem, position);
            return;
        }

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.openFileUrl(claimItem.getPermanentUrl()); //openClaimUrl(claim.getPermanentUrl());
        }
    }

    private void relatedContentRequestSuccedded(List<Claim> claims) {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        List<Claim> filteredClaims = new ArrayList<>();
        for (Claim c : claims) {
            if (!c.getClaimId().equalsIgnoreCase(actualClaim.getClaimId())) {
                filteredClaims.add(c);
            }
        }

        filteredClaims = Helper.filterClaimsByBlockedChannels(filteredClaims, Lbryio.blockedChannels);

        Context ctx = getContext();
        if (ctx != null) {
            relatedContentAdapter.setItems(filteredClaims);
            relatedContentAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    if (claim.isLoadingPlaceholder()) {
                        return;
                    }

                    if (ctx instanceof MainActivity) {
                        MainActivity activity = (MainActivity) ctx;
                        if (claim.getName().startsWith("@")) {
                            activity.openChannelClaim(claim);
                        } else {
                            Map<String, Object> params = new HashMap<>(1);
                            params.put("url", !Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl());

                            setParams(params);
                            checkParams();
                        }
                    }
                }
            });

            View v = getView();
            if (v != null) {
                View root = getView();
                RecyclerView relatedContentList = root.findViewById(R.id.file_view_related_content_list);
                relatedContentList.setAdapter(relatedContentAdapter);
                relatedContentAdapter.notifyDataSetChanged();

                Helper.setViewVisibility(
                        v.findViewById(R.id.file_view_no_related_content),
                        relatedContentAdapter == null || relatedContentAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            // if related content loads before comment, this will affect the scroll position
            // so just ensure that we are at the correct position
            scrollToCommentHash();
        }
    }
    private void loadComments() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        View root = getView();
        if (root != null && actualClaim != null) {
            ProgressBar commentsLoading = root.findViewById(R.id.file_view_comments_progress);
            CommentListTask task = new CommentListTask(1, 200, actualClaim.getClaimId(), commentsLoading, new CommentListHandler() {
                @Override
                public void onSuccess(List<Comment> comments, boolean hasReachedEnd) {
                    if (!comments.isEmpty()) {
                        // Load and process comments reactions on a different thread so main thread is not blocked
                        Helper.setViewVisibility(commentsLoading, View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Map<String, Reactions> commentReactions = loadReactions(comments);
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Helper.setViewVisibility(commentsLoading, View.GONE);
                                            processCommentReactions(comments, commentReactions);
                                        }
                                    });
                                }
                            }
                        }).start();
                    }
                }

                @Override
                public void onError(Exception error) {
                    if (error != null) {
                        error.printStackTrace();
                    }
                    checkNoComments();
                }

                private void processCommentReactions(List<Comment> comments, Map<String, Reactions> commentReactions) {
                    for (Comment c: comments) {
                        if (commentReactions != null) {
                            c.setReactions(commentReactions.get(c.getId()));
                        } else {
                            c.setReactions(new Reactions(0, 0, false, false));
                        }
                    }

                    List<Comment> rootComments = new ArrayList<>();

                    for (Comment c : comments) {
                        if (c.getParentId() == null) {
                            rootComments.add(c);
                        }
                    }

                    // Now we have level 0 comments to content

                    if (commentReactions != null) {
                        Collections.sort(rootComments, new Comparator<Comment>() {
                            @Override
                            public int compare(Comment o1, Comment o2) {
                                int o1SelfLiked = (Lbryio.isSignedIn() &&  o1.getReactions() != null && o1.getReactions().isLiked()) ? 1 : 0;
                                int o2SelfLiked = (Lbryio.isSignedIn() && o2.getReactions() != null && o2.getReactions().isLiked()) ? 1 : 0;
                                return (o2.getReactions().getOthersLikes() + o2SelfLiked) - (o1.getReactions().getOthersLikes() + o1SelfLiked);
                            }
                        });
                    }

                    // Direct comments are now sorted by their amount of likes. We can now pick the
                    // one to be displayed as the collapsed single comment.
                    Comment singleComment = rootComments.get(0);

                    TextView commentText = singleCommentRoot.findViewById(R.id.comment_text);
                    ImageView thumbnailView = singleCommentRoot.findViewById(R.id.comment_thumbnail);
                    View noThumbnailView = singleCommentRoot.findViewById(R.id.comment_no_thumbnail);
                    TextView alphaView = singleCommentRoot.findViewById(R.id.comment_thumbnail_alpha);

                    commentText.setText(singleComment.getText());
                    commentText.setMaxLines(3);
                    commentText.setEllipsize(TextUtils.TruncateAt.END);
                    commentText.setClickable(true);
                    commentText.setTextIsSelectable(false);

                    boolean hasThumbnail = singleComment.getPoster() != null && !Helper.isNullOrEmpty(singleComment.getPoster().getThumbnailUrl());
                    thumbnailView.setVisibility(hasThumbnail ? View.VISIBLE : View.INVISIBLE);
                    noThumbnailView.setVisibility(!hasThumbnail ? View.VISIBLE : View.INVISIBLE);

                    int bgColor = Helper.generateRandomColorForValue(singleComment.getChannelId());
                    Helper.setIconViewBackgroundColor(noThumbnailView, bgColor, false, getContext());
                    if (hasThumbnail) {
                        Context ctx = getContext();
                        if (ctx != null) {
                            Context appCtx = ctx.getApplicationContext();
                            Glide.with(appCtx).asBitmap().load(singleComment.getPoster().getThumbnailUrl()).
                                    apply(RequestOptions.circleCropTransform()).into(thumbnailView);
                        }
                    }
                    alphaView.setText(singleComment.getChannelName() != null ? singleComment.getChannelName().substring(1, 2).toUpperCase() : null);
                    singleCommentRoot.findViewById(R.id.comment_actions_area).setVisibility(View.GONE);
                    singleCommentRoot.findViewById(R.id.comment_time).setVisibility(View.GONE);
                    singleCommentRoot.findViewById(R.id.comment_channel_name).setVisibility(View.GONE);
                    singleCommentRoot.findViewById(R.id.comment_more_options).setVisibility(View.GONE);

                    singleCommentRoot.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            expandButton.performClick();
                        }
                    });

                    List<Comment> parentComments = rootComments;
//                    rootComments.clear();
                    for (Comment c : comments) {
                        if (!parentComments.contains(c)) {
                            if (c.getParentId() != null) {
                                Comment item = parentComments.stream().filter(v -> c.getParentId().equalsIgnoreCase(v.getId())).findFirst().orElse(null);
                                if (item != null) {
                                    parentComments.add(parentComments.indexOf(item) + 1, c);
                                }
                            }
                        }
                    }
                    comments = parentComments;

                    Context ctx = getContext();
                    View root = getView();
                    if (ctx != null && root != null) {
                        ensureCommentListAdapterCreated(comments);
                    }
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void ensureCommentListAdapterCreated(final List<Comment> comments) {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if ( commentListAdapter == null ) {

            final Context androidContext = getContext();
            final View root = getView();

            commentListAdapter = new CommentListAdapter(comments, getContext(), actualClaim, new CommentListAdapter.CommentListListener() {
                @Override
                public void onListChanged() {
                    checkNoComments();
                }

                @Override
                public void onCommentReactClicked(Comment c, boolean liked) {
                    react(c, liked);
                }

                @Override
                public void onReplyClicked(Comment comment) {
                    setReplyToComment(comment);
                }
            });
            commentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    if (!Helper.isNullOrEmpty(claim.getName()) && claim.getName().startsWith("@") &&
                            androidContext instanceof MainActivity) {
                        removeNotificationAsSource();
                        ((MainActivity) androidContext).openChannelClaim(claim);
                    }
                }
            });

            RecyclerView commentsList = root.findViewById(R.id.file_view_comments_list);
            // Indent reply-type items
            int marginInPx = Math.round(40 * ((float) androidContext.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
            commentsList.addItemDecoration(new CommentItemDecoration(marginInPx));
            commentsList.setAdapter(commentListAdapter);
            commentListAdapter.notifyItemRangeInserted(0, comments.size());

            scrollToCommentHash();
            checkNoComments();
            resolveCommentPosters();
        }
    }

    private void scrollToCommentHash() {
        View root = getView();
        // check for the position of commentHash if set
        if (root != null && !Helper.isNullOrEmpty(commentHash) && commentListAdapter != null && commentListAdapter.getItemCount() > 0) {
            RecyclerView commentList = root.findViewById(R.id.file_view_comments_list);
            int position = commentListAdapter.getPositionForComment(commentHash);
            if (position > -1 && commentList.getLayoutManager() != null) {
                NestedScrollView scrollView = root.findViewById(R.id.file_view_scroll_view);
                scrollView.requestChildFocus(commentList, commentList);
                commentList.getLayoutManager().scrollToPosition(position);
            }
        }
    }

    private void checkNoComments() {
        View root = getView();
        if (root != null) {
            Helper.setViewVisibility(root.findViewById(R.id.file_view_no_comments),
                    commentListAdapter == null || commentListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            Helper.setViewVisibility(root.findViewById(R.id.expand_commentarea_button),
                    commentListAdapter == null || commentListAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            Helper.setViewVisibility(root.findViewById(R.id.collapsed_comment),
                    commentListAdapter == null || commentListAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);

            if (commentListAdapter == null)
                root.findViewById(R.id.container_comment_form).setVisibility(View.VISIBLE);
        }
    }

    private void resolveCommentPosters() {
        if (commentListAdapter != null) {
            long st = System.currentTimeMillis();
            List<String> urlsToResolve = new ArrayList<>(commentListAdapter.getClaimUrlsToResolve());
            if (urlsToResolve.size() > 0) {
                ResolveTask task = new ResolveTask(urlsToResolve, Lbry.API_CONNECTION_STRING, null, new ClaimListResultHandler() {
                    @Override
                    public void onSuccess(List<Claim> claims) {
                        if (commentListAdapter != null) {
                            for (Claim claim : claims) {
                                if (claim.getClaimId() != null) {
                                    commentListAdapter.updatePosterForComment(claim.getClaimId(), claim);
                                }
                            }

                            // filter for blocked comments
                            commentListAdapter.filterBlockedChannels(Lbryio.blockedChannels);

                            commentListAdapter.notifyDataSetChanged();
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
    }

    public boolean onBackPressed() {
        if (isInFullscreenMode()) {
            disableFullScreenMode();
            return true;
        }

        if (isImageViewerVisible()) {
            View root = getView();
            if (root != null) {
                root.findViewById(R.id.file_view_imageviewer_container).setVisibility(View.GONE);
            }
            restoreMainActionButton();
            return true;
        }
        if (isWebViewVisible()) {
            View root = getView();
            if (root != null) {
                root.findViewById(R.id.file_view_webview_container).setVisibility(View.GONE);
                ((RelativeLayout) root.findViewById(R.id.file_view_webview_container)).removeAllViews();
            }
            closeWebView();
            restoreMainActionButton();
            return true;
        }

        return false;
    }

    private boolean isImageViewerVisible() {
        View view = getView();
        return view != null && view.findViewById(R.id.file_view_imageviewer_container).getVisibility() == View.VISIBLE;
    }

    private boolean isWebViewVisible() {
        View view = getView();
        return view != null && view.findViewById(R.id.file_view_webview_container).getVisibility() == View.VISIBLE;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void enableFullScreenMode() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            View root = getView();
            if (root != null) {
                ConstraintLayout globalLayout = root.findViewById(R.id.file_view_global_layout);
                View exoplayerContainer = root.findViewById(R.id.file_view_exoplayer_container);
                ((ViewGroup) exoplayerContainer.getParent()).removeView(exoplayerContainer);
                globalLayout.addView(exoplayerContainer);

                View playerView = root.findViewById(R.id.file_view_exoplayer_view);
                ((ImageView) playerView.findViewById(R.id.player_image_full_screen_toggle)).setImageResource(R.drawable.ic_fullscreen_exit);

                MainActivity activity = (MainActivity) context;
                activity.enterFullScreenMode();

                exoplayerContainer.setPadding(0, 0, 0, 0);

                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void disableFullScreenMode() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            View root = getView();
            if (root != null) {
                RelativeLayout mediaContainer = root.findViewById(R.id.file_view_media_container);
                View exoplayerContainer = root.findViewById(R.id.file_view_exoplayer_container);
                ((ViewGroup) exoplayerContainer.getParent()).removeView(exoplayerContainer);
                mediaContainer.addView(exoplayerContainer);

                View playerView = root.findViewById(R.id.file_view_exoplayer_view);
                ((ImageView) playerView.findViewById(R.id.player_image_full_screen_toggle)).setImageResource(R.drawable.ic_fullscreen);
                exoplayerContainer.setPadding(0, 0, 0, 0);

                activity.exitFullScreenMode();
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }

    private boolean isInFullscreenMode() {
        View view = getView();
        if (view != null) {
            View exoplayerContainer = view.findViewById(R.id.file_view_exoplayer_container);
            return exoplayerContainer.getParent() instanceof ConstraintLayout;
        }
        return false;
    }

    private void scheduleElapsedPlayback() {
        if (!elapsedPlaybackScheduled) {
            elapsedPlaybackScheduler = Executors.newSingleThreadScheduledExecutor();
            elapsedPlaybackScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (MainActivity.appPlayer != null) {
                                    elapsedDuration = MainActivity.appPlayer.getCurrentPosition();
                                    int elapsedSeconds = Double.valueOf(elapsedDuration / 1000.0).intValue();
                                    if (elapsedDuration > 0 && elapsedSeconds % 5 == 0 && elapsedSeconds != lastPositionSaved) {
                                        // save playback position every 5 seconds
                                        savePlaybackPosition();
                                        lastPositionSaved = elapsedSeconds;
                                    }

                                    renderElapsedDuration();
                                }
                            }
                        });
                    }
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
            elapsedPlaybackScheduled = true;
        }
    }

    private void resetPlayer() {
        elapsedDuration = 0;
        totalDuration = 0;
        renderElapsedDuration();
        renderTotalDuration();

        elapsedPlaybackScheduled = false;
        if (elapsedPlaybackScheduler != null) {
            elapsedPlaybackScheduler.shutdownNow();
            elapsedPlaybackScheduler = null;
        }

        if (seekOverlayHandler != null) {
            seekOverlayHandler.removeCallbacksAndMessages(null);
            seekOverlayHandler = null;
        }

        playbackStarted = false;
        startTimeMillis = 0;
        isPlaying = false;

        if (MainActivity.appPlayer != null) {
            MainActivity.appPlayer.stop(true);
            MainActivity.appPlayer.removeListener(fileViewPlayerListener);
            PlaybackParameters params = new PlaybackParameters(1.0f);
            MainActivity.appPlayer.setPlaybackParameters(params);
            MainActivity.videoIsTranscoded = false;
        }
    }

    private void showBuffering() {
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.player_buffering_progress).setVisibility(View.VISIBLE);

            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            playerView.findViewById(R.id.player_skip_back_10).setVisibility(View.INVISIBLE);
            playerView.findViewById(R.id.player_skip_forward_10).setVisibility(View.INVISIBLE);
        }
    }

    private void hideBuffering() {
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.player_buffering_progress).setVisibility(View.INVISIBLE);

            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            playerView.findViewById(R.id.player_skip_back_10).setVisibility(View.VISIBLE);
            playerView.findViewById(R.id.player_skip_forward_10).setVisibility(View.VISIBLE);
        }
    }

    private void renderElapsedDuration() {
        View view = getView();
        if (view != null) {
            Helper.setViewText(view.findViewById(R.id.player_duration_elapsed), Helper.formatDuration(Double.valueOf(elapsedDuration / 1000.0).longValue()));
        }
    }

    private void renderTotalDuration() {
        View view = getView();
        if (view != null) {
            Helper.setViewText(view.findViewById(R.id.player_duration_total), Helper.formatDuration(Double.valueOf(totalDuration / 1000.0).longValue()));
        }
    }

    private void loadAndScheduleDurations() {
        if (MainActivity.appPlayer != null && playbackStarted) {
            elapsedDuration = MainActivity.appPlayer.getCurrentPosition() < 0 ? 0 : MainActivity.appPlayer.getCurrentPosition();
            totalDuration = MainActivity.appPlayer.getDuration() < 0 ? 0 : MainActivity.appPlayer.getDuration();

            renderElapsedDuration();
            renderTotalDuration();
            scheduleElapsedPlayback();
        }
    }

    private void logPlay(String url, long startTimeMillis) {
        long timeToStartMillis = startTimeMillis > 0 ? System.currentTimeMillis() - startTimeMillis : 0;

        Bundle bundle = new Bundle();
        bundle.putString("uri", url);
        bundle.putLong("time_to_start_ms", timeToStartMillis);
        bundle.putLong("time_to_start_seconds", Double.valueOf(timeToStartMillis / 1000.0).longValue());
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_PLAY, bundle);

        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        logFileView(actualClaim.getPermanentUrl(), timeToStartMillis);
    }

    private void logFileView(String url, long timeToStart) {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            String authToken = Lbryio.AUTH_TOKEN;
            Map<String, String> options = new HashMap<>();
            options.put("uri", url);
            options.put("claim_id", actualClaim.getClaimId());
            options.put("outpoint", String.format("%s:%d", actualClaim.getTxid(), actualClaim.getNout()));
            if (timeToStart > 0) {
                options.put("time_to_start", String.valueOf(timeToStart));
            }
            if (!Helper.isNullOrEmpty(authToken)) {
                options.put("auth_token", authToken);
            }

            Activity activity = getActivity();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                Supplier<Boolean> s = new Supplier<Boolean>() {
                    @Override
                    public Boolean get() {
                        try {
                            Lbryio.call("file", "view", options,  null).close();
                            return true;
                        } catch (LbryioRequestException | LbryioResponseException ex) {
                            return false;
                        }
                    }
                };

                CompletableFuture<Boolean> cf = CompletableFuture.supplyAsync(s);
                cf.whenComplete((result, ex) -> {
                    if (result) {
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    claimEligibleRewards();
                                }
                            });
                        }
                        if (ex != null) {
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) activity).showError(ex.getMessage());
                                    }
                                });
                            }
                        }
                    }
                });
            } else {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Lbryio.call("file", "view", options,  null).close();
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        claimEligibleRewards();
                                    }
                                });
                            }
                        } catch (LbryioRequestException | LbryioResponseException ex) {
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) activity).showError(ex.getMessage());
                                    }
                                });
                            }
                        }
                    }
                });
                t.start();
            }
        }
    }

    private void checkIsFollowing() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null && actualClaim.getSigningChannel() != null) {
            boolean isFollowing = Lbryio.isFollowing(actualClaim.getSigningChannel());
            boolean notificationsDisabled = Lbryio.isNotificationsDisabled(actualClaim.getSigningChannel());
            Context context = getContext();
            View root = getView();
            if (context != null && root != null) {
                View iconFollow = root.findViewById(R.id.file_view_icon_follow);
                SolidIconView iconUnfollow = root.findViewById(R.id.file_view_icon_unfollow);
                SolidIconView iconBell = root.findViewById(R.id.file_view_icon_bell);
                Helper.setViewVisibility(iconFollow, !isFollowing ? View.VISIBLE: View.GONE);
                Helper.setViewVisibility(iconUnfollow, isFollowing ? View.VISIBLE : View.GONE);
                Helper.setViewVisibility(iconBell, isFollowing ? View.VISIBLE : View.GONE);

                iconBell.setText(notificationsDisabled ? R.string.fa_bell : R.string.fa_bell_slash);
            }
        }
    }

    private void claimEligibleRewards() {
        // attempt to claim eligible rewards after viewing or playing a file (fail silently)
        final String authToken = Lbryio.AUTH_TOKEN;
        ClaimRewardTask firstStreamTask = new ClaimRewardTask(Reward.TYPE_FIRST_STREAM, null, authToken, eligibleRewardHandler);
        ClaimRewardTask dailyViewTask = new ClaimRewardTask(Reward.TYPE_DAILY_VIEW, null, authToken, eligibleRewardHandler);
        firstStreamTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        dailyViewTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final ClaimRewardTask.ClaimRewardHandler eligibleRewardHandler = new ClaimRewardTask.ClaimRewardHandler() {
        @Override
        public void onSuccess(double amountClaimed, String message) {
            if (Helper.isNullOrEmpty(message)) {
                message = getResources().getQuantityString(
                        R.plurals.claim_reward_message,
                        amountClaimed == 1 ? 1 : 2,
                        new DecimalFormat(Helper.LBC_CURRENCY_FORMAT_PATTERN).format(amountClaimed));
            }
            View root = getView();

            if (root != null) {
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
            }
        }

        @Override
        public void onError(Exception error) {
            // pass
        }
    };

    private void checkIsFileComplete() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim == null) {
            return;
        }
        View root = getView();
        if (root != null) {
            if (actualClaim.getFile() != null && actualClaim.getFile().isCompleted()) {
                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_delete), View.VISIBLE);
                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_download), View.GONE);
            } else {
                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_delete), View.GONE);
//                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_download), View.VISIBLE);
            }

        }
    }

    private void toggleCast() {
        if (!MainActivity.castPlayer.isCastSessionAvailable()) {
            showError(getString(R.string.no_cast_session_available));
            return;
        }

        if (currentPlayer == MainActivity.appPlayer) {
            setCurrentPlayer(MainActivity.castPlayer);
        } else {
            setCurrentPlayer(MainActivity.appPlayer);
        }
    }

    private void onDownloadAborted() {
        downloadInProgress = false;

        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (actualClaim != null) {
            actualClaim.setFile(null);
        }
        View root = getView();
        if (root != null) {
            ((ImageView) root.findViewById(R.id.file_view_action_download_icon)).setImageResource(R.drawable.ic_download);
            Helper.setViewVisibility(root.findViewById(R.id.file_view_download_progress), View.GONE);
            Helper.setViewVisibility(root.findViewById(R.id.file_view_unsupported_container), View.GONE);
        }

        checkIsFileComplete();
        restoreMainActionButton();
    }

    private void restoreMainActionButton() {
        View root = getView();
        if (root != null) {
            root.findViewById(R.id.file_view_main_action_loading).setVisibility(View.INVISIBLE);
            root.findViewById(R.id.file_view_main_action_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onClaimsFetched(List<Claim> claims) {
        checkOwnClaim();
    }

    @Override
    public void onPortraitOrientationEntered() {
        // Skip this for now. User restores default view mode by pressing fullscreen toggle
        /*Context context = getContext();
        if (context instanceof MainActivity && ((MainActivity) context).isInFullscreenMode()) {
            disableFullScreenMode();
        }*/
    }

    @Override
    public void onLandscapeOrientationEntered() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (activity.isEnteringPIPMode() || activity.isInPictureInPictureMode()) {
                return;
            }
            Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
            if (actualClaim != null && actualClaim.isPlayable() && !activity.isInFullscreenMode()) {
                enableFullScreenMode();
            }
        }
    }

    private void checkRewardsDriver() {
        Context ctx = getContext();
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (ctx != null && actualClaim != null && !actualClaim.isFree() && actualClaim.getFile() == null) {
            String rewardsDriverText = getString(R.string.earn_some_credits_to_access);
            checkRewardsDriverCard(rewardsDriverText, actualClaim.getActualCost(Lbryio.LBCUSDRate).doubleValue());
        }
    }

    private static class LbryWebViewClient extends WebViewClient {
        private final Context context;
        public LbryWebViewClient(Context context) {
            this.context = context;
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri url = request.getUrl();
            if (context != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, url);
                context.startActivity(intent);
            }
            return true;
        }
    }

    private void onRelatedDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress) {
        if ("abort".equals(downloadAction)) {
            if (relatedContentAdapter != null) {
                relatedContentAdapter.clearFileForClaimOrUrl(outpoint, uri);
            }
            return;
        }

        try {
            JSONObject fileInfo = new JSONObject(fileInfoJson);
            LbryFile claimFile = LbryFile.fromJSONObject(fileInfo);
            String claimId = claimFile.getClaimId();
            if (relatedContentAdapter != null) {
                relatedContentAdapter.updateFileForClaimByIdOrUrl(claimFile, claimId, uri);
            }
        } catch (JSONException ex) {
            // invalid file info for download
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == FILE_CONTEXT_GROUP_ID && item.getItemId() == R.id.action_block) {
            if (relatedContentAdapter != null) {
                int position = relatedContentAdapter.getPosition();
                Claim claim = relatedContentAdapter.getItems().get(position);
                if (claim != null && claim.getSigningChannel() != null) {
                    Claim channel = claim.getSigningChannel();
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).handleBlockChannel(channel);
                    }
                }
            }
            return true;
        }

        if (item.getGroupId() == Helper.PLAYBACK_SPEEDS_GROUP_ID) {
            int speed = item.getItemId();
            if (MainActivity.appPlayer != null) {
                setPlaybackSpeed(MainActivity.appPlayer, speed);
                return true;
            }
        } else if (item.getGroupId() == Helper.QUALITIES_GROUP_ID) {
            loadingQualityChanged = true;
            int quality = item.getItemId();

            if (MainActivity.appPlayer != null) {
                setPlayerQuality(MainActivity.appPlayer, quality);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldHideGlobalPlayer() {
        return true;
    }

    private void checkOwnClaim() {
        Claim claimToCheck = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        if (claimToCheck != null) {
            boolean isOwnClaim = Lbry.ownClaims.contains(claimToCheck);
            View root = getView();
            if (root != null) {
                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_report), isOwnClaim ? View.GONE : View.VISIBLE);
                // TODO Re-enable this when implemented
//                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_edit), isOwnClaim ? View.VISIBLE : View.GONE);
//                Helper.setViewVisibility(root.findViewById(R.id.file_view_action_unpublish), isOwnClaim ? View.VISIBLE : View.GONE);


                LinearLayout fileViewActionsArea = root.findViewById(R.id.file_view_actions_area);
                fileViewActionsArea.setWeightSum(isOwnClaim ? 6 : 5);
            }
        }
    }

    public static class StreamLoadErrorPolicy extends DefaultLoadErrorHandlingPolicy {
        @Override
        public int getMinimumLoadableRetryCount(int dataType) {
            return Integer.MAX_VALUE;
        }
    }

    public void onEnterPIPMode() {
        View root = getView();
        if (root != null) {
            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            playerView.setVisibility(View.GONE);
        }
    }

    public void onExitPIPMode() {
        View root = getView();
        if (root != null) {
            PlayerView playerView = root.findViewById(R.id.file_view_exoplayer_view);
            playerView.setVisibility(View.VISIBLE);
        }
    }

    private void showStoragePermissionRefusedError() {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, R.string.storage_permission_rationale_download, Snackbar.LENGTH_LONG).
                    setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
        }
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        fetchingChannels = true;
        disableChannelSpinner();
        ClaimListTask task = new ClaimListTask(Claim.TYPE_CHANNEL, progressLoadingChannels, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                Lbry.ownChannels = new ArrayList<>(claims);
                updateChannelList(Lbry.ownChannels);
                enableChannelSpinner();
                fetchingChannels = false;
            }

            @Override
            public void onError(Exception error) {
                enableChannelSpinner();
                fetchingChannels = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void disableChannelSpinner() {
        Helper.setViewEnabled(commentChannelSpinner, false);
    }
    private void enableChannelSpinner() {
        Helper.setViewEnabled(commentChannelSpinner, true);
        if (commentChannelSpinner != null) {
            Claim selectedClaim = (Claim) commentChannelSpinner.getSelectedItem();
            if (selectedClaim != null) {
                if (selectedClaim.isPlaceholder()) {
                    showChannelCreator();
                }
            }
        }
    }
    private void showChannelCreator() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.showChannelCreator(this);
        }
    }

    @Override
    public void onChannelCreated(Claim claimResult) {
        // add the claim to the channel list and set it as the selected item
        if (commentChannelSpinnerAdapter != null) {
            commentChannelSpinnerAdapter.add(claimResult);
        } else {
            updateChannelList(Collections.singletonList(claimResult));
        }
        if (commentChannelSpinner != null && commentChannelSpinnerAdapter != null) {
            // Ensure adapter is set for the spinner
            if (commentChannelSpinner.getAdapter() == null) {
                commentChannelSpinner.setAdapter(commentChannelSpinnerAdapter);
            }
            commentChannelSpinner.setSelection(commentChannelSpinnerAdapter.getCount() - 1);
        }

        if (commentChannelSpinner != null) {
            View formRoot = (View) commentChannelSpinner.getParent().getParent();
            formRoot.setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.has_channels).setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.no_channels).setVisibility(View.GONE);
        }
    }

    private void updateChannelList(List<Claim> channels) {
        if (commentChannelSpinnerAdapter == null) {
            Context context = getContext();
            if (context != null) {
                commentChannelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, new ArrayList<>(channels));
                commentChannelSpinnerAdapter.addPlaceholder(false);
                commentChannelSpinnerAdapter.notifyDataSetChanged();
            }
        } else {
            commentChannelSpinnerAdapter.clear();
            commentChannelSpinnerAdapter.addAll(channels);
            commentChannelSpinnerAdapter.addPlaceholder(false);
            commentChannelSpinnerAdapter.notifyDataSetChanged();
        }

        if (commentChannelSpinner != null) {
            commentChannelSpinner.setAdapter(commentChannelSpinnerAdapter);
        }

        if (commentChannelSpinnerAdapter != null && commentChannelSpinner != null) {
            if (commentChannelSpinnerAdapter.getCount() > 1) {
                commentChannelSpinner.setSelection(1);
            }
        }
    }

    private void initCommentForm(View root) {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.refreshChannelCreationRequired(root);
        }

        textCommentLimit.setText(String.format("%d / %d", Helper.getValue(inputComment.getText()).length(), Comment.MAX_LENGTH));

        buttonClearReplyToComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearReplyToComment();
            }
        });

        buttonPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAndCheckPostComment();
            }
        });

        buttonCreateChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!fetchingChannels) {
                    showChannelCreator();
                }
            }
        });

        buttonCommentSignedInUserRequired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();

                if (activity != null) {
                    activity.simpleSignIn(0);
                }
            }
        });

        inputComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int len = charSequence.length();
                textCommentLimit.setText(String.format("%d / %d", len, Comment.MAX_LENGTH));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        commentChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Object item = adapterView.getItemAtPosition(position);
                if (item instanceof Claim) {
                    Claim claim = (Claim) item;
                    if (claim.isPlaceholder()) {
                        if (!fetchingChannels) {
                            showChannelCreator();
                            if (commentChannelSpinnerAdapter.getCount() > 1) {
                                commentChannelSpinner.setSelection(commentChannelSpinnerAdapter.getCount() - 1);
                            }
                        }
                    } else {
                        updatePostAsChannel(claim);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void validateAndCheckPostComment() {
        String comment = Helper.getValue(inputComment.getText());
        Claim channel = (Claim) commentChannelSpinner.getSelectedItem();

        if (Helper.isNullOrEmpty(comment)) {
            showError(getString(R.string.please_enter_comment));
            return;
        }
        if (channel == null || Helper.isNullOrEmpty(channel.getClaimId())) {
            showError(getString(R.string.please_select_channel));
            return;
        }

        postComment();
    }

    private void updatePostAsChannel(Claim channel) {
        boolean hasThumbnail = !Helper.isNullOrEmpty(channel.getThumbnailUrl());
        Helper.setViewVisibility(commentPostAsThumbnail, hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        Helper.setViewVisibility(commentPostAsNoThumbnail, !hasThumbnail ? View.VISIBLE : View.INVISIBLE);
        Helper.setViewText(commentPostAsAlpha, channel.getName() != null ? channel.getName().substring(1, 2).toUpperCase() : null);

        Context context = getContext();
        int bgColor = Helper.generateRandomColorForValue(channel.getClaimId());
        Helper.setIconViewBackgroundColor(commentPostAsNoThumbnail, bgColor, false, context);

        if (hasThumbnail && context != null) {
            Glide.with(context.getApplicationContext()).
                    asBitmap().
                    load(channel.getThumbnailUrl(commentPostAsThumbnail.getLayoutParams().width, commentPostAsThumbnail.getLayoutParams().height, 85)).
                    apply(RequestOptions.circleCropTransform()).
                    into(commentPostAsThumbnail);
        }
    }

    private void beforePostComment() {
        postingComment = true;
        Helper.setViewEnabled(commentChannelSpinner, false);
        Helper.setViewEnabled(inputComment, false);
        Helper.setViewEnabled(buttonClearReplyToComment, false);
        Helper.setViewEnabled(buttonPostComment, false);
    }

    private void afterPostComment() {
        Helper.setViewEnabled(commentChannelSpinner, true);
        Helper.setViewEnabled(inputComment, true);
        Helper.setViewEnabled(buttonClearReplyToComment, true);
        Helper.setViewEnabled(buttonPostComment, true);
        postingComment = false;
    }

    private Comment buildPostComment() {
        Claim actualClaim = collectionClaimItem != null ? collectionClaimItem : fileClaim;
        Comment comment = new Comment();
        Claim channel = (Claim) commentChannelSpinner.getSelectedItem();
        comment.setClaimId(actualClaim.getClaimId());
        comment.setChannelId(channel.getClaimId());
        comment.setChannelName(channel.getName());
        comment.setText(Helper.getValue(inputComment.getText()));
        comment.setPoster(channel);
        if (replyToComment != null) {
            comment.setParentId(replyToComment.getId());
        }

        return comment;
    }

    private void setReplyToComment(Comment comment) {
        replyToComment = comment;
        Helper.setViewText(textReplyingTo, getString(R.string.replying_to, comment.getChannelName()));
        Helper.setViewText(textReplyToBody, comment.getText());
        Helper.setViewVisibility(containerReplyToComment, View.VISIBLE);

        inputComment.requestFocus();
        Context context = getContext();
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(inputComment, InputMethodManager.SHOW_FORCED);
        }
    }

    private void clearReplyToComment() {
        Helper.setViewText(textReplyingTo, null);
        Helper.setViewText(textReplyToBody, null);
        Helper.setViewVisibility(containerReplyToComment, View.GONE);
        replyToComment = null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void switchCommentListVisibility(Boolean expanded) {
        View root = getView();
        View relatedContentArea = root.findViewById(R.id.file_view_related_content_area);
        View actionsArea = root.findViewById(R.id.file_view_actions_area);
        View publisherArea = root.findViewById(R.id.file_view_publisher_area);
        ImageButton expandButton = root.findViewById(R.id.expand_commentarea_button);
        Context context = getContext();

        if (expanded) {
            root.findViewById(R.id.collapsed_comment).setVisibility(View.GONE);
            Helper.setViewVisibility(containerCommentForm, View.VISIBLE);
            Helper.setViewVisibility(relatedContentArea, View.GONE);
            Helper.setViewVisibility(actionsArea, View.GONE);
            Helper.setViewVisibility(publisherArea, View.GONE);
            if (context != null)
                expandButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_close, context.getTheme()));
        } else {
            Helper.setViewVisibility(containerCommentForm, View.GONE);
            root.findViewById(R.id.collapsed_comment).setVisibility(View.VISIBLE);
            Helper.setViewVisibility(relatedContentArea, View.VISIBLE);
            Helper.setViewVisibility(actionsArea, View.VISIBLE);
            Helper.setViewVisibility(publisherArea, View.VISIBLE);
            if (context != null)
                expandButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand, context.getTheme()));
        }
    }

    private void postComment() {
        if (postingComment) {
            return;
        }

        Comment comment = buildPostComment();

        beforePostComment();
        AccountManager am = AccountManager.get(getContext());
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());

        CommentCreateTask task = new CommentCreateTask(comment, am.peekAuthToken(odyseeAccount, "auth_token_type"), progressPostComment, new CommentCreateTask.CommentCreateWithTipHandler() {
            @Override
            public void onSuccess(Comment createdComment) {
                inputComment.setText(null);
                clearReplyToComment();

                final boolean thisIsFirstComment = commentListAdapter == null;

                ensureCommentListAdapterCreated(new ArrayList<Comment>());

                if (commentListAdapter != null) {
                    createdComment.setPoster(comment.getPoster());
                    if (!Helper.isNullOrEmpty(createdComment.getParentId())) {
                        commentListAdapter.addReply(createdComment);
                    } else {
                        commentListAdapter.insert(0, createdComment);
                    }
                }
                afterPostComment();
                checkNoComments();

                if ( thisIsFirstComment ) {
                    expandButton.performClick();
                }

                singleCommentRoot.setVisibility(View.GONE);

                Bundle bundle = new Bundle();
                bundle.putString("claim_id", fileClaim != null ? fileClaim.getClaimId() : null);
                bundle.putString("claim_name", fileClaim != null ? fileClaim.getName() : null);
                LbryAnalytics.logEvent(LbryAnalytics.EVENT_COMMENT_CREATE, bundle);

                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).showMessage(R.string.comment_posted);
                }
            }

            @Override
            public void onError(Exception error) {
                try {
                    showError(error != null ? error.getMessage() : getString(R.string.comment_error));
                } catch (IllegalStateException ex) {
                    // pass
                }
                afterPostComment();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void react(Comment comment, boolean like) {
        JSONObject options = new JSONObject();
        try {
            options.put("comment_ids", comment.getId());
            options.put("type", like ? "like" : "dislike");
            options.put("clear_types", like ? "dislike" : "like");

            /**
             * This covers the case of a fresh comment being made and added to the list, and then liked by
             * the commenter themself, but the {@link Reactions} field is not instantiated in this case. It
             * would perhaps be better to fix this upstream and make this situation impossible, but even then
             * this last line of defense doesn't hurt.
             */
            if (comment.getReactions() == null) {
                comment.setReactions(Reactions.newInstanceWithNoLikesOrDislikes());
            }

            if ((like && comment.getReactions().isLiked()) || (!like && comment.getReactions().isDisliked())) {
                options.put("remove", true);
            }

            AccountManager am = AccountManager.get(getContext());
            Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
            if (odyseeAccount != null) {
                options.put("auth_token", am.peekAuthToken(odyseeAccount, "auth_token_type"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // This makes a network connection, so it needs to be executed on a different thread than main.
                if (Lbry.ownChannels.size() > 0) {
                    try {
                        // This makes a network connection, so it needs to be executed on a different thread than main.
                        Callable<JSONObject> optionsCallable = new BuildCommentReactOptions(options);
                        Future<JSONObject> optionsFuture = executor.submit(optionsCallable);

                        JSONObject opt = optionsFuture.get();

                        Future<?> futureReactions = executor.submit(new ReactToComment(opt));
                        futureReactions.get();

                        if (!executor.isShutdown()) {
                            executor.shutdown();
                        }
                        refreshCommentAfterReacting(comment);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (!executor.isShutdown()) {
                            executor.shutdown();
                        }
                    }
                }
            }
        });
        thread.start();
    }

    private void refreshCommentAfterReacting(Comment comment) {
        Map<String, Reactions> reactionsList = loadReactions(Collections.singletonList(comment));

        if (reactionsList != null && reactionsList.containsKey(comment.getId())){
            Activity activity = getActivity();

            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        commentListAdapter.updateReactions(comment, reactionsList.get(comment.getId()));
                    }
                });
            }
        }
    }

    private void react(Claim claim, boolean like) {
        Runnable runnable = () -> {
            purgeLoadingReactionsTask();

            Map<String, String> options = new HashMap<>();
            options.put("claim_ids", claim.getClaimId());
            options.put("type", like ? "like" : "dislike");
            options.put("clear_types", like ? "dislike" : "like");

            if ((like && claim.isLiked()) || (!like && claim.isDisliked()))
                options.put("remove", "true");

            try {
                JSONObject jsonResponse = (JSONObject) Lbryio.parseResponse(Lbryio.call("reaction", "react", options, Helper.METHOD_POST, getContext()));

                if (jsonResponse != null && jsonResponse.has(claim.getClaimId())) {
                    reactions.setLiked(jsonResponse.getJSONObject(claim.getClaimId()).has("like") && !reactions.isLiked());
                    reactions.setDisliked(jsonResponse.getJSONObject(claim.getClaimId()).has("dislike") && !reactions.isDisliked());
                    updateContentReactions();
                }
            } catch (LbryioRequestException | LbryioResponseException | JSONException e) {
                e.printStackTrace();
            } finally {
                loadReactions(claim);
            }
        };

        try {
            new Thread(runnable).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyFilterForBlockedChannels(List<LbryUri> blockedChannels) {
        if (relatedContentAdapter != null) {
            relatedContentAdapter.filterBlockedChannels(blockedChannels);
        }
        if (commentListAdapter != null) {
            commentListAdapter.filterBlockedChannels(blockedChannels);
        }
    }

    class CodeAttributeProvider implements AttributeProvider {

        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            Context context = getContext();
            if (node instanceof Code && context != null) {
                String colorCodeText = "#".concat(Integer.toHexString(ContextCompat.getColor(context, R.color.codeTagText) & 0x00ffffff));
                String colorCodeBg = "#".concat(Integer.toHexString(ContextCompat.getColor(context, R.color.codeTagBackground) & 0x00ffffff));
                String codeStyle = "display: inline-block; border-radius: 0.2rem" +
                        "; color: " + colorCodeText + "; background-color: " + colorCodeBg +
                        "; font-size: 0.8571rem; padding: calc(2rem/5 - 4px) calc(2rem/5) calc(2rem/5 - 5px);";
                attributes.put("style", codeStyle);
            }
        }
    }
}
