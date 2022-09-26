package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AnyThread;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.odysee.app.adapter.ProfileDefaultChannelAdapter;
import com.odysee.app.callable.WalletBalanceFetch;
import com.odysee.app.dialog.AddToListsDialogFragment;
import com.odysee.app.listener.VerificationListener;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.lbryinc.CustomBlockRule;
import com.odysee.app.model.lbryinc.OdyseeLocale;
import com.odysee.app.model.lbryinc.RewardVerified;
import com.odysee.app.tasks.RewardVerifiedHandler;
import com.odysee.app.tasks.claim.ResolveResultHandler;
import com.odysee.app.ui.channel.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLParameters;

import com.odysee.app.adapter.NotificationListAdapter;
import com.odysee.app.adapter.StartupStageAdapter;
import com.odysee.app.adapter.UrlSuggestionListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.dialog.ContentScopeDialogFragment;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.AuthTokenInvalidatedException;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.listener.CameraPermissionListener;
import com.odysee.app.listener.DownloadActionListener;
import com.odysee.app.listener.FetchChannelsListener;
import com.odysee.app.listener.FetchClaimsListener;
import com.odysee.app.listener.FilePickerListener;
import com.odysee.app.listener.PIPModeListener;
import com.odysee.app.listener.ScreenOrientationListener;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.listener.StoragePermissionListener;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ClaimCacheKey;
import com.odysee.app.model.StartupStage;
import com.odysee.app.model.Tag;
import com.odysee.app.model.UrlSuggestion;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.model.WalletSync;
import com.odysee.app.model.lbryinc.LbryNotification;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.supplier.FetchRewardsSupplier;
import com.odysee.app.supplier.GetLocalNotificationsSupplier;
import com.odysee.app.supplier.NotificationListSupplier;
import com.odysee.app.supplier.NotificationUpdateSupplier;
import com.odysee.app.supplier.UnlockingTipsSupplier;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.lbryinc.ClaimRewardTask;
import com.odysee.app.tasks.MergeSubscriptionsTask;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.lbryinc.NotificationDeleteTask;
import com.odysee.app.tasks.localdata.FetchRecentUrlHistoryTask;
import com.odysee.app.tasks.wallet.DefaultSyncTaskHandler;
import com.odysee.app.tasks.wallet.LoadSharedUserStateTask;
import com.odysee.app.tasks.wallet.SaveSharedUserStateTask;
import com.odysee.app.tasks.wallet.SyncApplyTask;
import com.odysee.app.tasks.wallet.SyncGetTask;
import com.odysee.app.tasks.wallet.SyncSetTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.findcontent.FileViewFragment;
import com.odysee.app.ui.findcontent.FollowingFragment;
import com.odysee.app.ui.other.BlockedAndMutedFragment;
import com.odysee.app.ui.other.CreatorSettingsFragment;
import com.odysee.app.ui.rewards.RewardVerificationFragment;
import com.odysee.app.ui.library.LibraryFragment;
import com.odysee.app.ui.library.PlaylistFragment;
import com.odysee.app.ui.other.SettingsFragment;
import com.odysee.app.ui.publish.PublishFormFragment;
import com.odysee.app.ui.publish.PublishFragment;
import com.odysee.app.ui.publish.PublishesFragment;
import com.odysee.app.ui.findcontent.AllContentFragment;
import com.odysee.app.ui.findcontent.SearchFragment;
import com.odysee.app.ui.wallet.InvitesFragment;
import com.odysee.app.ui.wallet.RewardsFragment;
import com.odysee.app.ui.wallet.WalletFragment;
import com.odysee.app.utils.CastHelper;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.ContentSources;
import com.odysee.app.utils.FirebaseMessagingToken;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.PlayerManager;
import com.odysee.app.utils.PurchasedChecker;
import com.odysee.app.utils.Utils;
import com.odysee.app.utils.VerificationSkipQueue;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener,
        ActionMode.Callback, SelectionModeListener, OnAccountsUpdateListener, VerificationListener {
    private static final String PLAYER_NOTIFICATION_CHANNEL_ID = "com.odysee.app.PLAYER_NOTIFICATION_CHANNEL";
    private static final int PLAYBACK_NOTIFICATION_ID = 3;
    private static final String SPECIAL_URL_PREFIX = "lbry://?";
    private static final int REMOTE_NOTIFICATION_REFRESH_TTL = 300000; // 5 minutes
    public static final String SKU_SKIP = "lbryskip";

    public static final int SOURCE_NOW_PLAYING_FILE = 1;
    public static final int SOURCE_NOW_PLAYING_SHUFFLE = 2;
    public static MainActivity instance;
    private int pendingSourceTabId;

    @Getter
    private boolean shuttingDown;
    private Date remoteNotifcationsLastLoaded;
    private Map<String, Class> specialRouteFragmentClassMap;
    @Getter
    private boolean inPictureInPictureMode;
    @Getter
    private boolean inFullscreenMode;
    // make tip unlock a global operation
    @Getter
    private boolean unlockingTips;

    private VerificationSkipQueue verificationSkipQueue;

    public static PlayerManager playerManager;
    public static Cache playerCache;
    public static boolean playerReassigned;
    public boolean mediaRouteButtonVisible;
    public MediaRouteButton appBarMediaRouteButton;
    public MediaRouteButton playerMediaRouteButton;
    public static int nowPlayingSource;
    public static Claim nowPlayingClaim;
    public static String nowPlayingClaimUrl;
    public static boolean videoIsTranscoded;
    public static int videoQuality;
    public static boolean startingFilePickerActivity = false;
    public static boolean startingShareActivity = false;
    public static boolean startingPermissionRequest = false;
    public static final boolean startingSignInFlowActivity = false;

    @Getter
    private boolean userInstallInitialised;
    @Getter
    private boolean initialCategoriesLoaded;
    @Getter
    private boolean customBlockingLoaded;
    private boolean initialBlockedChannelsLoaded;
    private ActionMode actionMode;
    @Getter
    private PurchasedChecker purchasedChecker;
    @Getter
    private boolean enteringPIPMode = false;
    private boolean fullSyncInProgress = false;
    private int queuedSyncCount = 0;
    private String cameraOutputFilename;
    private Bitmap nowPlayingClaimBitmap;
    private Fragment currentDisplayFragment;
    private boolean rewardVerificationActive;

    @Setter
    private BackPressInterceptor backPressInterceptor;
    private WebSocketClient webSocketClient;

    private int bottomNavigationHeight = 0;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Getter
    private String firebaseMessagingToken;

    @Getter
    private CastHelper castHelper;

    private NotificationListAdapter notificationListAdapter;

    private static final Map<Class, Integer> fragmentClassNavIdMap = new HashMap<>();
    static {
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    public static final int REQUEST_STORAGE_PERMISSION = 1001;
    public static final int REQUEST_CAMERA_PERMISSION = 1002;
    public static final int REQUEST_SIMPLE_SIGN_IN = 2001;
    public static final int REQUEST_WALLET_SYNC_SIGN_IN = 2002;
    public static final int REQUEST_REWARDS_VERIFY_SIGN_IN = 2003;

    public static final int REQUEST_FILE_PICKER = 5001;
    public static final int REQUEST_VIDEO_CAPTURE = 5002;
    public static final int REQUEST_TAKE_PHOTO = 5003;

    private static final String SP_NAME = "app";
    private static final String KEY_ALIAS = "LBRYKey";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private static final int SIGN_IN_SOURCE_NOTIFICATIONS = -99;
    private static final int SIGN_IN_SOURCE_PUBLISH = -98;

    // broadcast action names
    public static final String ACTION_AUTH_TOKEN_GENERATED = "com.odysee.app.Broadcast.AuthTokenGenerated";
    public static final String ACTION_USER_AUTHENTICATION_SUCCESS = "com.odysee.app.Broadcast.UserAuthenticationSuccess";
    public static final String ACTION_USER_SIGN_IN_SUCCESS = "com.odysee.app.Broadcast.UserSignInSuccess";
    public static final String ACTION_USER_AUTHENTICATION_FAILED = "com.odysee.app.Broadcast.UserAuthenticationFailed";
    public static final String ACTION_NOW_PLAYING_CLAIM_UPDATED = "com.odysee.app.Broadcast.NowPlayingClaimUpdated";
    public static final String ACTION_NOW_PLAYING_CLAIM_CLEARED = "com.odysee.app.Broadcast.NowPlayingClaimCleared";
    public static final String ACTION_PUBLISH_SUCCESSFUL = "com.odysee.app.Broadcast.PublishSuccessful";
    public static final String ACTION_OPEN_ALL_CONTENT_TAG = "com.odysee.app.Broadcast.OpenAllContentTag";
    public static final String ACTION_WALLET_BALANCE_UPDATED = "com.odysee.app.Broadcast.WalletBalanceUpdated";
    public static final String ACTION_OPEN_CHANNEL_URL = "com.odysee.app.Broadcast.OpenChannelUrl";
    public static final String ACTION_OPEN_WALLET_PAGE = "com.odysee.app.Broadcast.OpenWalletPage";
    public static final String ACTION_OPEN_REWARDS_PAGE = "com.odysee.app.Broadcast.OpenRewardsPage";
    public static final String ACTION_SAVE_SHARED_USER_STATE = "com.odysee.app.Broadcast.SaveSharedUserState";

    // preference keys
    public static final String PREFERENCE_KEY_INSTALL_ID = "com.odysee.app.InstallId";
    public static final String PREFERENCE_KEY_INTERNAL_BACKGROUND_PLAYBACK = "com.odysee.app.preference.userinterface.BackgroundPlayback";
    public static final String PREFERENCE_KEY_INTERNAL_BACKGROUND_PLAYBACK_PIP_MODE = "com.odysee.app.preference.userinterface.BackgroundPlaybackPIPMode";
    public static final String PREFERENCE_KEY_INTERNAL_AUTOPLAY_MEDIA = "com.odysee.app.preference.userinterface.AutoplayMedia";
    public static final String PREFERENCE_KEY_INTERNAL_WIFI_DEFAULT_QUALITY = "com.odysee.app.preference.userinterface.WifiDefaultQuality";
    public static final String PREFERENCE_KEY_INTERNAL_MOBILE_DEFAULT_QUALITY = "com.odysee.app.preference.userinterface.MobileDefaultQuality";
    public static final String PREFERENCE_KEY_INTERNAL_PLAYBACK_DEFAULT_SPEED = "com.odysee.app.preference.userinterface.PlaybackDefaultSpeed";
    public static final String PREFERENCE_KEY_DARK_MODE = "com.odysee.app.preference.userinterface.DarkMode";
    public static final String PREFERENCE_KEY_SHOW_MATURE_CONTENT = "com.odysee.app.preference.userinterface.ShowMatureContent";
    public static final String PREFERENCE_KEY_SHOW_URL_SUGGESTIONS = "com.odysee.app.preference.userinterface.UrlSuggestions";
    public static final String PREFERENCE_KEY_MINI_PLAYER_BOTTOM_MARGIN = "com.odysee.app.preference.userinterface.MiniPlayerBottomMargin";
    public static final String PREFERENCE_KEY_NOTIFICATION_COMMENTS = "com.odysee.app.preference.notifications.Comments";
    public static final String PREFERENCE_KEY_NOTIFICATION_SUBSCRIPTIONS = "com.odysee.app.preference.notifications.Subscriptions";
    public static final String PREFERENCE_KEY_NOTIFICATION_REWARDS = "com.odysee.app.preference.notifications.Rewards";
    public static final String PREFERENCE_KEY_NOTIFICATION_CONTENT_INTERESTS = "com.odysee.app.preference.notifications.ContentInterests";
    public static final String PREFERENCE_KEY_NOTIFICATION_CREATOR = "com.odysee.app.preference.notifications.Creator";
    public static final String PREFERENCE_KEY_SEND_BUFFERING_EVENTS = "com.odysee.app.preference.other.SendBufferingEvents";

    // Internal flags / setting preferences
    public static final String PREFERENCE_KEY_INTERNAL_SKIP_WALLET_ACCOUNT = "com.odysee.app.preference.internal.WalletSkipAccount";
    public static final String PREFERENCE_KEY_INTERNAL_WALLET_SYNC_ENABLED = "com.odysee.app.preference.internal.WalletSyncEnabled";
    public static final String PREFERENCE_KEY_INTERNAL_WALLET_RECEIVE_ADDRESS = "com.odysee.app.preference.internal.WalletReceiveAddress";
    public static final String PREFERENCE_KEY_INTERNAL_REWARDS_NOT_INTERESTED = "com.odysee.app.preference.internal.RewardsNotInterested";
    public static final String PREFERENCE_KEY_INTERNAL_NEW_ANDROID_REWARD_CLAIMED = "com.odysee.app.preference.internal.NewAndroidRewardClaimed";
    public static final String PREFERENCE_KEY_INTERNAL_INITIAL_SUBSCRIPTION_MERGE_DONE = "com.odysee.app.preference.internal.InitialSubscriptionMergeDone";
    public static final String PREFERENCE_KEY_INTERNAL_INITIAL_BLOCKED_LIST_LOADED = "com.odysee.app.preference.internal.InitialBlockedListLoaded";
    public static final String PREFERENCE_KEY_INTERNAL_INITIAL_COLLECTIONS_LOADED = "com.odysee.app.preference.internal.InitialCollectionsLoaded";

    public static final String PREFERENCE_KEY_INTERNAL_FIRST_RUN_COMPLETED = "com.odysee.app.preference.internal.FirstRunCompleted";
    public static final String PREFERENCE_KEY_INTERNAL_FIRST_AUTH_COMPLETED = "com.odysee.app.preference.internal.FirstAuthCompleted";
    public static final String PREFERENCE_KEY_INTERNAL_EMAIL_REWARD_CLAIMED = "com.odysee.app.preference.internal.EmailRewardClaimed";
    public static final String PREFERENCE_KEY_INTERNAL_FIRST_YOUTUBE_SYNC_DONE = "com.odysee.app.preference.internal.FirstYouTubeSyncDone";

    public static final String SECURE_VALUE_KEY_SAVED_PASSWORD = "com.odysee.app.PX";
    public static final String SECURE_VALUE_FIRST_RUN_PASSWORD = "firstRunPassword";

    public static final String APP_SETTING_DARK_MODE_NIGHT = "night";
    public static final String APP_SETTING_DARK_MODE_NOTNIGHT = "notnight";
    public static final String APP_SETTING_DARK_MODE_SYSTEM = "system";

    public static final String APP_SETTING_AUTOPLAY_NEVER = "never";
    public static final String APP_SETTING_AUTOPLAY_NOTHING_PLAYING = "nothing_playing";
    public static final String APP_SETTING_AUTOPLAY_ALWAYS = "always";

    private static final String TAG = "OdyseeMain";
    private static final String FILE_VIEW_TAG = "FileView";

    private UrlSuggestionListAdapter urlSuggestionListAdapter;
    private List<UrlSuggestion> recentUrlHistory;
    private boolean hasLoadedFirstBalance;

    // broadcast receivers
    private BroadcastReceiver requestsReceiver;
    private BroadcastReceiver uaReceiver;

    private static boolean appStarted;
    private PlayerNotificationManager playerNotificationManager;
    private MediaSessionCompat mediaSession;
    private ActionBarDrawerToggle toggle;
    private SwipeRefreshLayout notificationsSwipeContainer;
    private SyncSetTask syncSetTask = null;
    private List<WalletSync> pendingSyncSetQueue;
    private static DatabaseHelper dbHelper;
    private List<CameraPermissionListener> cameraPermissionListeners;
    private List<DownloadActionListener> downloadActionListeners;
    private List<FilePickerListener> filePickerListeners;
    private List<PIPModeListener> pipModeListeners;
    private List<ScreenOrientationListener> screenOrientationListeners;
    private List<StoragePermissionListener> storagePermissionListeners;
    private List<WalletBalanceListener> walletBalanceListeners;
    private List<FetchClaimsListener> fetchClaimsListeners;
    private List<FetchChannelsListener> fetchChannelsListeners;
    @Getter
    private ScheduledFuture<?> scheduledWalletUpdater;
    private boolean walletSyncScheduled;

    ScheduledFuture<?> scheduledSearchFuture;
    private boolean autoSearchEnabled = false;
    ChannelCreateDialogFragment channelCreationBottomSheet;
    AccountManager accountManager;

    @Getter
    private OdyseeLocale odyseeLocale;
    @Getter
    private Map<String, List<CustomBlockRule>> customBlockingRulesMap = new HashMap<>();

    private static final String KEY_BLOCKING_LIVESTREAMS = "livestreams";
    private static final String KEY_BLOCKING_VIDEOS = "videos";

    private static final String KEY_SCOPE_COUNTRIES = "countries";
    private static final String KEY_SCOPE_CONTINENTS = "continents";
    private static final String KEY_SCOPE_SPECIALS = "specials";

    // startup stages (to be able to determine how far a user made it if startup fails)
    // and display a more useful message for troubleshooting
    private static final int STARTUP_STAGE_INSTALL_ID_LOADED = 1;
    private static final int STARTUP_STAGE_KNOWN_TAGS_LOADED = 2;
    private static final int STARTUP_STAGE_EXCHANGE_RATE_LOADED = 3;
    private static final int STARTUP_STAGE_USER_AUTHENTICATED = 4;
    private static final int STARTUP_STAGE_NEW_INSTALL_DONE = 5;
    private static final int STARTUP_STAGE_SUBSCRIPTIONS_LOADED = 6;
    private static final int STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED = 7;
    private static final int STARTUP_STAGE_BLOCK_LIST_LOADED = 8;
    private static final int STARTUP_STAGE_FILTER_LIST_LOADED = 9;
    private static final int DEFAULT_MINI_PLAYER_MARGIN = 4;

    private boolean readyToDraw = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findViewById(R.id.root).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            findViewById(R.id.launch_splash).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.root).getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // Check if the initial data is ready.
                        if (readyToDraw) {
                            // The content is ready; start drawing.
                            findViewById(R.id.root).getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        } else {
                            // The content is not ready; suspend.
                            return false;
                        }
                    }
                });
        }
        instance = this;
        // workaround to fix dark theme because https://issuetracker.google.com/issues/37124582
        try {
            new WebView(this);
        } catch (Exception ex) {
            // pass (don't fail initialization on some _weird_ device implementations)
        }

        initKeyStore();
        loadAuthToken();

        // Change status bar text color depending on Night mode when app is running
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT) && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                //noinspection deprecation
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            int defaultNight = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NOTNIGHT) || (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_SYSTEM) && defaultNight == Configuration.UI_MODE_NIGHT_NO)) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    //noinspection deprecation
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        }

        activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // first run completed or skipped
                        checkFirstYouTubeSync();
                        return;
                    }

                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        // back button pressed, so it was cancelled
                        finish();
                    }
                }
            });

        initSpecialRouteMap();

        LbryAnalytics.init(this);
        FirebaseMessagingToken.getFirebaseMessagingToken(new FirebaseMessagingToken.GetTokenListener() {
            @Override
            public void onComplete(String token) {
                firebaseMessagingToken = token;
            }
        });

        // create player notification channel
        NotificationManager notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PLAYER_NOTIFICATION_CHANNEL_ID, "Odysee Player", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Odysee player notification channel");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }

        dbHelper = new DatabaseHelper(this);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    DatabaseHelper.checkAndCreateBuiltinPlaylists(dbHelper.getWritableDatabase());
                }
            }
        });
        checkNotificationOpenIntent(getIntent());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateMiniPlayerMargins(true);
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // setup the purchased checker in main activity (to handle cases where the verification purchase flow may have been interrupted)
        purchasedChecker = new PurchasedChecker(this, MainActivity.this);
        purchasedChecker.createBillingClientAndEstablishConnection();

        playerNotificationManager = new PlayerNotificationManager.Builder(this, PLAYBACK_NOTIFICATION_ID, PLAYER_NOTIFICATION_CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationDescriptionAdapter()).build();

        // TODO: Check Google Play Services availability
        // Listener here is not called in foss build
        appBarMediaRouteButton = findViewById(R.id.app_bar_media_route_button);
        castHelper = new CastHelper(this, appBarMediaRouteButton, new CastHelper.Listener() {
            @Override
            public void updateMediaRouteButtonVisibility(boolean isVisible) {
                mediaRouteButtonVisible = isVisible;
                appBarMediaRouteButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                if (playerMediaRouteButton != null) {
                    playerMediaRouteButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                }
            }
        });
        castHelper.setUpCastButton(appBarMediaRouteButton);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_main), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.onApplyWindowInsets(findViewById(R.id.url_suggestions_container),
                        insets.replaceSystemWindowInsets(0, 0, 0, insets.getSystemWindowInsetBottom()));

                return ViewCompat.onApplyWindowInsets(v,
                        insets.replaceSystemWindowInsets(
                                0,
                                0,
                                0,
                                insets.getSystemWindowInsetBottom()));
            }
        });

        // verification skip queue
        verificationSkipQueue = new VerificationSkipQueue(this, new VerificationSkipQueue.ShowInProgressListener() {
            @Override
            public void maybeShowRequestInProgress() {

            }
        }, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }

                if (!rewardVerified.isRewardApproved()) {
                    // show pending purchase message (possible slow card tx)
                    showMessage(getString(R.string.purchase_request_pending));
                } else  {
                    showMessage(getString(R.string.reward_verification_successful));
                }
            }

            @Override
            public void onError(Exception error) {
                showError(getString(R.string.purchase_request_failed_error));
            }
        });
        verificationSkipQueue.createBillingClientAndEstablishConnection();

        // register receivers
        registerRequestsReceiver();
        registerUAReceiver();

        // setup uri bar
//        setupUriBar();
        initNotificationsPage();
        loadUnseenNotificationsCount();

        // other
        pendingSyncSetQueue = new ArrayList<>();

        cameraPermissionListeners = new ArrayList<>();
        downloadActionListeners = new ArrayList<>();
        fetchChannelsListeners = new ArrayList<>();
        fetchClaimsListeners = new ArrayList<>();
        filePickerListeners = new ArrayList<>();
        pipModeListeners = new ArrayList<>();
        screenOrientationListeners = new ArrayList<>();
        storagePermissionListeners = new ArrayList<>();
        walletBalanceListeners = new ArrayList<>();

        SharedPreferences sharedPreferences = getSharedPreferences("lbry_shared_preferences", MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();

        if (sharedPreferences.getString("lbry_installation_id", "").equals("")) {
            Lbry.INSTALLATION_ID = Lbry.generateId();
            sharedPreferencesEditor.putString("lbry_installation_id", Lbry.INSTALLATION_ID);
            sharedPreferencesEditor.commit();
        }

        // Preliminary internal releases of Odysee was using SharedPreferences to store the authentication token.
        // Currently, it is using Android AccountManager, so let's check if value is stored and remove it for
        // privacy concerns.
        if (sharedPreferences.contains("auth_token")) {
            sharedPreferencesEditor.remove("auth_token").apply();
        }

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor defaultSharedPreferencesEditor = defaultSharedPreferences.edit();
        if (defaultSharedPreferences.contains("com.odysee.app.Preference.AuthToken")) {
            defaultSharedPreferencesEditor.remove("com.odysee.app.Preference.AuthToken").apply();
        }

        // Create Fragment instances here so they are not recreated when selected on the bottom navigation bar
        Fragment homeFragment = new AllContentFragment();
        Fragment followingFragment = new FollowingFragment();
        Fragment walletFragment = new WalletFragment();
        Fragment libraryFragment = new LibraryFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(backStackChangedListener);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment;
                String fragmentTag;

                if (!isSignedIn() && item.getItemId() != R.id.action_home_menu) {
                    simpleSignIn(item.getItemId());
                    return false;
                }

                switch (item.getItemId()) {
                    case R.id.action_home_menu:
                    default:
                        selectedFragment = homeFragment;
                        fragmentTag = "HOME";
                        break;
                    case R.id.action_following_menu:
                        selectedFragment = followingFragment;
                        fragmentTag = "FOLLOWING";
                        break;
                    case R.id.action_wallet_menu:
                        selectedFragment = walletFragment;
                        fragmentTag = "WALLET";
                        break;
                    case R.id.action_library_menu:
                        selectedFragment = libraryFragment;
                        fragmentTag = "LIBRARY";
                        break;
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_main_activity, selectedFragment, fragmentTag).commit();

                return true;
            }
        });
        bottomNavigation.setSelectedItemId(R.id.action_home_menu);

        findViewById(R.id.brand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomNavigation.setSelectedItemId(R.id.action_home_menu);
            }
        });

        findViewById(R.id.wallet_balance_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideNotifications();
                bottomNavigation.setSelectedItemId(R.id.action_wallet_menu);
            }
        });

        findViewById(R.id.upload_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSignedIn()) {
                    simpleSignIn(SIGN_IN_SOURCE_PUBLISH);
                    return;
                }

                showPublishFlow();
            }
        });

        findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Enter Search Mode
                hideNotifications();
                hideBottomNavigation();
                switchToolbarForSearch(true);
                findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);

                if (!isSearchUIActive()) {
                    try {
                        SearchFragment searchFragment = SearchFragment.class.newInstance();
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_search, searchFragment, "SEARCH").commit();
                        currentDisplayFragment = searchFragment;
                        findViewById(R.id.fragment_container_search).setVisibility(View.VISIBLE);
                        findViewById(R.id.search_query_text).requestFocus();
                        InputMethodManager imm =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(findViewById(R.id.search_query_text), InputMethodManager.SHOW_FORCED);
                    } catch (IllegalAccessException | InstantiationException e) {
                        e.printStackTrace();
                    }
                } else {
                    EditText queryText = findViewById(R.id.search_query_text);
                    // hide keyboard
                    InputMethodManager inputMethodManager = (InputMethodManager) queryText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(queryText.getWindowToken(), 0);

                    findViewById(R.id.fragment_container_search).setVisibility(View.VISIBLE);
                    String query = queryText.getText().toString();

                    SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag("SEARCH");

                    if (searchFragment != null) {
                        searchFragment.search(query, 0);
                    }
                }
            }
        });

        ((EditText)findViewById(R.id.search_query_text)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Context context = getApplicationContext();
                if (context != null) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    autoSearchEnabled = sp.getBoolean("com.odysee.app.preference.userinterface.Autosearch", false);
                }
                if (autoSearchEnabled) {
                    // Cancel any previously scheduled search as soon as possible if not yet running.
                    // Let it finish otherwise, as it will be re-scheduled on aftertextChanged()
                    if (scheduledSearchFuture != null && !scheduledSearchFuture.isCancelled()) {
                        scheduledSearchFuture.cancel(false);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (autoSearchEnabled) {
                    if (!s.toString().equals("")) {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        EditText queryText = findViewById(R.id.search_query_text);

                                        findViewById(R.id.fragment_container_search).setVisibility(View.VISIBLE);
                                        String query = queryText.getText().toString();

                                        SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag("SEARCH");

                                        if (searchFragment != null) {
                                            searchFragment.search(query, 0);
                                        }
                                    }
                                });
                            }
                        };
                        scheduledSearchFuture = ((OdyseeApp) getApplication()).getScheduledExecutor().schedule(runnable, 500, TimeUnit.MILLISECONDS);
                    } else {
                        SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag("SEARCH");

                        if (searchFragment != null) {
                            searchFragment.search("", 0);
                        }
                    }
                }
            }
        });

        ((EditText)findViewById(R.id.search_query_text)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    findViewById(R.id.search_button).callOnClick();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.search_close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText queryText = findViewById(R.id.search_query_text);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(queryText.getWindowToken(), 0);

                Fragment searchFragment = getSupportFragmentManager().findFragmentByTag("SEARCH");
                if (searchFragment != null) {
                    getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
                }

                ((EditText)findViewById(R.id.search_query_text)).setText("");
                showBottomNavigation();
                switchToolbarForSearch(false);

                /* FIXME (when tablet support): Home screen not shown because File View never hidden (#???)
                // On tablets, multiple fragments could be visible. Don't show Home Screen when File View is visible
                if (findViewById(R.id.main_activity_other_fragment).getVisibility() != View.VISIBLE) {
                    findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
                }*/
                findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);

                showWalletBalance();
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);
            }
        });

        Context ctx = this;
        findViewById(R.id.clear_all_library_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx).
                        setTitle(R.string.confirm_clear_view_history_title).
                        setMessage(R.string.confirm_clear_view_history)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SQLiteDatabase db = DatabaseHelper.getInstance().getWritableDatabase();
                                        DatabaseHelper.clearViewHistory(db);

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ((LibraryFragment) libraryFragment).onViewHistoryCleared();
                                            }
                                        });
                                    }
                                });
                                t.start();
                            }
                        }).setNegativeButton(R.string.no, null);
                builder.show();
            }
        });

        findViewById(R.id.profile_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.profile_button).setEnabled(false);
                LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View customView = layoutInflater.inflate(R.layout.popup_user,null);
                PopupWindow popupWindow = new PopupWindow(customView, getScaledValue(240), WindowManager.LayoutParams.WRAP_CONTENT);
                popupWindow.setFocusable(true);
                popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        findViewById(R.id.profile_button).setEnabled(true);
                    }
                });

                ImageButton closeButton = customView.findViewById(R.id.popup_user_close_button);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                    }
                });
                MaterialButton signUserButton = customView.findViewById(R.id.button_sign_user);

                View buttonChangeDefaultChannel = customView.findViewById(R.id.button_change_default_channel);
                View defaultChannelListParent = customView.findViewById(R.id.default_channel_list_layout);
                ListView defaultChannelList = customView.findViewById(R.id.default_channel_list);
                View buttonGoLive = customView.findViewById(R.id.button_go_live);
                View buttonChannels = customView.findViewById(R.id.button_channels);
                View buttonBlockedAndMuted = customView.findViewById(R.id.button_blocked_and_muted);
                View buttonCreatorSettings = customView.findViewById(R.id.button_creator_settings);
                View buttonPublishes = customView.findViewById(R.id.button_publishes);
                View buttonShowRewards = customView.findViewById(R.id.button_show_rewards);
                View buttonYouTubeSync = customView.findViewById(R.id.button_youtube_sync);
                View buttonSignOut = customView.findViewById(R.id.button_sign_out);

                TextView userIdText = customView.findViewById(R.id.user_id);

                AccountManager am = AccountManager.get(getApplicationContext());
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                final boolean isSignedIn = odyseeAccount != null;

                buttonGoLive.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonChannels.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonCreatorSettings.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonPublishes.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonShowRewards.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonYouTubeSync.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);
                buttonSignOut.setVisibility(isSignedIn ? View.VISIBLE : View.GONE);

                if (isSignedIn) {
                    userIdText.setVisibility(View.VISIBLE);
                    signUserButton.setVisibility(View.GONE);
                    userIdText.setText(am.getUserData(odyseeAccount, "email"));

                    if (Lbry.ownChannels.size() > 0) {
                        buttonChangeDefaultChannel.setVisibility(View.VISIBLE);
                    } else {
                        buttonChangeDefaultChannel.setVisibility(View.GONE);
                    }
                } else {
                    userIdText.setVisibility(View.GONE);
                    userIdText.setText("");
                    signUserButton.setVisibility(View.VISIBLE);
                    signUserButton.setText(getString(R.string.sign_up_log_in));
                    buttonChangeDefaultChannel.setVisibility(View.GONE);
                }

                customView.findViewById(R.id.button_app_settings).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(SettingsFragment.class, true, null);
                    }
                });

                customView.findViewById(R.id.button_community_guidelines).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFileUrl(getResources().getString(R.string.community_guidelines_url));
                    }
                });

                customView.findViewById(R.id.button_help_support).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openChannelUrl(getResources().getString(R.string.help_and_support_url));
                    }
                });

                buttonChangeDefaultChannel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (defaultChannelListParent.getVisibility() == View.GONE) {
                            int listHeight = Math.round(getResources().getDisplayMetrics().density);
                            List<String> ownChannels = Lbry.ownChannels.stream().map(Claim::getName).filter(name -> name != null).collect(Collectors.toList());
                            ProfileDefaultChannelAdapter adapter = new ProfileDefaultChannelAdapter(ctx, ownChannels);

                            defaultChannelList.setAdapter(adapter);

                            String defaultChannel = Helper.getDefaultChannelName(ctx);

                            if (defaultChannel != null) {
                                adapter.setDefaultChannelName(defaultChannel);
                            }

                            for (int i = 0; i < ownChannels.size(); i++) {
                                View item = adapter.getView(i, null, defaultChannelList);
                                item.measure(0, 0);
                                listHeight += item.getMeasuredHeight();
                            }

                            // Avoid scroll bars being displayed
                            ViewGroup.LayoutParams params = defaultChannelList.getLayoutParams();
                            params.height = listHeight + (defaultChannelList.getCount() + 1) * defaultChannelList.getDividerHeight();
                            defaultChannelList.setLayoutParams(params);
                            defaultChannelList.setVerticalScrollBarEnabled(false);
                            TransitionManager.beginDelayedTransition((ViewGroup) popupWindow.getContentView());
                            defaultChannelList.requestLayout();

                            buttonChannels.setVisibility(View.GONE);
                            buttonGoLive.setVisibility(View.GONE);
                            buttonPublishes.setVisibility(View.GONE);
                            buttonShowRewards.setVisibility(View.GONE);
                            customView.findViewById(R.id.button_help_support).setVisibility(View.GONE);
                            customView.findViewById(R.id.button_app_settings).setVisibility(View.GONE);
                            customView.findViewById(R.id.button_community_guidelines).setVisibility(View.GONE);
                            buttonSignOut.setVisibility(View.GONE);
                            buttonYouTubeSync.setVisibility(View.GONE);
                            defaultChannelListParent.setVisibility(View.VISIBLE);
                            ((ImageView) buttonChangeDefaultChannel.findViewById(R.id.expandable)).setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_arrow_dropup, getTheme()));
                        } else {
                            TransitionManager.beginDelayedTransition((ViewGroup) popupWindow.getContentView());
                            buttonChannels.setVisibility(View.VISIBLE);
                            buttonGoLive.setVisibility(View.VISIBLE);
                            buttonPublishes.setVisibility(View.VISIBLE);
                            buttonShowRewards.setVisibility(View.VISIBLE);
                            customView.findViewById(R.id.button_help_support).setVisibility(View.VISIBLE);
                            customView.findViewById(R.id.button_app_settings).setVisibility(View.VISIBLE);
                            customView.findViewById(R.id.button_community_guidelines).setVisibility(View.VISIBLE);
                            buttonSignOut.setVisibility(View.VISIBLE);
                            buttonYouTubeSync.setVisibility(View.VISIBLE);
                            defaultChannelListParent.setVisibility(View.GONE);
                            ((ImageView) buttonChangeDefaultChannel.findViewById(R.id.expandable)).setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_arrow_dropdown, getTheme()));
                        }
                    }
                });

                defaultChannelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String defaultChannelName = (String) defaultChannelList.getAdapter().getItem(position);
                        AccountManager am = AccountManager.get(ctx);
                        am.setUserData(Helper.getOdyseeAccount(am.getAccounts()), "default_channel_name", defaultChannelName);
                        ((ProfileDefaultChannelAdapter)defaultChannelList.getAdapter()).setDefaultChannelName(defaultChannelName);
                    }
                });

                buttonGoLive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        startActivity(new Intent(MainActivity.this, GoLiveActivity.class));
                    }
                });
                buttonChannels.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(ChannelManagerFragment.class, true, null);
                    }
                });
                buttonBlockedAndMuted.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(BlockedAndMutedFragment.class, true, null);
                    }
                });
                buttonCreatorSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(CreatorSettingsFragment.class, true, null);
                    }
                });
                buttonPublishes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(PublishesFragment.class, true, null);
                    }
                });
                buttonShowRewards.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        openFragment(RewardsFragment.class, true, null);
                    }
                });
                buttonYouTubeSync.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        startActivity(new Intent(MainActivity.this, YouTubeSyncActivity.class));
                    }
                });
                signUserButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Close the popup window so its status gets updated when user opens it again
                        popupWindow.dismiss();
                        hideNotifications();
                        simpleSignIn(R.id.action_home_menu);
                    }
                });

                buttonSignOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                        hideNotifications();
                        if (isSignedIn) {
                            signOutUser();
                        }
                    }
                });

                int[] coords = new int[2];
                View profileButton = findViewById(R.id.profile_button);
                profileButton.getLocationInWindow(coords);
                int ypos = coords[1] + profileButton.getHeight() - 32;

                popupWindow.showAtLocation(findViewById(R.id.fragment_container_main_activity), Gravity.TOP|Gravity.END, 24, ypos);
                View container = (View) popupWindow.getContentView().getParent();
                WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();

                p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                p.dimAmount = 0.3f;
                wm.updateViewLayout(container, p);

            }
        });

        findViewById(R.id.global_now_playing_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopExoplayer();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                nowPlayingClaim = null;
                nowPlayingClaimUrl = null;
                nowPlayingClaimBitmap = null;
                findViewById(R.id.miniplayer).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.wunderbar_notifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View container = findViewById(R.id.notifications_container);
                if (container.getVisibility() != View.VISIBLE) {
                    if (!isSignedIn()) {
                        // use -99 to indicate that notifications should be displayed afterwards
                        simpleSignIn(SIGN_IN_SOURCE_NOTIFICATIONS);
                        return;
                    }

                    showNotifications();
                } else {
                    hideNotifications();
                }
            }
        });

        notificationsSwipeContainer = findViewById(R.id.notifications_list_swipe_container);
        notificationsSwipeContainer.setColorSchemeResources(R.color.odyseePink);
        notificationsSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                notificationsSwipeContainer.setRefreshing(true);
                loadRemoteNotifications(false);
            }
        });

        findViewById(R.id.global_now_playing_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (nowPlayingClaim != null && (!Helper.isNullOrEmpty(nowPlayingClaimUrl) || !Helper.isNullOrEmpty(nowPlayingClaim.getCanonicalUrl()))) {
                    hideNotifications();
                    hideGlobalNowPlaying();

                    String urlParam;

                    if (!Helper.isNullOrEmpty(nowPlayingClaimUrl)) {
                        urlParam = nowPlayingClaimUrl;
                    } else {
                        urlParam = nowPlayingClaim.getCanonicalUrl();
                    }
                    openFileUrl(urlParam);
                }
            }
        });

        accountManager = AccountManager.get(this);
    }

    public static DatabaseHelper getDatabaseHelper() {
        return dbHelper;
    }

    /**
     * Call this method when starting a new activity to avoid display glitches when user re-opens app from picture-in-picture
     */
    private void clearPlayingPlayer() {
        if (playerManager != null && playerManager.getCurrentPlayer().isPlaying()) {
            playerManager.getCurrentPlayer().stop();
            clearNowPlayingClaim();
        }
    }

    private void showPublishFlow() {
        // Hide bottom navigation
        // Hide main bar
        // Show PublishFragment.class
        clearPlayingPlayer();
        hideNotifications(); // Avoid showing Notifications fragment when clicking Publish when Notification panel is opened
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.main_activity_other_fragment, new PublishFragment(), "PUBLISH").addToBackStack("publish_claim").commit();
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
        hideActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            ActionBar actionBar = getSupportActionBar();

            if (actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void cancelScheduledSearchFuture() {
        if (scheduledSearchFuture != null && !scheduledSearchFuture.isCancelled()) {
            scheduledSearchFuture.cancel(true);
        }
    }
    public void hideToolbar() {
        findViewById(R.id.toolbar).setVisibility(View.GONE);
    }
    public void updateMiniPlayerMargins(boolean withBottomNavigation) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.miniplayer).getLayoutParams();
        int scaledMiniPlayerMargin = getScaledValue(DEFAULT_MINI_PLAYER_MARGIN);
        int scaledMiniPlayerBottomMargin = (withBottomNavigation ? bottomNavigationHeight : 0) + getScaledValue(2);
        if (lp.leftMargin != scaledMiniPlayerMargin || lp.rightMargin != scaledMiniPlayerMargin || lp.bottomMargin != scaledMiniPlayerBottomMargin) {
            lp.setMargins(scaledMiniPlayerMargin, 0, scaledMiniPlayerMargin, scaledMiniPlayerBottomMargin);
        }
    }


    /**
     * Returns the Battery Saver mode of the device.
     *
     * Note: Some manufacturers are always returning 'false' as its Battery Saver mode. That's not a standard behavior.
     * @return 'true' if device is in Battery Saver mode. 'false' otherwise
     */
    public boolean isBatterySaverMode() {
        Context ctx = getBaseContext();
        if (ctx != null) {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);

            return (pm != null && pm.isPowerSaveMode());
        }
        return false;
    }

    public boolean isBackgroundPlaybackEnabled() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_BACKGROUND_PLAYBACK, true);
    }

    public boolean isContinueBackgroundPlaybackPIPModeEnabled() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_BACKGROUND_PLAYBACK_PIP_MODE, false);
    }

    public String mediaAutoplayEnabled() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(PREFERENCE_KEY_INTERNAL_AUTOPLAY_MEDIA, APP_SETTING_AUTOPLAY_NOTHING_PLAYING);
    }

    public int wifiDefaultQuality() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sp.getString(PREFERENCE_KEY_INTERNAL_WIFI_DEFAULT_QUALITY, "0"));
    }

    public int mobileDefaultQuality() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sp.getString(PREFERENCE_KEY_INTERNAL_MOBILE_DEFAULT_QUALITY, "0"));
    }

    public int playbackDefaultSpeed() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return Integer.parseInt(sp.getString(PREFERENCE_KEY_INTERNAL_PLAYBACK_DEFAULT_SPEED, "100"));
    }

    public boolean initialSubscriptionMergeDone() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_SUBSCRIPTION_MERGE_DONE, false);
    }

    public boolean initialBlockedListLoaded() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_BLOCKED_LIST_LOADED, false);
    }

    public boolean initialCollectionsLoaded() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_COLLECTIONS_LOADED, false);
    }

    private void initSpecialRouteMap() {
        specialRouteFragmentClassMap = new HashMap<>();
        specialRouteFragmentClassMap.put("allcontent", AllContentFragment.class);
        specialRouteFragmentClassMap.put("channels", ChannelManagerFragment.class);
        specialRouteFragmentClassMap.put("invite", InvitesFragment.class);
        specialRouteFragmentClassMap.put("invites", InvitesFragment.class);
        specialRouteFragmentClassMap.put("library", LibraryFragment.class);
        specialRouteFragmentClassMap.put("publish", PublishFragment.class);
        specialRouteFragmentClassMap.put("publishes", PublishesFragment.class);
        specialRouteFragmentClassMap.put("following", FollowingFragment.class);
        specialRouteFragmentClassMap.put("rewards", RewardsFragment.class);
        specialRouteFragmentClassMap.put("subscription", FollowingFragment.class);
        specialRouteFragmentClassMap.put("subscriptions", FollowingFragment.class);
        specialRouteFragmentClassMap.put("wallet", WalletFragment.class);
        specialRouteFragmentClassMap.put("discover", FollowingFragment.class);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkSendToIntent(intent);
        checkUrlIntent(intent);
        checkNotificationOpenIntent(intent);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                for (ScreenOrientationListener listener : screenOrientationListeners) {
                    listener.onPortraitOrientationEntered();
                }
                break;
            case  Configuration.ORIENTATION_LANDSCAPE:
                for (ScreenOrientationListener listener : screenOrientationListeners) {
                    listener.onLandscapeOrientationEntered();
                }
                break;
        }
    }

    public void setActionBarTitle(@StringRes int stringResourceId) {
        ((TextView) findViewById(R.id.title)).setText(stringResourceId);
        findViewById(R.id.title).setVisibility(View.VISIBLE);
    }

    public void clearActionBarTitle() {
        ((TextView) findViewById(R.id.title)).setText(null);
        findViewById(R.id.title).setVisibility(View.GONE);
    }

    public void addScreenOrientationListener(ScreenOrientationListener listener) {
        if (!screenOrientationListeners.contains(listener)) {
            screenOrientationListeners.add(listener);
        }
    }

    public void removeScreenOrientationListener(ScreenOrientationListener listener) {
        screenOrientationListeners.remove(listener);
    }

    public void addDownloadActionListener(DownloadActionListener listener) {
        if (!downloadActionListeners.contains(listener)) {
            downloadActionListeners.add(listener);
        }
    }

    public void removeDownloadActionListener(DownloadActionListener listener) {
        downloadActionListeners.remove(listener);
    }

    public void addFilePickerListener(FilePickerListener listener) {
        if (!filePickerListeners.contains(listener)) {
            filePickerListeners.add(listener);
        }
    }

    public void removeFilePickerListener(FilePickerListener listener) {
        filePickerListeners.remove(listener);
    }

    public void addPIPModeListener(PIPModeListener listener) {
        if (!pipModeListeners.contains(listener)) {
            pipModeListeners.add(listener);
        }
    }

    public void removePIPModeListener(PIPModeListener listener) {
        pipModeListeners.remove(listener);
    }

    public void addCameraPermissionListener(CameraPermissionListener listener) {
        if (!cameraPermissionListeners.contains(listener)) {
            cameraPermissionListeners.add(listener);
        }
    }

    public void removeCameraPermissionListener(CameraPermissionListener listener) {
        cameraPermissionListeners.remove(listener);
    }

    public void addStoragePermissionListener(StoragePermissionListener listener) {
        if (!storagePermissionListeners.contains(listener)) {
            storagePermissionListeners.add(listener);
        }
    }

    public void removeStoragePermissionListener(StoragePermissionListener listener) {
        storagePermissionListeners.remove(listener);
    }

    public void addWalletBalanceListener(WalletBalanceListener listener) {
        if (!walletBalanceListeners.contains(listener)) {
            walletBalanceListeners.add(listener);
        }
    }

    public void removeWalletBalanceListener(WalletBalanceListener listener) {
        walletBalanceListeners.remove(listener);
    }

    public void hideWalletBalance() {
        findViewById(R.id.wallet_balance_container).setVisibility(View.GONE);
    }

    public void showWalletBalance() {
        findViewById(R.id.wallet_balance_container).setVisibility(View.VISIBLE);
    }

    public void addFetchChannelsListener(FetchChannelsListener listener) {
        if (!fetchChannelsListeners.contains(listener)) {
            fetchChannelsListeners.add(listener);
        }
    }
    public void removeFetchChannelsListener(FetchChannelsListener listener) {
        fetchChannelsListeners.remove(listener);
    }

    public void addFetchClaimsListener(FetchClaimsListener listener) {
        if (!fetchClaimsListeners.contains(listener)) {
            fetchClaimsListeners.add(listener);
        }
    }
    public void removeFetchClaimsListener(FetchClaimsListener listener) {
        fetchClaimsListeners.remove(listener);
    }

    public void openChannelClaim(Claim claim) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", !Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl());
        params.put("claim", getCachedClaimForUrl(claim.getPermanentUrl()));
        openFragment(ChannelFragment.class, true, params);
    }

    public void openChannelForm(Claim claim) {
        Map<String, Object> params = new HashMap<>();
        if (claim != null) {
            params.put("claim", claim);
        }
        openFragment(ChannelFormFragment.class, true, params);
    }

    public void navigateBackToMain() {
        getSupportFragmentManager().popBackStack();
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
        findViewById(R.id.title).setVisibility(View.GONE);
        findViewById(R.id.toolbar_balance_and_tools_layout).setVisibility(View.VISIBLE);
    }

    public void openPublishesOnSuccessfulPublish() {
        // close publish form
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    getSupportFragmentManager().popBackStack();
                    openFragment(PublishesFragment.class, true, null);
                } catch (IllegalStateException ex) {
                    // pass
                    try {
                        onBackPressed();
                    } catch (IllegalStateException iex) {
                        // if this fails on some devices. what's the solution?
                    }
                }
            }
        });
    }

    public void openPublishForm(Claim claim) {
        Map<String, Object> params = new HashMap<>();
        if (claim != null) {
            params.put("claim", claim);
        }
        openFragment(PublishFormFragment.class, true, params);
    }

    public void openChannelUrl(String url, String source) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        params.put("claim", getCachedClaimForUrl(url));
        if (!Helper.isNullOrEmpty(source)) {
            params.put("source", source);
        }
        openFragment(ChannelFragment.class, true, params);
    }
    public void openChannelUrl(String url) {
        openChannelUrl(url, null);
    }

    private Claim getCachedClaimForUrl(String url) {
        ClaimCacheKey key = new ClaimCacheKey();
        key.setUrl(url);
        return Lbry.claimCache.containsKey(key) ? Lbry.claimCache.get(key) : null;
    }

    public void setWunderbarValue(String value) {
//        EditText wunderbar = findViewById(R.id.wunderbar);
//        wunderbar.setText(value);
//        wunderbar.setSelection(0);
    }

    public void openAllContentFragmentWithTag(String tag) {
        Map<String, Object> params = new HashMap<>();
        params.put("singleTag", tag);
//        openFragment(AllContentFragment.class, true, NavMenuItem.ID_ITEM_ALL_CONTENT, params);
    }

    public void openFileUrl(String url) {
        openFileUrl(url, null);
    }
    public void openFileUrl(String url, String source) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        if (!Helper.isNullOrEmpty(source)) {
            params.put("source", source);
        }
        openFragment(FileViewFragment.class, true, params);
    }

    public void openPrivatePlaylist(OdyseeCollection collection) {
        openPrivatePlaylist(collection, null, -1);
    }

    public void openPrivatePlaylist(OdyseeCollection collection, Claim playlistItem, int playlistIndex) {
        Map<String, Object> params = new HashMap<>();
        params.put("collection", collection);
        if (playlistItem != null && playlistIndex > -1) {
            params.put("item", playlistItem);
            params.put("itemIndex", playlistIndex);
        }
        openFragment(FileViewFragment.class, true, params);
    }

    private void openSpecialUrl(String url, String source) {
        String specialPath = url.substring(8).toLowerCase();
        if (specialRouteFragmentClassMap.containsKey(specialPath)) {
            Map<String, Object> params = null;
            if (!Helper.isNullOrEmpty(source)) {
                params = new HashMap<>();
                params.put("source",  source);
            }

            if (specialPath.equalsIgnoreCase("rewards")) {
                openRewards(params);
            }
        }
    }

    public void openSendTo(String path) {
        Map<String, Object> params = new HashMap<>();
        params.put("directFilePath", path);
        openFragment(PublishFormFragment.class, true, params);
    }

    public void openFileClaim(Claim claim) {
        Map<String, Object> params = new HashMap<>();
        params.put("claimId", claim.getClaimId());
        params.put("url", !Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl());

        if (claim.getLivestreamUrl() != null) {
            params.put("livestreamUrl", claim.getLivestreamUrl());
        }
        params.put("claim", claim);
        openFragment(FileViewFragment.class, true, params);
    }

    /**
     *
     * @param params Can be null if you don't want to pass any parameters
     */
    public void openRewards(@Nullable Map<String, Object> params) {
        if (params != null && params.containsKey("source") ) {
            String sourceParam = (String) params.get("source");
            if (sourceParam != null && sourceParam.equals("notification")) {
                hideNotifications();
            }
        }
        openFragment(RewardsFragment.class, true, params);
    }

    private final FragmentManager.OnBackStackChangedListener backStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            FragmentManager manager = getSupportFragmentManager();
            if (manager != null) {
                Fragment currentFragment = getCurrentFragment();

            }
        }
    };

    public void showAppBar() {
        findViewById(R.id.appbar).setVisibility(View.VISIBLE);
    }

    private void renderPictureInPictureMode() {
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
        findViewById(R.id.miniplayer).setVisibility(View.GONE);
        findViewById(R.id.appbar).setVisibility(View.GONE);
        hideBottomNavigation();
        hideNotifications();
        hideActionBar();
        dismissActiveDialogs();

        View pipPlayerContainer = findViewById(R.id.pip_player_container);
        PlayerView pipPlayer = findViewById(R.id.pip_player);
        pipPlayer.setPlayer(playerManager.getCurrentPlayer());
        pipPlayer.setUseController(false);
        pipPlayerContainer.setVisibility(View.VISIBLE);
        playerReassigned = true;
    }

    private void dismissActiveDialogs() {
        for( Fragment fragment: getSupportFragmentManager().getFragments() ){
            if (fragment instanceof DialogFragment){
                ((DialogFragment) fragment).dismiss();
            }
        }
    }

    private void renderFullMode() {
        if (!inFullscreenMode) {
            showActionBar();
        } else {
            View v = findViewById(R.id.appbar);
            if (v != null) {
                v.setFitsSystemWindows(false);
            }
        }

        Fragment fragment = getCurrentFragment();
        boolean inMainView = currentDisplayFragment == null;
        boolean inFileView = fragment instanceof FileViewFragment;
        boolean inChannelView = fragment instanceof ChannelFragment;
        boolean inSearchView = fragment instanceof SearchFragment;

        findViewById(R.id.content_main).setVisibility(View.VISIBLE);

        findViewById(R.id.fragment_container_main_activity).setVisibility(inMainView ? View.VISIBLE : View.GONE);
        if (inMainView) {
            showBottomNavigation();
        }
        findViewById(R.id.appbar).setVisibility(inMainView || inSearchView ? View.VISIBLE : View.GONE);
        if (!inFileView && !inFullscreenMode && nowPlayingClaim != null) {
            findViewById(R.id.miniplayer).setVisibility(View.VISIBLE);
            setPlayerForMiniPlayerView();
        }

        View pipPlayerContainer = findViewById(R.id.pip_player_container);
        PlayerView pipPlayer = findViewById(R.id.pip_player);
        pipPlayer.setPlayer(null);
        pipPlayerContainer.setVisibility(View.GONE);
        playerReassigned = true;
    }

    private boolean isMiniPlayerVisible() {
        return findViewById(R.id.miniplayer).getVisibility() == View.VISIBLE;
    }

    private void setPlayerForMiniPlayerView() {
        PlayerView view = findViewById(R.id.global_now_playing_player_view);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            view.setPlayer(null);
            view.setPlayer(playerManager.getCurrentPlayer());
        }
    }

    @Override
    protected void onDestroy() {
        shuttingDown = true;
        unregisterReceivers();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (mediaSession != null && !isBackgroundPlaybackEnabled()) {
            mediaSession.release();
        }
        if (!isBackgroundPlaybackEnabled()) {
            playerNotificationManager.setPlayer(null);
            stopExoplayer();
            nowPlayingClaim = null;
            nowPlayingClaimUrl = null;
            nowPlayingClaimBitmap = null;
        }
        appStarted = false;

        super.onDestroy();
    }

    public static void stopExoplayer() {
        if (playerManager != null) {
            playerManager.stopPlayers();
            playerManager = null;
        }
        if (playerCache != null) {
            playerCache.release();
            playerCache = null;
        }
        videoIsTranscoded = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    public String getAuthToken() {
        AccountManager am = AccountManager.get(this);
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        if (odyseeAccount != null) {
            return am.peekAuthToken(odyseeAccount, "auth_token_type");
        }
        return null;
    }

    // Annotated as AnyThread because it is using an AsyncTask. Change it as needed when that was no longer the case
    @AnyThread
    public void updateWalletBalance() {
        if (isSignedIn()) {
            Activity a = this;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<WalletBalance> c = new WalletBalanceFetch(getAuthToken());
                    Future<WalletBalance> f = ((OdyseeApp) a.getApplication()).getExecutor().submit(c);
                    try {
                        WalletBalance balance = f.get();

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Lbry.walletBalance = balance;
                                for (WalletBalanceListener listener : walletBalanceListeners) {
                                    if (listener != null) {
                                        listener.onWalletBalanceUpdated(balance);
                                    }
                                }
                                sendBroadcast(new Intent(ACTION_WALLET_BALANCE_UPDATED));
                                ((TextView) findViewById(R.id.floating_balance_value)).setText(Helper.shortCurrencyFormat(Lbry.getTotalBalance()));
                            }
                        });
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        } else {
            Lbry.walletBalance = new WalletBalance();

            for (WalletBalanceListener listener : walletBalanceListeners) {
                if (listener != null) {
                    listener.onWalletBalanceUpdated(Lbry.walletBalance);
                }
            }
            sendBroadcast(new Intent(ACTION_WALLET_BALANCE_UPDATED));
            ((TextView) findViewById(R.id.floating_balance_value)).setText(Helper.shortCurrencyFormat(Lbry.getTotalBalance()));
        }
    }

    @SneakyThrows
    private void checkWebSocketClient() {
        if ((webSocketClient == null || webSocketClient.isClosed()) && !Helper.isNullOrEmpty(Lbryio.AUTH_TOKEN)) {
            webSocketClient = new WebSocketClient(new URI(String.format("%s%s", Lbryio.WS_CONNECTION_BASE_URL, Lbryio.AUTH_TOKEN))) {
                @Override
                public void onOpen(ServerHandshake handshakedata) { }

                @Override
                public void onMessage(String message) {
                    loadRemoteNotifications(false);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!shuttingDown) {
                        // attempt to re-establish the connection if the app isn't being closed
                        checkWebSocketClient();
                    }
                }

                @Override
                public void onError(Exception ex) { }

                protected void onSetSSLParameters(SSLParameters sslParameters) {
                    // don't call setEndpointIdentificationAlgorithm for API level < 24
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                    }
                }
            };
            webSocketClient.connect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isSignedIn()) {
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.action_home_menu);
        }

        accountManager.addOnAccountsUpdatedListener(this, null, true);

        // Change status bar text color depending on Night mode when app is running
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT) && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                //noinspection deprecation
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            int defaultNight = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NOTNIGHT) || (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_SYSTEM) && defaultNight == Configuration.UI_MODE_NIGHT_NO)) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().getWindowInsetsController().setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    //noinspection deprecation
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        purchasedChecker.checkPurchases();
        checkWebSocketClient();
        checkBottomNavigationHeight();
        updateMiniPlayerMargins(findViewById(R.id.bottom_navigation).getVisibility() == View.VISIBLE);
        enteringPIPMode = false;

        castHelper.addCastStateListener();
        checkNowPlaying();

        if (isSignedIn()) {
            loadRemoteNotifications(false);
        }

        scheduleWalletBalanceUpdate();

        if (pendingSourceTabId != 0) {
            if (pendingSourceTabId == SIGN_IN_SOURCE_NOTIFICATIONS) {
                showNotifications();
                pendingSourceTabId = 0;
                return;
            }
            if (pendingSourceTabId == SIGN_IN_SOURCE_PUBLISH) {
                showPublishFlow();
                pendingSourceTabId = 0;
                return;
            }

            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            bottomNavigation.setSelectedItemId(pendingSourceTabId);
            pendingSourceTabId = 0;
        }

        /**
         * Note initial flow steps before displaying user interface elements to interact with
         * 1. Initialise user install (initialiseUserInstall)
         * 2. Load custom blocking rules (loadCustomBlocking)
         * 3. Load initial categories (loadInitialCategories)
         */
        initialiseUserInstall();
        // checkPendingOpens();
    }

    public void displayCurrentlyPlayingVideo() {
        if (playerManager != null && playerManager.getCurrentPlayer().isPlaying() && !isMiniPlayerVisible()) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FILE_VIEW_TAG);
            if (fragment != null) {
                updateCurrentDisplayFragment(fragment);
                hideBottomNavigation();
                findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
            }
        }
    }

    private void checkBottomNavigationHeight() {
        if (bottomNavigationHeight == 0) {
            final BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            ViewTreeObserver viewTreeObserver = bottomNavigation.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int viewHeight = bottomNavigation.getHeight();
                        if (viewHeight != 0) {
                            bottomNavigationHeight = viewHeight;
                            bottomNavigation.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            updateMiniPlayerMargins(true);
                        }
                    }
                });
            }
        }
    }

    public boolean isFirstRunCompleted() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_FIRST_RUN_COMPLETED, false);
    }

    public boolean isFirstYouTubeSyncDone() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_FIRST_YOUTUBE_SYNC_DONE, false);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        castHelper.removeCastStateListener();
        if (!enteringPIPMode && !inPictureInPictureMode && playerManager != null && !isBackgroundPlaybackEnabled()) {
            playerManager.getCurrentPlayer().setPlayWhenReady(false);
        }
        super.onPause();
    }

    public static void suspendGlobalPlayer(Context context) {
        if (playerManager != null) {
            playerManager.getCurrentPlayer().setPlayWhenReady(false);
        }
        if (context instanceof MainActivity) {
            ((MainActivity) context).hideGlobalNowPlaying();
        }
    }
    public static void resumeGlobalPlayer(Context context) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).checkNowPlaying();
        }
    }

    private void toggleUrlSuggestions(boolean visible) {
        View container = findViewById(R.id.url_suggestions_container);
//        View closeIcon = findViewById(R.id.wunderbar_close);
        //EditText wunderbar = findViewById(R.id.wunderbar);
        //wunderbar.setPadding(0, 0, visible ? getScaledValue(36) : 0, 0);

        container.setVisibility(visible ? View.VISIBLE : View.GONE);
//        closeIcon.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public int getScaledValue(int value) {
        float scale = getResources().getDisplayMetrics().density;
        return Helper.getScaledValue(value, scale);
    }


    public boolean canShowUrlSuggestions() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(MainActivity.PREFERENCE_KEY_SHOW_URL_SUGGESTIONS, true);
    }
/*
    private void setupUriBar() {
        findViewById(R.id.wunderbar_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearWunderbarFocus(view);
            }
        });


        EditText wunderbar = findViewById(R.id.wunderbar);
        wunderbar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    hideNotifications();
                    findViewById(R.id.wunderbar_notifications).setVisibility(View.INVISIBLE);

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, 0);
                } else {
                    findViewById(R.id.wunderbar_notifications).setVisibility(View.VISIBLE);
                }

                if (canShowUrlSuggestions()) {
                    toggleUrlSuggestions(hasFocus);
                    if (hasFocus && Helper.isNullOrEmpty(Helper.getValue(((EditText) view).getText()))) {
                        displayUrlSuggestionsForNoInput();
                    }
                }
            }
        });

        wunderbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence != null && canShowUrlSuggestions()) {
                    handleUriInputChanged(charSequence.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        wunderbar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String input = Helper.getValue(wunderbar.getText());
                    boolean handled = false;
                    if (input.startsWith(LbryUri.PROTO_DEFAULT) && !input.equalsIgnoreCase(LbryUri.PROTO_DEFAULT)) {
                        try {
                            LbryUri uri = LbryUri.parse(input);
                            if (uri.isChannel()) {
                                openChannelUrl(uri.toString());
                                clearWunderbarFocus(wunderbar);
                                handled = true;
                            } else {
                                openFileUrl(uri.toString());
                                clearWunderbarFocus(wunderbar);
                                handled = true;
                            }
                        } catch (LbryUriException ex) {
                            // pass
                        }
                    }
                    if (!handled) {
                        // search
                        launchSearch(input);
                        clearWunderbarFocus(wunderbar);
                    }

                    return true;
                }

                return false;
            }
        });


        urlSuggestionListAdapter = new UrlSuggestionListAdapter(this);
        urlSuggestionListAdapter.setListener(new UrlSuggestionListAdapter.UrlSuggestionClickListener() {
            @Override
            public void onUrlSuggestionClicked(UrlSuggestion urlSuggestion) {
                switch (urlSuggestion.getType()) {
                    case UrlSuggestion.TYPE_CHANNEL:
                        // open channel page
                        if (urlSuggestion.getClaim() != null) {
                            openChannelClaim(urlSuggestion.getClaim());
                        } else {
                            openChannelUrl(urlSuggestion.getUri().toString());
                        }
                        break;
                    case UrlSuggestion.TYPE_FILE:
                        if (urlSuggestion.getClaim() != null) {
                            openFileClaim(urlSuggestion.getClaim());
                        } else {
                            openFileUrl(urlSuggestion.getUri().toString());
                        }
                        break;
                    case UrlSuggestion.TYPE_SEARCH:
                        launchSearch(urlSuggestion.getText());
                        break;
                    case UrlSuggestion.TYPE_TAG:
                        // open tag page
                        openAllContentFragmentWithTag(urlSuggestion.getText());
                        break;
                }
                clearWunderbarFocus(findViewById(R.id.wunderbar));
            }
        });

        RecyclerView urlSuggestionList = findViewById(R.id.url_suggestions);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        urlSuggestionList.setLayoutManager(llm);
        urlSuggestionList.setAdapter(urlSuggestionListAdapter);


    }

    public void clearWunderbarFocus(View view) {
        findViewById(R.id.wunderbar).clearFocus();
        findViewById(R.id.appbar).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public View getWunderbar() {
        return findViewById(R.id.wunderbar);
    }
*/
    private void launchSearch(String text) {
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof SearchFragment) {
            ((SearchFragment) currentFragment).search(text, 0);
        } else {
            try {
                SearchFragment fragment = SearchFragment.class.newInstance();
                fragment.setCurrentQuery(text);
                openFragment(fragment, true);
            } catch (Exception ex) {
                // pass
            }
        }
    }

    private void resolveUrlSuggestions(List<String> urls) {
        ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, null, new ResolveResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                if (findViewById(R.id.url_suggestions_container).getVisibility() == View.VISIBLE) {
                    for (int i = 0; i < claims.size(); i++) {
                        // build a simple url from the claim for matching
                        Claim claim = claims.get(i);
                        Claim actualClaim = claim;
                        boolean isRepost = false;
                        if (Claim.TYPE_REPOST.equalsIgnoreCase(claim.getValueType())) {
                            actualClaim = claim.getRepostedClaim();
                            isRepost = true;
                        }
                        if (Helper.isNullOrEmpty(claim.getName())) {
                            continue;
                        }

                        LbryUri simpleUrl = new LbryUri();
                        if (actualClaim.getName().startsWith("@") && !isRepost) {
                            // channel
                            simpleUrl.setChannelName(actualClaim.getName());
                        } else {
                            simpleUrl.setStreamName(claim.getName());
                        }

                        urlSuggestionListAdapter.setClaimForUrl(simpleUrl, actualClaim);
                    }
                    urlSuggestionListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Exception error) {

            }
        });
        task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void displayUrlSuggestionsForNoInput() {
        urlSuggestionListAdapter.clear();
        loadDefaultSuggestionsForBlankUrl();
    }

    private void handleUriInputChanged(String text) {
        // build the default suggestions
        urlSuggestionListAdapter.clear();
        if (Helper.isNullOrEmpty(text) || text.trim().equals("@")) {
            displayUrlSuggestionsForNoInput();
            return;
        }

        List<UrlSuggestion> defaultSuggestions = buildDefaultSuggestions(text);
        urlSuggestionListAdapter.addUrlSuggestions(defaultSuggestions);
        if (LbryUri.PROTO_DEFAULT.equalsIgnoreCase(text)) {
            return;
        }
/*
        LighthouseAutoCompleteTask task = new LighthouseAutoCompleteTask(text, null, new LighthouseAutoCompleteTask.AutoCompleteResultHandler() {
            @Override
            public void onSuccess(List<UrlSuggestion> suggestions) {
                String wunderBarText = Helper.getValue(((EditText) findViewById(R.id.wunderbar)).getText());
                if (wunderBarText.equalsIgnoreCase(text)) {
                    urlSuggestionListAdapter.addUrlSuggestions(suggestions);
                    List<String> urls = urlSuggestionListAdapter.getItemUrls();
                    resolveUrlSuggestions(urls);
                }
            }

            @Override
            public void onError(Exception error) {

            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

 */
    }

    private void loadDefaultSuggestionsForBlankUrl() {
        if (recentUrlHistory != null && recentUrlHistory.size() > 0) {
            urlSuggestionListAdapter.addUrlSuggestions(recentUrlHistory);
        }

        FetchRecentUrlHistoryTask task = new FetchRecentUrlHistoryTask(DatabaseHelper.getInstance(), new FetchRecentUrlHistoryTask.FetchRecentUrlHistoryHandler() {
            @Override
            public void onSuccess(List<UrlSuggestion> recentHistory) {
                List<UrlSuggestion> suggestions = new ArrayList<>(recentHistory);
                List<UrlSuggestion> lbrySuggestions = buildLbryUrlSuggestions();
                if (suggestions.size() < 10) {
                    for (int i = suggestions.size(), j = 0; i < 10 && j < lbrySuggestions.size(); i++, j++) {
                        suggestions.add(lbrySuggestions.get(j));
                    }
                } else if (suggestions.size() == 0) {
                    suggestions.addAll(lbrySuggestions);
                }

                for (UrlSuggestion suggestion : suggestions) {
                    suggestion.setUseTextAsDescription(true);
                }

                recentUrlHistory = new ArrayList<>(suggestions);
                urlSuggestionListAdapter.clear();
                urlSuggestionListAdapter.addUrlSuggestions(recentUrlHistory);
                List<String> urls = urlSuggestionListAdapter.getItemUrls();
                resolveUrlSuggestions(urls);
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private List<UrlSuggestion> buildLbryUrlSuggestions() {
        List<UrlSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new UrlSuggestion(
                UrlSuggestion.TYPE_FILE, "What is LBRY?", LbryUri.tryParse("lbry://what#19b9c243bea0c45175e6a6027911abbad53e983e")));
        suggestions.add(new UrlSuggestion(
                UrlSuggestion.TYPE_CHANNEL, "LBRYCast", LbryUri.tryParse("lbry://@lbrycast#4c29f8b013adea4d5cca1861fb2161d5089613ea")));
        suggestions.add(new UrlSuggestion(
                UrlSuggestion.TYPE_CHANNEL, "The LBRY Channel", LbryUri.tryParse("lbry://@lbry#3fda836a92faaceedfe398225fb9b2ee2ed1f01a")));
        for (UrlSuggestion suggestion : suggestions) {
            suggestion.setUseTextAsDescription(true);
        }
        return suggestions;
    }

    private List<UrlSuggestion> buildDefaultSuggestions(String text) {
        List<UrlSuggestion> suggestions = new ArrayList<UrlSuggestion>();

        if (LbryUri.PROTO_DEFAULT.equalsIgnoreCase(text)) {
            loadDefaultSuggestionsForBlankUrl();
            return recentUrlHistory != null ? recentUrlHistory : new ArrayList<>();
        }

        // First item is always search
        if (!text.startsWith(LbryUri.PROTO_DEFAULT)) {
            UrlSuggestion searchSuggestion = new UrlSuggestion(UrlSuggestion.TYPE_SEARCH, text);
            suggestions.add(searchSuggestion);
        }

        if (!text.matches(LbryUri.REGEX_INVALID_URI)) {
            boolean isUrlWithScheme = text.startsWith(LbryUri.PROTO_DEFAULT);
            boolean isChannel = text.startsWith("@");
            LbryUri uri = null;
            if (isUrlWithScheme && text.length() > 7) {
                try {
                    uri = LbryUri.parse(text);
                    isChannel = uri.isChannel();
                } catch (LbryUriException ex) {
                    // pass
                }
            }

            if (!isChannel) {
                if (uri == null) {
                    uri = new LbryUri();
                    uri.setStreamName(text);
                }
                UrlSuggestion fileSuggestion = new UrlSuggestion(UrlSuggestion.TYPE_FILE, text);
                fileSuggestion.setUri(uri);
                suggestions.add(fileSuggestion);
            }

            if (text.indexOf(' ') == -1) {
                // channels should not contain spaces
                if (isChannel) {
                    if (uri == null) {
                        uri = new LbryUri();
                        uri.setChannelName(text);
                    }
                    UrlSuggestion suggestion = new UrlSuggestion(UrlSuggestion.TYPE_CHANNEL, text);
                    suggestion.setUri(uri);
                    suggestions.add(suggestion);
                }
            }

            if (!isUrlWithScheme && !isChannel) {
                UrlSuggestion suggestion = new UrlSuggestion(UrlSuggestion.TYPE_TAG, text);
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    public void checkNowPlaying() {
        // Don't show the toolbar when returning from the Share Activity
        if (getSupportFragmentManager().findFragmentByTag(FILE_VIEW_TAG) == null) {
            findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        }
        if (nowPlayingClaim != null) {
            findViewById(R.id.miniplayer).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.global_now_playing_title)).setText(nowPlayingClaim.getTitle());
            ((TextView) findViewById(R.id.global_now_playing_channel_title)).setText(nowPlayingClaim.getPublisherTitle());
        }
        if (playerManager != null && !playerReassigned) {
            PlayerView playerView = findViewById(R.id.global_now_playing_player_view);
            playerView.setPlayer(null);
            playerView.setPlayer(playerManager.getCurrentPlayer());
            playerView.setUseController(false);
            playerReassigned = true;

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void hideGlobalNowPlaying() {
        findViewById(R.id.miniplayer).setVisibility(View.GONE);
    }

    public void enterFullScreenMode() {
        inFullscreenMode = true;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        findViewById(R.id.appbar).setFitsSystemWindows(false);

        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController windowInsetsController = decorView.getWindowInsetsController();
            windowInsetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            windowInsetsController.hide(WindowInsets.Type.systemBars());
        } else {
            //noinspection deprecation
            int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            //noinspection deprecation
            decorView.setSystemUiVisibility(flags);
        }
    }

    public void exitFullScreenMode() {
        View appBarMainContainer = findViewById(R.id.appbar);

        appBarMainContainer.setFitsSystemWindows(false);
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            getWindow().setDecorFitsSystemWindows(true);

            WindowInsetsController windowInsetsController = getWindow().getInsetsController();
            if (!darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT)) {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NOTNIGHT)
                     || (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_SYSTEM)
                          && (nightModeFlags == Configuration.UI_MODE_NIGHT_NO || nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED))) {
                    windowInsetsController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
            windowInsetsController.show(WindowInsets.Type.systemBars());
        } else {
            //noinspection deprecation
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE;

            if (!darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT) && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                //noinspection deprecation
                flags = flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            View decorView = getWindow().getDecorView();
            //noinspection deprecation
            decorView.setSystemUiVisibility(flags);
        }
        inFullscreenMode = false;
    }

    private void initKeyStore() {
        try {
            Lbry.KEYSTORE = Utils.initKeyStore(this);
        } catch (Exception ex) {
            // This shouldn't happen, but in case it does.
            Toast.makeText(this, "The keystore could not be initialized. The app requires a secure keystore to run properly.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkFirstRun() {
        if (!isFirstRunCompleted()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activityResultLauncher.launch(new Intent(MainActivity.this, FirstRunActivity.class));
                }
            });
            return;
        } else if (!isFirstYouTubeSyncDone()) {
            // if first run is already done, then check first YT sync instead
            checkFirstYouTubeSync();
        }

        fetchRewards();
    }

    private void checkFirstYouTubeSync() {
        if (!Lbryio.isSignedIn()) {
            return;
        }

        if (!isFirstYouTubeSyncDone()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, YouTubeSyncActivity.class));
                }
            });
        }
    }

    /**
     * Checks if an auth token is present and then sets it for Lbryio
     */
    private void loadAuthToken() {
        AccountManager am = AccountManager.get(this);
        Account account = Helper.getOdyseeAccount(am.getAccounts());
        if (account != null) {
            String authToken = am.peekAuthToken(account, "auth_token_type");
            if (!Helper.isNullOrEmpty(authToken)) {
                Lbryio.AUTH_TOKEN = authToken;
            }
        }
    }

    public void checkAndClaimNewAndroidReward() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean rewardClaimed = sp.getBoolean(PREFERENCE_KEY_INTERNAL_NEW_ANDROID_REWARD_CLAIMED, false);
        if (!rewardClaimed) {
            ClaimRewardTask task = new ClaimRewardTask(
                    Reward.TYPE_NEW_ANDROID,
                    null,
                    null,
                    new ClaimRewardTask.ClaimRewardHandler() {
                @Override
                public void onSuccess(double amountClaimed, String message) {
                    if (Helper.isNullOrEmpty(message)) {
                        message = getResources().getQuantityString(
                                R.plurals.claim_reward_message,
                                amountClaimed == 1 ? 1 : 2,
                                new DecimalFormat(Helper.LBC_CURRENCY_FORMAT_PATTERN).format(amountClaimed));
                    }
                    Snackbar.make(findViewById(R.id.content_main), message, Snackbar.LENGTH_LONG).show();
                    sp.edit().putBoolean(PREFERENCE_KEY_INTERNAL_NEW_ANDROID_REWARD_CLAIMED, true).apply();
                }

                @Override
                public void onError(Exception error) {
                    // pass. fail silently
                    sp.edit().putBoolean(PREFERENCE_KEY_INTERNAL_NEW_ANDROID_REWARD_CLAIMED, true).apply();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(this, "LBRYMediaSession", mediaButtonReceiver, null);
        MediaSessionConnector connector = new MediaSessionConnector(mediaSession);
        connector.setPlayer(playerManager.getCurrentPlayer());
        mediaSession.setActive(true);
    }

    public void initNotificationsPage() {
        findViewById(R.id.notification_list_empty_container).setVisibility(View.VISIBLE);

        RecyclerView notificationsList = findViewById(R.id.notifications_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        notificationsList.setLayoutManager(llm);
    }

    public void initPlaybackNotification() {
        if (isBackgroundPlaybackEnabled()) {
            playerNotificationManager.setPlayer(playerManager.getCurrentPlayer());
            if (mediaSession != null) {
                playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.actionMode = mode;
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT)) {
            //noinspection deprecation
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        actionMode.getMenuInflater().inflate(R.menu.menu_notification, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (R.id.action_delete == item.getItemId()) {
            if (notificationListAdapter != null && notificationListAdapter.getSelectedCount() > 0) {

                final List<LbryNotification> selectedNotifications = new ArrayList<>(notificationListAdapter.getSelectedItems());
                String message = getResources().getQuantityString(R.plurals.confirm_delete_notifications, selectedNotifications.size());
                AlertDialog.Builder builder = new AlertDialog.Builder(this).
                        setTitle(R.string.delete_selection).
                        setMessage(message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                handleDeleteSelectedNotifications(selectedNotifications);
                            }
                        }).setNegativeButton(R.string.no, null);
                builder.show();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (notificationListAdapter != null) {
            notificationListAdapter.clearSelectedItems();
            notificationListAdapter.setInSelectionMode(false);
            notificationListAdapter.notifyDataSetChanged();
        }

        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && darkModeAppSetting.equals(APP_SETTING_DARK_MODE_NIGHT)) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        this.actionMode = null;
    }

    @Override
    public void onEnterSelectionMode() {
        startSupportActionMode(this);
    }

    @Override
    public void onExitSelectionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onItemSelectionToggled() {
        if (actionMode != null) {
            actionMode.setTitle(notificationListAdapter != null ? String.valueOf(notificationListAdapter.getSelectedCount()) : "");
            actionMode.invalidate();
        }
    }

    private void handleDeleteSelectedNotifications(List<LbryNotification> notifications) {
        List<Long> remoteIds = new ArrayList<>();
        for (LbryNotification notification : notifications) {
            remoteIds.add(notification.getRemoteId());
        }
        (new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.deleteNotifications(notifications, db);
                } catch (Exception ex) {
                    // pass
                }
                return null;
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new NotificationDeleteTask(remoteIds).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if (notificationListAdapter != null) {
            notificationListAdapter.removeNotifications(notifications);
        }
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        Account odyseeAccount = Helper.getOdyseeAccount(accounts);
        if (odyseeAccount != null) {
            fetchOwnChannels();
            fetchOwnClaims();
            scheduleWalletBalanceUpdate();
            loadRemoteNotifications(false);
        } else {
            // Accounts can be removed from the system settings UI, so let's clear
            // database's tables if there is no Odysee account on the device
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                DatabaseHelper.clearNotifications(db);
                if (notificationListAdapter != null) {
                    notificationListAdapter.clearNotifications();
                }
                loadUnseenNotificationsCount();

                DatabaseHelper.clearViewHistory(db);
                DatabaseHelper.clearUrlHistory(db);
                DatabaseHelper.clearSubscriptions(db);
                db.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.action_home_menu);

            if (scheduledWalletUpdater != null)
                scheduledWalletUpdater.cancel(true);

            updateWalletBalance();
        }

        if (initialCategoriesLoaded && userInstallInitialised && customBlockingLoaded) {
            hideLaunchScreen();
        }
    }

    private void initialiseUserInstall() {
        if (!userInstallInitialised) {
            ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String installId = sp.getString(PREFERENCE_KEY_INSTALL_ID, null);
                    if (Helper.isNullOrEmpty(installId)) {
                        // no install_id found (first run didn't start the sdk successfully?)
                        installId = Lbry.generateId();
                        sp.edit().putString(PREFERENCE_KEY_INSTALL_ID, installId).apply();
                    }

                    Lbry.INSTALLATION_ID = installId;
                    //android.util.Log.d(TAG, String.format("InstallationID: %s", Lbry.INSTALLATION_ID));

                    try {
                        try {
                            Lbryio.authenticate(MainActivity.this);
                        } catch (AuthTokenInvalidatedException ex) {
                            // if this happens, attempt to authenticate again, so that we can obtain a new auth token
                            // this will also result in the user having to sign in again
                            Lbryio.authenticate(MainActivity.this);
                        }

                        if (Lbryio.currentUser == null) {
                            // display simplified startup error
                        }

                        Lbryio.newInstall(MainActivity.this);
                    } catch (Exception ex) {
                        // startup steps failed completely
                        // display startup error

                        return;
                    }

                    checkFirstRun();
                    scheduleWalletSyncTask();

                    if (!initialBlockedChannelsLoaded) {
                        loadBlockedChannels();
                    }

                    if (!customBlockingLoaded) {
                        loadCustomBlocking();
                        return;
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            hideLaunchScreen();
                        }
                    });
                }
            });
            userInstallInitialised = true;
            return;
        }

        // if the user install has already been initialised, proceed to load categories
        loadInitialCategories();
    }

    private void loadBlockedChannels() {
        if (!initialBlockedChannelsLoaded) {
            ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Lbryio.mutedChannels = new ArrayList<>(DatabaseHelper.getBlockedChannels(db));
                    initialBlockedChannelsLoaded = true;

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelMuting(true);
                        }
                    });
                }
            });
        }
    }

    private void loadCustomBlocking() {
        if (!customBlockingLoaded) {
            ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    final Map<String, List<CustomBlockRule>> rulesMap = new HashMap<>();
                    try {
                        // get the odysee locale
                        JSONObject localeObject = (JSONObject) Lbryio.parseResponse(Lbryio.call("locale", "get", null));
                        OdyseeLocale locale = new OdyseeLocale(
                                Helper.getJSONString("country", "", localeObject),
                                Helper.getJSONString("continent", "", localeObject),
                                Helper.getJSONBoolean("is_eu_member", false, localeObject)
                        );

                        // load custom blocking rules
                        JSONObject rulesObject = (JSONObject) Lbryio.parseResponse(Lbryio.call("geo", "blocked_list", null));
                        if (rulesObject.has(KEY_BLOCKING_LIVESTREAMS)) {
                            JSONObject baseObject = Helper.getJSONObject(KEY_BLOCKING_LIVESTREAMS, rulesObject);
                            Iterator<String> claimIds = baseObject.keys();
                            while (claimIds.hasNext()) {
                                String claimId = claimIds.next();
                                List<CustomBlockRule> rules = new ArrayList<>();
                                JSONObject baseRulesObject = Helper.getJSONObject(claimId, baseObject);

                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_COUNTRIES, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.country));
                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_CONTINENTS, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.continent));
                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_SPECIALS, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.special));

                                rulesMap.put(claimId, rules);
                            }
                        }

                        if (rulesObject.has(KEY_BLOCKING_VIDEOS)) {
                            JSONObject baseObject = Helper.getJSONObject(KEY_BLOCKING_VIDEOS, rulesObject);
                            Iterator<String> claimIds = baseObject.keys();
                            while (claimIds.hasNext()) {
                                String claimId = claimIds.next();
                                List<CustomBlockRule> rules = new ArrayList<>();
                                JSONObject baseRulesObject = Helper.getJSONObject(claimId, baseObject);

                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_COUNTRIES, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.country));
                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_CONTINENTS, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.continent));
                                rules.addAll(parseCustomBlockRules(Helper.getJSONArray(KEY_SCOPE_SPECIALS, baseRulesObject),
                                        CustomBlockRule.ContentType.livestreams, CustomBlockRule.Scope.special));

                                rulesMap.put(claimId, rules);
                            }
                        }

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                finishedLoadingCustomBlockRules(locale, rulesMap);
                            }
                        });
                    } catch (LbryioRequestException | LbryioResponseException ex) {
                        // failed to load, so we keep trying (or do we want to fail silently?)
                        loadCustomBlocking();
                    }
                }
            });
        }
    }

    private static List<CustomBlockRule> parseCustomBlockRules(
            JSONArray rulesList, CustomBlockRule.ContentType type, CustomBlockRule.Scope scope) {
        List<CustomBlockRule> rules = new ArrayList<>();
        if (rulesList != null) {
            for (int i = 0; i < rulesList.length(); i++) {
                try {
                    JSONObject ruleObject = rulesList.getJSONObject(i);
                    CustomBlockRule rule = new CustomBlockRule();
                    rule.setType(type);
                    rule.setScope(scope);
                    rule.setId(Helper.getJSONString("id", null, ruleObject));
                    rule.setTrigger(Helper.getJSONString("trigger", null, ruleObject));
                    rule.setMessage(Helper.getJSONString("message", null, ruleObject));
                    rule.setReason(Helper.getJSONString("reason", null, ruleObject));
                    rules.add(rule);
                } catch (JSONException ex) {
                    // pass
                }
            }
        }
        return rules;
    }

    private void finishedLoadingCustomBlockRules(OdyseeLocale locale, Map<String, List<CustomBlockRule>> rulesMap) {
        if (!Helper.isNullOrEmpty(locale.getCountry()) && !Helper.isNullOrEmpty(locale.getContinent())) {
            // make sure the country and continent at least loaded before setting the locale property
            odyseeLocale = locale;
        }

        customBlockingRulesMap = new HashMap<>(rulesMap);
        customBlockingLoaded = true;

        if (!initialCategoriesLoaded) {
            loadInitialCategories();
            return;
        }

        if (initialCategoriesLoaded && userInstallInitialised) {
            // if other initial loads are done, hide the launch screen
            hideLaunchScreen();
        }
    }

    private void loadInitialCategories() {
        if (!initialCategoriesLoaded) {
            ContentSources.loadCategories(((OdyseeApp) getApplication()).getExecutor(), new ContentSources.CategoriesLoadedHandler() {
                @Override
                public void onCategoriesLoaded(List<ContentSources.Category> categories) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (categories == null || categories.size() == 0) {
                                // display a startup error
                                Toast.makeText(MainActivity.this, R.string.startup_failed, Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            initialCategoriesLoaded = true;
                            displayDynamicCategories();
                        }
                    });
                }
            }, MainActivity.this);
        }
    }

    private void displayDynamicCategories() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_main_activity);
        if (fragment instanceof AllContentFragment) {
            ((AllContentFragment) fragment).displayDynamicCategories();
        }
        hideLaunchScreen();
    }

    private void hideLaunchScreen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Animate?
            View launchSplash = findViewById(R.id.launch_splash);
            if (launchSplash.getVisibility() == View.VISIBLE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                int width = launchSplash.getWidth();
                ValueAnimator valueAnimator = ValueAnimator.ofInt(width, 0);
                valueAnimator.setInterpolator(new DecelerateInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        launchSplash.getLayoutParams().width = (int) animation.getAnimatedValue();
                        launchSplash.requestLayout();
                    }
                });
                valueAnimator.setInterpolator(new DecelerateInterpolator());
                valueAnimator.setDuration(200);
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        launchSplash.clearAnimation();
                        launchSplash.setVisibility(View.GONE);
                        findViewById(R.id.root).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                    }
                });
                valueAnimator.start();
            }
        } else {
            readyToDraw = true;
        }
    }

    private class PlayerNotificationDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return nowPlayingClaim != null ? nowPlayingClaim.getTitle() : "";
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            if (nowPlayingClaimUrl != null || (nowPlayingClaim != null && !Helper.isNullOrEmpty(nowPlayingClaim.getCanonicalUrl()))) {
                Intent launchIntent = new Intent(MainActivity.this, MainActivity.class);
                launchIntent.setAction(Intent.ACTION_VIEW);
                if (!Helper.isNullOrEmpty(nowPlayingClaimUrl)) {
                    launchIntent.setData(Uri.parse(nowPlayingClaimUrl));
                } else {
                    launchIntent.setData(Uri.parse(nowPlayingClaim.getCanonicalUrl()));
                }
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return PendingIntent.getActivity(MainActivity.this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);
            }
            return null;
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return nowPlayingClaim != null && nowPlayingClaim.getSigningChannel() != null ? nowPlayingClaim.getSigningChannel().getTitleOrName() : null;
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            if (nowPlayingClaimBitmap == null &&
                    nowPlayingClaim != null &&
                    !Helper.isNullOrEmpty(nowPlayingClaim.getThumbnailUrl())) {
                Glide.with(getApplicationContext()).asBitmap().load(nowPlayingClaim.getThumbnailUrl(Utils.STREAM_THUMBNAIL_WIDTH, Utils.STREAM_THUMBNAIL_HEIGHT, Utils.STREAM_THUMBNAIL_Q)).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        nowPlayingClaimBitmap = resource;
                        callback.onBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
            }
            return nowPlayingClaimBitmap;
        }
    }

    private void scheduleWalletBalanceUpdate() {
        if (isSignedIn() && (scheduledWalletUpdater == null || scheduledWalletUpdater.isDone() || scheduledWalletUpdater.isCancelled())) {
            MainActivity a = this;
            try {
                scheduledWalletUpdater = ((OdyseeApp) a.getApplication()).getScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        if (!a.isBatterySaverMode()) {
                            updateWalletBalance();
                        }
                    }
                }, 0, 5, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scheduleWalletSyncTask() {
        if (!walletSyncScheduled) {
            ((OdyseeApp) getApplication()).getScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    syncWalletAndLoadPreferences();
                }
            }, 0, 5, TimeUnit.MINUTES);
            walletSyncScheduled = true;
        }
    }

    public void saveSharedUserState() {
        if (!userSyncEnabled()) {
            return;
        }
        SaveSharedUserStateTask saveTask = new SaveSharedUserStateTask(Lbryio.AUTH_TOKEN, this, new SaveSharedUserStateTask.SaveSharedUserStateHandler() {
            @Override
            public void onSuccess() {
                // push wallet sync changes
                pushCurrentWalletSync();
            }

            @Override
            public void onError(Exception error) {
                // pass
            }
        });
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void loadSharedUserState() {
        // load wallet preferences
        LoadSharedUserStateTask loadTask = new LoadSharedUserStateTask(MainActivity.this, new LoadSharedUserStateTask.LoadSharedUserStateHandler() {
            @Override
            public void onSuccess(List<Subscription> subscriptions, List<Tag> followedTags, List<LbryUri> mutedChannels,
                                  List<String> editedCollectionClaimIds) {
                if (subscriptions != null && subscriptions.size() > 0) {
                    // reload subscriptions if wallet fragment is FollowingFragment
                    //openNavFragments.get
                    MergeSubscriptionsTask mergeTask = new MergeSubscriptionsTask(
                            subscriptions,
                            initialSubscriptionMergeDone(),
                            MainActivity.this, new MergeSubscriptionsTask.MergeSubscriptionsHandler() {
                        @Override
                        public void onSuccess(List<Subscription> subscriptions, List<Subscription> diff) {
                            Lbryio.subscriptions = new ArrayList<>(subscriptions);
                            if (!diff.isEmpty()) {
                                saveSharedUserState();
                            }

                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            sp.edit().putBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_SUBSCRIPTION_MERGE_DONE, true).apply();
                            Lbryio.cacheResolvedSubscriptions.clear();

                            FollowingFragment f = (FollowingFragment) getSupportFragmentManager().findFragmentByTag("FOLLOWING");
                            if (f != null) {
                                f.fetchLoadedSubscriptions(true);
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            Log.e(TAG, String.format("merge subscriptions failed: %s", error.getMessage()), error);
                        }
                    });
                    mergeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                if (followedTags != null && followedTags.size() > 0) {
                    List<Tag> previousTags = new ArrayList<>(Lbry.followedTags);
                    Lbry.followedTags = new ArrayList<>(followedTags);

                    AllContentFragment f = (AllContentFragment) getSupportFragmentManager().findFragmentByTag("HOME");
                    if (f != null) {
                        if (!f.isSingleTagView() &&
                                f.getCurrentContentScope() == ContentScopeDialogFragment.ITEM_TAGS &&
                                !previousTags.equals(followedTags)) {
                            f.fetchClaimSearchContent(true);
                        }
                    }
                }

                if (mutedChannels != null && !mutedChannels.isEmpty()) {
                    if (!initialBlockedListLoaded()) {
                        // first time the blocked list is loaded, so we attempt to merge the entries
                        List<LbryUri> newBlockedChannels = new ArrayList<>(Lbryio.mutedChannels);
                        for (LbryUri uri : mutedChannels) {
                            if (!newBlockedChannels.contains(uri)) {
                                newBlockedChannels.add(uri);
                            }
                        }

                        Lbryio.mutedChannels = new ArrayList<>(newBlockedChannels);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        sp.edit().putBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_BLOCKED_LIST_LOADED, true).apply();
                    } else {
                        // replace the blocked channels list entirely
                        Lbryio.mutedChannels = new ArrayList<>(mutedChannels);
                    }
                }

                if (!initialCollectionsLoaded()) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    sp.edit().putBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_COLLECTIONS_LOADED, true).apply();
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, String.format("load shared user state failed: %s", error != null ? error.getMessage() : "no error message"), error);
            }
        }, Lbryio.AUTH_TOKEN);
        loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void pushCurrentWalletSync() {
        String password = getSecureValue(SECURE_VALUE_KEY_SAVED_PASSWORD, this, Lbry.KEYSTORE);
        SyncApplyTask fetchTask = new SyncApplyTask(true, password, new DefaultSyncTaskHandler() {
            @Override
            public void onSyncApplySuccess(String hash, String data) {
                SyncSetTask setTask = new SyncSetTask(Lbryio.lastRemoteHash, hash, data, null);
                setTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            @Override
            public void onSyncApplyError(Exception error) { }
        });
        fetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean userSyncEnabled() {
        // For Odysee, wallet sync is always enabled, so just check that the user is signed in
        return Lbryio.isSignedIn();
    }

    public void syncSet(String hash, String data) {
        if (syncSetTask == null || syncSetTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncSetTask = new SyncSetTask(Lbryio.lastRemoteHash, hash, data, new DefaultSyncTaskHandler() {
                @Override
                public void onSyncSetSuccess(String hash) {
                    Lbryio.lastRemoteHash = hash;
                    Lbryio.lastWalletSync = new WalletSync(hash, data);

                    if (!pendingSyncSetQueue.isEmpty()) {
                        fullSyncInProgress = true;
                        WalletSync nextSync = pendingSyncSetQueue.remove(0);
                        syncSet(nextSync.getHash(), nextSync.getData());
                    } else if (queuedSyncCount > 0) {
                        queuedSyncCount--;
                        syncApplyAndSet();
                    }

                    fullSyncInProgress = false;
                }
                @Override
                public void onSyncSetError(Exception error) {
                    // log app exceptions
                    if (!pendingSyncSetQueue.isEmpty()) {
                        WalletSync nextSync = pendingSyncSetQueue.remove(0);
                        syncSet(nextSync.getHash(), nextSync.getData());
                    } else if (queuedSyncCount > 0) {
                        queuedSyncCount--;
                        syncApplyAndSet();
                    }

                    fullSyncInProgress = false;
                }
            });
            syncSetTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            WalletSync pending = new WalletSync(hash, data);
            pendingSyncSetQueue.add(pending);
        }
    }

    public void syncApplyAndSet() {
        fullSyncInProgress = true;
        String password = getSecureValue(SECURE_VALUE_KEY_SAVED_PASSWORD, this, Lbry.KEYSTORE);
        SyncApplyTask fetchTask = new SyncApplyTask(true, password, new DefaultSyncTaskHandler() {
            @Override
            public void onSyncApplySuccess(String hash, String data) {
                if (!hash.equalsIgnoreCase(Lbryio.lastRemoteHash)) {
                    syncSet(hash, data);
                } else {
                    fullSyncInProgress = false;
                    queuedSyncCount = 0;
                }
            }
            @Override
            public void onSyncApplyError(Exception error) {
                fullSyncInProgress = false;
                if (queuedSyncCount > 0) {
                    queuedSyncCount--;
                    syncApplyAndSet();
                }
            }
        });
        fetchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setExpandedStatePreferenceScheduledClaims(boolean forceExpanded) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean("com.odysee.app.force_expanded_state_scheduled_claims_list", forceExpanded).apply();
    }

    public boolean getExpandedStatePreferenceScheduledClaims() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean("com.odysee.app.force_expanded_state_scheduled_claims_list", true);
    }

    private byte[] rsaEncrypt(byte[] secret, KeyStore keyStore) throws Exception {
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        } else {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            privateKey = privateKeyEntry.getPrivateKey();
            publicKey = privateKeyEntry.getCertificate().getPublicKey();
        }

        if (publicKey == null) {
            throw new Exception("Could not obtain public key for encryption.");
        }

        // Encrypt the text
        Cipher inputCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        inputCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        return outputStream.toByteArray();
    }

    private static byte[] rsaDecrypt(byte[] encrypted, KeyStore keyStore) throws Exception {
        PrivateKey privateKey = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        } else {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            privateKey = privateKeyEntry.getPrivateKey();
        }

        if (privateKey == null) {
            throw new Exception("Could not obtain private key for decryption");
        }

        Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        output.init(Cipher.DECRYPT_MODE, privateKey);
        CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<Byte>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }
    private String generateSecretKey(KeyStore keyStore) throws Exception {
        byte[] key = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(key);

        byte[] encryptedKey = rsaEncrypt(key, keyStore);
        String base64Encrypted = Base64.encodeToString(encryptedKey, Base64.DEFAULT);

        SharedPreferences pref = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("key", base64Encrypted);
        editor.commit();

        return base64Encrypted;
    }

    private Key getSecretKey(KeyStore keyStore) throws Exception{
        SharedPreferences pref = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String base64Key = pref.getString("key", null);
        if (base64Key == null || base64Key.trim().length() == 0) {
            base64Key = generateSecretKey(keyStore);
        }
        return new SecretKeySpec(rsaDecrypt(Base64.decode(base64Key, Base64.DEFAULT), keyStore), "AES");
    }

    public byte[] decrypt(byte[] encrypted, Context context, KeyStore keyStore) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
        c.init(Cipher.DECRYPT_MODE, getSecretKey(keyStore));
        return c.doFinal(encrypted);
    }


    public String getSecureValue(String key, Context context, KeyStore keyStore) {
        try {
            SharedPreferences pref = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            String encryptedValue = pref.getString(key, null);

            if (encryptedValue == null || encryptedValue.trim().length() == 0) {
                return null;
            }

            byte[] decoded = Base64.decode(encryptedValue, Base64.DEFAULT);
            return new String(decrypt(decoded, context, keyStore), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Log.e(TAG, "utils - Could not retrieve a secure value", ex);
        }

        return null;
    }

    public void syncWalletAndLoadPreferences() {
        if (!userSyncEnabled()) {
            return;
        }
        if (fullSyncInProgress) {
            queuedSyncCount++;
        }

        fullSyncInProgress = true;
        String password = getSecureValue(SECURE_VALUE_KEY_SAVED_PASSWORD, this, Lbry.KEYSTORE);
        SyncGetTask task = new SyncGetTask(password, true, null, new DefaultSyncTaskHandler() {
            @Override
            public void onSyncGetSuccess(WalletSync walletSync) {
                Lbryio.lastWalletSync = walletSync;
                Lbryio.lastRemoteHash = walletSync.getHash();
                loadSharedUserState();
            }

            @Override
            public void onSyncGetWalletNotFound() {
                // pass. This actually shouldn't happen at this point.
                // But if it does, send what we have
                if (Lbryio.isSignedIn()) {
                    syncApplyAndSet();
                }
            }

            @Override
            public void onSyncGetError(Exception error) {
                // pass
                Log.e(TAG, String.format("sync get failed: %s", error != null ? error.getMessage() : "no error message"), error);

                fullSyncInProgress = false;
                if (queuedSyncCount > 0) {
                    queuedSyncCount--;
                    syncApplyAndSet();
                }
            }

            @Override
            public void onSyncApplySuccess(String hash, String data) {
                if (!hash.equalsIgnoreCase(Lbryio.lastRemoteHash)) {
                    syncSet(hash, data);
                } else {
                    fullSyncInProgress = false;
                    queuedSyncCount = 0;
                }

                loadSharedUserState();
            }

            @Override
            public void onSyncApplyError(Exception error) {
                // pass
                Log.e(TAG, String.format("sync apply failed: %s", error != null ? error.getMessage() : "no error message"), error);
                fullSyncInProgress = false;
                if (queuedSyncCount > 0) {
                    queuedSyncCount--;
                    syncApplyAndSet();
                }
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void registerUAReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SignInActivity.ACTION_USER_FINISHED_SIGN_IN);
        uaReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (SignInActivity.ACTION_USER_FINISHED_SIGN_IN.equalsIgnoreCase(action)) {
                    handleUserFinishedSignIn(intent);
                }
            }
            private void handleUserFinishedSignIn(Intent intent) {
                int sourceTabId = intent.getIntExtra("sourceTabId", 0);
                if (sourceTabId != 0) {
                    pendingSourceTabId = sourceTabId;
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(uaReceiver, intentFilter);
    }

    private void registerRequestsReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_AUTH_TOKEN_GENERATED);
        intentFilter.addAction(ACTION_USER_SIGN_IN_SUCCESS);
        intentFilter.addAction(ACTION_OPEN_ALL_CONTENT_TAG);
        intentFilter.addAction(ACTION_OPEN_CHANNEL_URL);
        intentFilter.addAction(ACTION_OPEN_WALLET_PAGE);
        intentFilter.addAction(ACTION_OPEN_REWARDS_PAGE);
        intentFilter.addAction(ACTION_PUBLISH_SUCCESSFUL);
        intentFilter.addAction(ACTION_SAVE_SHARED_USER_STATE);
        intentFilter.addAction(LbrynetMessagingService.ACTION_NOTIFICATION_RECEIVED);
        requestsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_AUTH_TOKEN_GENERATED.equalsIgnoreCase(action)) {
                    handleAuthTokenGenerated(intent);
                } else if (ACTION_OPEN_ALL_CONTENT_TAG.equalsIgnoreCase(action)) {
                    handleOpenContentTag(intent);
                } else if (ACTION_OPEN_CHANNEL_URL.equalsIgnoreCase(action)) {
                    handleOpenChannelUrl(intent);
                } else if (ACTION_SAVE_SHARED_USER_STATE.equalsIgnoreCase(action)) {
                    saveSharedUserState();
                } else if (ACTION_PUBLISH_SUCCESSFUL.equalsIgnoreCase(action)) {
                    openPublishesOnSuccessfulPublish();
                } else if (LbrynetMessagingService.ACTION_NOTIFICATION_RECEIVED.equalsIgnoreCase(action)) {
                    handleNotificationReceived(intent);
                }
            }

            private void handleNotificationReceived(Intent intent) {
                loadRemoteNotifications(false);
            }

            private void handleAuthTokenGenerated(Intent intent) {
            }

            private void handleOpenContentTag(Intent intent) {
                String tag = intent.getStringExtra("tag");
                if (!Helper.isNullOrEmpty(tag)) {
                }
            }
            private void handleOpenChannelUrl(Intent intent) {
            }
        };
        registerReceiver(requestsReceiver, intentFilter);
    }

    public void showMessage(String message) {
        showMessage(message, null, null);
    }
    public void showMessage(String message, String actionText, View.OnClickListener actionListener) {
        Snackbar snackbar = getSnackbar(message);
        if (!Helper.isNullOrEmpty(actionText) && actionListener != null) {
            snackbar.setAction(actionText, actionListener);
        }
        snackbar.show();
    }
    public Snackbar getSnackbar(String message) {
        return Snackbar.make(findViewById(R.id.content_main), message, Snackbar.LENGTH_LONG);
    }
    public void showStreamStoppedMessage() {
        View view = findViewById(R.id.content_main);
        Snackbar snackbar = Snackbar.make(view, R.string.stream_stopped_went_to_home_reason, Snackbar.LENGTH_LONG);
        TextView snackbarText = snackbar.getView().findViewById(R.id.snackbar_text);
        snackbarText.setMaxLines(Integer.MAX_VALUE);
        snackbar.show();
    }
    public void showError(String message) {
        getSnackbar(message).setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }

    public void showNotifications() {
        findViewById(R.id.content_main_container).setVisibility(View.GONE);
        findViewById(R.id.notifications_container).setVisibility(View.VISIBLE);
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);

        hideBottomNavigation();
        updateMiniPlayerMargins(false);
        ((ImageView) findViewById(R.id.notifications_toggle_icon)).setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
        if (isSignedIn()) {
            if (remoteNotifcationsLastLoaded == null ||
                    (System.currentTimeMillis() - remoteNotifcationsLastLoaded.getTime() > REMOTE_NOTIFICATION_REFRESH_TTL)) {
                loadRemoteNotifications(false);
            }

            if (notificationListAdapter != null) {
                markNotificationsSeen();
            }
        } else {
            loadLocalNotifications(); // If there is no signed in user, this will just show the "no notifications" UI
        }
    }

    public void hideNotifications() {
        hideNotifications(true);
    }
    public void hideNotifications(boolean hideSingleContentView) {
        ((ImageView) findViewById(R.id.notifications_toggle_icon)).setColorFilter(ContextCompat.getColor(this, R.color.actionBarForeground));
        findViewById(R.id.content_main_container).setVisibility(View.GONE);
        findViewById(R.id.notifications_container).setVisibility(View.GONE);
        if (!isInPictureInPictureMode() && hideSingleContentView) {
            findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
            showBottomNavigation();
            updateMiniPlayerMargins(true);
        }
    }

    private void markNotificationsSeen() {
        List<LbryNotification> all = notificationListAdapter != null ? notificationListAdapter.getItems() : null;
        if (all != null) {
            List<Long> unseenIds = new ArrayList<>();
            for (LbryNotification notification : all) {
                if (!notification.isSeen() && notification.getRemoteId() > 0) {
                    unseenIds.add(notification.getRemoteId());
                }
            }
            if (!unseenIds.isEmpty()) {
                AccountManager am = AccountManager.get(this);
                Map<String, String> options = new HashMap<>();
                options.put("notification_ids", Helper.joinL(unseenIds, ","));
                options.put("is_seen", "true");

                if (am != null) {
                    String at = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    if (at != null)
                        options.put("auth_token", at);
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    Supplier<Boolean> task = new NotificationUpdateSupplier(options);
                    CompletableFuture<Boolean> cf = CompletableFuture.supplyAsync(task, ((OdyseeApp) getApplication()).getExecutor());
                    cf.thenAcceptAsync(result -> {
                        if (result) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            DatabaseHelper.markNotificationsSeen(db);
                            loadUnseenNotificationsCount();
                        }
                    }, ((OdyseeApp) getApplication()).getExecutor());
                } else {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Lbryio.call("notification", "edit", options, null);
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                DatabaseHelper.markNotificationsSeen(db);
                                db.close();
                                loadUnseenNotificationsCount();
                            } catch (LbryioRequestException | LbryioResponseException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
            }
        }
    }

    public void navigateBackToNotifications() {
        showNotifications();
    }

    public void showBottomNavigation() {
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
    }

    public void hideBottomNavigation() {
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.url_suggestions_container).getVisibility() == View.VISIBLE) {
//            clearWunderbarFocus(findViewById(R.id.wunderbar));
            return;
        }
        if (findViewById(R.id.notifications_container).getVisibility() == View.VISIBLE) {
            hideNotifications();
            return;
        }

        if (backPressInterceptor != null && backPressInterceptor.onBackPressed()) {
            return;
        }

        // check fragment and nav history
        FragmentManager manager = getSupportFragmentManager();
        int backCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backCount > 0) {
            // we can pop the stack
            manager.popBackStack();

            if (backCount == 1) { // It was 1 before popping
                if (isSearchUIActive()) {
                    findViewById(R.id.fragment_container_search).setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
                    hideBottomNavigation();
                } else {
                    findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
                    showBottomNavigation();
                }
                findViewById(R.id.title).setVisibility(View.GONE);
                findViewById(R.id.toolbar_balance_and_tools_layout).setVisibility(View.VISIBLE);

                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    showActionBar();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
            }
        } else if (!enterPIPMode()) {
            // we're at the top of the stack
            if (isSearchUIActive()) {
                // Close Search UI
                Fragment fragmentSearch = getSupportFragmentManager().findFragmentByTag("SEARCH");
                if (fragmentSearch != null) {
                    getSupportFragmentManager().beginTransaction().remove(fragmentSearch).commit();
                }
                switchToolbarForSearch(false);
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);
                findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
            } else {
                moveTaskToBack(true);
                checkNowPlaying();
            }
        }
    }

    private void switchToolbarForSearch(boolean showSearch) {
        if (showSearch) {
            findViewById(R.id.brand).setVisibility(View.GONE);
            hideWalletBalance();
            findViewById(R.id.upload_button).setVisibility(View.GONE);
            findViewById(R.id.profile_button).setVisibility(View.GONE);
            findViewById(R.id.wunderbar_notifications).setVisibility(View.GONE);
            findViewById(R.id.search_query_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.search_close_button).setVisibility(View.VISIBLE);
            switchClearViewHistoryButton(false);
        } else {
            EditText queryTextView = findViewById(R.id.search_query_text);
            queryTextView.setText("");
            findViewById(R.id.search_query_layout).setVisibility(View.GONE);
            findViewById(R.id.search_close_button).setVisibility(View.GONE);
            findViewById(R.id.brand).setVisibility(View.VISIBLE);
            showWalletBalance();
            findViewById(R.id.upload_button).setVisibility(View.VISIBLE);
            findViewById(R.id.profile_button).setVisibility(View.VISIBLE);
            findViewById(R.id.wunderbar_notifications).setVisibility(View.VISIBLE);

            if (((BottomNavigationView) findViewById(R.id.bottom_navigation)).getSelectedItemId() == R.id.action_library_menu) {
                switchClearViewHistoryButton(true);
            } else {
                switchClearViewHistoryButton(false);
            }
        }
    }

    public void switchClearViewHistoryButton(boolean makeItVisible) {
        if (makeItVisible) {
            findViewById(R.id.clear_all_library_button).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.clear_all_library_button).setVisibility(View.GONE);
        }
    }
    private boolean isSearchUIActive() {
        return getSupportFragmentManager().findFragmentByTag("SEARCH") != null;
    }

    public void signOutUser() {
        Lbryio.currentUser = null;
        Lbryio.AUTH_TOKEN = "";

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            accountManager.removeAccountExplicitly(Helper.getOdyseeAccount(accountManager.getAccounts()));
        } else {
            // removeAccount() was deprecated on API Level 22. Any device running that version will take the other branch
            // on this conditional
            //noinspection deprecation
            accountManager.removeAccount(Helper.getOdyseeAccount(accountManager.getAccounts()), null, null);
        }

        ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    DatabaseHelper.clearLocalUserData(db);
                }
            }
        });
    }

    public boolean isSignedIn() {
        AccountManager am = AccountManager.get(this);
        return Helper.getOdyseeAccount(am.getAccounts()) != null;
    }

    public void simpleSignIn(int sourceTabId) {
        clearPlayingPlayer();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra("sourceTabId", sourceTabId);
        startActivity(intent);
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }


    public void driveUserSignIn() {
        if (playerManager != null && playerManager.getCurrentPlayer().isPlaying()) {
            playerManager.getCurrentPlayer().pause();
        }
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    public void walletSyncSignIn() {
        Intent intent = new Intent(this, VerificationActivity.class);
        intent.putExtra("flow", VerificationActivity.VERIFICATION_FLOW_WALLET);
        startActivityForResult(intent, REQUEST_WALLET_SYNC_SIGN_IN);
    }

    public void rewardsSignIn() {
        Intent intent = new Intent(this, VerificationActivity.class);
        intent.putExtra("flow", VerificationActivity.VERIFICATION_FLOW_REWARDS);
        startActivityForResult(intent, REQUEST_REWARDS_VERIFY_SIGN_IN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    for (StoragePermissionListener listener : storagePermissionListeners) {
                        listener.onStoragePermissionGranted();
                    }
                } else {
                    for (StoragePermissionListener listener : storagePermissionListeners) {
                        listener.onStoragePermissionRefused();
                    }
                }
                startingPermissionRequest = false;
                break;

            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    for (CameraPermissionListener listener : cameraPermissionListeners) {
                        listener.onCameraPermissionGranted();
                    }
                } else {
                    for (CameraPermissionListener listener : cameraPermissionListeners) {
                        listener.onCameraPermissionRefused();
                    }
                }
                startingPermissionRequest = false;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_PICKER) {
            startingFilePickerActivity = false;
            if (resultCode == RESULT_OK && data != null) {
                Uri fileUri = data.getData();
                String filePath = Helper.getRealPathFromURI_API19(this, fileUri);
                for (FilePickerListener listener : filePickerListeners) {
                    listener.onFilePicked(filePath);
                }
            } else {
                for (FilePickerListener listener : filePickerListeners) {
                    listener.onFilePickerCancelled();
                }
            }
        } else if (requestCode == REQUEST_SIMPLE_SIGN_IN || requestCode == REQUEST_WALLET_SYNC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // user signed in
                if (requestCode == REQUEST_WALLET_SYNC_SIGN_IN) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    sp.edit().putBoolean(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_SYNC_ENABLED, true).apply();

                    WalletFragment f = (WalletFragment) getSupportFragmentManager().findFragmentByTag("WALLET");

                    if (f != null) {
                        f.onWalletSyncEnabled();
                    }

                    scheduleWalletSyncTask();
                }
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE || requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                PublishFragment publishFragment = null;

                PublishFragment f = (PublishFragment) getSupportFragmentManager().findFragmentByTag("PUBLISH");

                if (f != null) {
                    publishFragment = (PublishFragment) f;
                }

                Map<String, Object> params = new HashMap<>();
                params.put("directFilePath", cameraOutputFilename);
                if (publishFragment != null) {
                    params.put("suggestedUrl", publishFragment.getSuggestedPublishUrl());
                }
                openFragment(PublishFormFragment.class, true, params);
            }
            cameraOutputFilename = null;
        }
    }

    private String getAppInternalStorageDir() {
        File[] dirs = getExternalFilesDirs(null);
        return dirs[0].getAbsolutePath();

    }
    public void requestVideoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            String outputPath = String.format("%s/record", getAppInternalStorageDir());
            File dir = new File(outputPath);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }

            cameraOutputFilename = String.format("%s/VID_%s.mp4", outputPath, Helper.FILESTAMP_FORMAT.format(new Date()));
            Uri outputUri = FileProvider.getUriForFile(this, String.format("%s.fileprovider", getPackageName()), new File(cameraOutputFilename));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
            return;
        }

        showError(getString(R.string.cannot_capture_video));
    }

    public void requestTakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            String outputPath = String.format("%s/photos", getAppInternalStorageDir());
            File dir = new File(outputPath);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }

            cameraOutputFilename = String.format("%s/IMG_%s.jpg", outputPath, Helper.FILESTAMP_FORMAT.format(new Date()));
            Uri outputUri = FileProvider.getUriForFile(this, String.format("%s.fileprovider", getPackageName()), new File(cameraOutputFilename));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            return;
        }

        showError(getString(R.string.cannot_take_photo));
    }

    private Fragment getCurrentFragment() {
        if (currentDisplayFragment != null) {
            return currentDisplayFragment;
        }

        return getSupportFragmentManager().findFragmentById(R.id.content_main);
    }

    public void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
    public void showActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }
    private void renderStartupFailed(List<StartupStage> startupStages) {
        ListView listView = findViewById(R.id.startup_stage_error_listview);
        StartupStageAdapter adapter = new StartupStageAdapter(this, startupStages);
        listView.setAdapter(adapter);

        // Add 1 pixel to listview height
        int listHeight = Math.round(getResources().getDisplayMetrics().density);

        for (int i = 0; i < startupStages.size(); i++) {
            View item = adapter.getView(i, null, listView);
            item.measure(0, 0);
            listHeight += item.getMeasuredHeight();
        }

        // Properly set listview height by adding all seven items and the divider heights
        // and the additional 1 pixel so no vertical scroll bar is shown
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = listHeight + (listView.getCount() + 1) * listView.getDividerHeight();
        listView.setLayoutParams(params);
        listView.invalidate();
        listView.requestLayout();

        findViewById(R.id.splash_view_loading_container).setVisibility(View.GONE);
        findViewById(R.id.splash_view_error_container).setVisibility(View.VISIBLE);
    }

    @SuppressLint("StaticFieldLeak")
    private void startup() {
        final Context context = this;
        Lbry.startupInit();

        // perform some tasks before launching
        (new AsyncTask<Void, Void, Boolean>() {
            private final List<StartupStage> startupStages = new ArrayList<>(7);

            private void initStartupStages() {
                startupStages.add(new StartupStage(STARTUP_STAGE_INSTALL_ID_LOADED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_KNOWN_TAGS_LOADED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_EXCHANGE_RATE_LOADED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_USER_AUTHENTICATED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_NEW_INSTALL_DONE, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_LOADED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_BLOCK_LIST_LOADED, false));
                startupStages.add(new StartupStage(STARTUP_STAGE_FILTER_LIST_LOADED, false));
            }
            protected void onPreExecute() {
                hideActionBar();
//                findViewById(R.id.splash_view).setVisibility(View.VISIBLE);
                LbryAnalytics.setCurrentScreen(MainActivity.this, "Splash", "Splash");
                initStartupStages();
            }
            protected Boolean doInBackground(Void... params) {
                BufferedReader reader = null;
                try {
                    // Load the installation id from the file system
                    String lbrynetDir = String.format("%s/%s", getAppInternalStorageDir(), "lbrynet");
                    String installIdPath = String.format("%s/install_id", lbrynetDir);
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(installIdPath)));
                    String installId = reader.readLine();
                    if (Helper.isNullOrEmpty(installId)) {
                        // no install_id found (first run didn't start the sdk successfully?)
                        startupStages.set(STARTUP_STAGE_INSTALL_ID_LOADED - 1, new StartupStage(STARTUP_STAGE_INSTALL_ID_LOADED, false));
                        return false;
                    }

                    Lbry.INSTALLATION_ID = installId;
                    startupStages.set(STARTUP_STAGE_INSTALL_ID_LOADED - 1, new StartupStage(STARTUP_STAGE_INSTALL_ID_LOADED, true));

                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    List<Tag> fetchedTags = DatabaseHelper.getTags(db);
                    Lbry.knownTags = Helper.mergeKnownTags(fetchedTags);
                    Collections.sort(Lbry.knownTags, new Tag());
                    Lbry.followedTags = Helper.filterFollowedTags(Lbry.knownTags);
                    startupStages.set(STARTUP_STAGE_KNOWN_TAGS_LOADED - 1, new StartupStage(STARTUP_STAGE_KNOWN_TAGS_LOADED, true));

                    // load the exchange rate
                    Lbryio.loadExchangeRate();
                    if (Lbryio.LBCUSDRate == 0) {
                        return false;
                    }
                    startupStages.set(STARTUP_STAGE_EXCHANGE_RATE_LOADED - 1, new StartupStage(STARTUP_STAGE_EXCHANGE_RATE_LOADED, true));

                    try {
                        Lbryio.authenticate(context);
                    } catch (AuthTokenInvalidatedException ex) {
                        // if this happens, attempt to authenticate again, so that we can obtain a new auth token
                        // this will also result in the user having to sign in again
                        Lbryio.authenticate(context);
                    }
                    if (Lbryio.currentUser == null) {
                        throw new Exception("Did not retrieve authenticated user.");
                    }
                    startupStages.set(STARTUP_STAGE_USER_AUTHENTICATED - 1, new StartupStage(STARTUP_STAGE_USER_AUTHENTICATED, true));

                    Lbryio.newInstall(context);
                    startupStages.set(STARTUP_STAGE_NEW_INSTALL_DONE - 1, new StartupStage(STARTUP_STAGE_NEW_INSTALL_DONE, true));

                    startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_LOADED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_LOADED, true));
                    startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED, true));

                    JSONObject blockedObject = (JSONObject) Lbryio.parseResponse(Lbryio.call("file", "list_blocked", context));
                    JSONArray blockedArray = blockedObject.getJSONArray("outpoints");
                    Lbryio.populateOutpointList(Lbryio.blockedOutpoints, blockedArray);
                    startupStages.set(STARTUP_STAGE_BLOCK_LIST_LOADED - 1, new StartupStage(STARTUP_STAGE_BLOCK_LIST_LOADED, true));

                    JSONObject filteredObject = (JSONObject) Lbryio.parseResponse(Lbryio.call("file", "list_filtered", context));
                    JSONArray filteredArray = filteredObject.getJSONArray("outpoints");
                    Lbryio.populateOutpointList(Lbryio.filteredOutpoints, filteredArray);
                    startupStages.set(STARTUP_STAGE_FILTER_LIST_LOADED - 1, new StartupStage(STARTUP_STAGE_FILTER_LIST_LOADED, true));
                } catch (Exception ex) {
                    // nope
                    Log.e(TAG, String.format("App startup failed: %s", ex.getMessage()), ex);
                    return false;
                } finally {
                    Helper.closeCloseable(reader);
                }

                return true;
            }
            protected void onPostExecute(Boolean startupSuccessful) {
                if (!startupSuccessful) {
                    // show which startup stage failed
                    renderStartupFailed(startupStages);
                    appStarted = false;
                    return;
                }

//                findViewById(R.id.splash_view).setVisibility(View.GONE);
                showActionBar();

//                loadLastFragment();
                fetchRewards();
                loadRemoteNotifications(false);

                checkUrlIntent(getIntent());
                checkWebSocketClient();
                LbryAnalytics.logEvent(LbryAnalytics.EVENT_APP_LAUNCH);
                appStarted = true;
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fetchRewards() {
        String authToken;
        AccountManager am = AccountManager.get(this);
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        if (odyseeAccount != null) {
            authToken = am.peekAuthToken(odyseeAccount, "auth_token_type");
        } else {
            authToken = "";
        }

        Map<String, String> options = new HashMap<>();
        options.put("multiple_rewards_per_type", "true");
        if (odyseeAccount != null && authToken != null) {
            options.put("auth_token", authToken);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<List<Reward>> supplier = new FetchRewardsSupplier(options);
            CompletableFuture<List<Reward>> cf = CompletableFuture.supplyAsync(supplier, ((OdyseeApp) getApplication()).getExecutor());
            cf.exceptionally(e -> {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showError(e.getMessage());
                    }
                });
                return null;
            }).thenAccept(rewards -> {
                Lbryio.updateRewardsLists(rewards);

                if (Lbryio.totalUnclaimedRewardAmount > 0) {
                    updateRewardsUsdValue();
                }
            });
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<List<Reward>> callable = new Callable<List<Reward>>() {
                        @Override
                        public List<Reward> call() {
                            List<Reward> rewards = null;
                            try {
                                JSONArray results = (JSONArray) Lbryio.parseResponse(Lbryio.call("reward", "list", options, null));
                                rewards = new ArrayList<>();
                                if (results != null) {
                                    for (int i = 0; i < results.length(); i++) {
                                        rewards.add(Reward.fromJSONObject(results.getJSONObject(i)));
                                    }
                                }
                            } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException ex) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showError(ex.getMessage());
                                    }
                                });
                            }

                            return rewards;
                        }
                    };

                    Future<List<Reward>> future = ((OdyseeApp) getApplication()).getExecutor().submit(callable);

                    try {
                        List<Reward> rewards = future.get();

                        if (rewards != null) {
                            Lbryio.updateRewardsLists(rewards);

                            if (Lbryio.totalUnclaimedRewardAmount > 0) {
                                updateRewardsUsdValue();
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    public void updateRewardsUsdValue() {
        if (Lbryio.totalUnclaimedRewardAmount > 0) {
            double usdRewardAmount = Lbryio.totalUnclaimedRewardAmount * Lbryio.LBCUSDRate;
        }
    }

    private void checkUrlIntent(Intent intent) {
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                // check special urls
                if (url.startsWith(SPECIAL_URL_PREFIX)) {
                    String specialPath = url.substring(8).toLowerCase();
                    if (specialRouteFragmentClassMap.containsKey(specialPath)) {
                        Class fragmentClass = specialRouteFragmentClassMap.get(specialPath);
                        if (fragmentClassNavIdMap.containsKey(fragmentClass)) {
                            Map<String, Object> params = new HashMap<>();
                            String tag = intent.getStringExtra("tag");
                            params.put("singleTag", tag);

//                            openFragment(specialRouteFragmentClassMap.get(specialPath), true, fragmentClassNavIdMap.get(fragmentClass), !Helper.isNullOrEmpty(tag) ? params : null);
                        }
                    }

                    // unrecognised path will open the following by default
                } else {
                    try {
                        LbryUri uri = LbryUri.parse(url);
                        String checkedURL = url.startsWith(LbryUri.PROTO_DEFAULT) ? url : uri.toString();
                        if (uri.isChannel()) {
                            openChannelUrl(checkedURL);
                        } else {
                            openFileUrl(checkedURL);
                        }
                    } catch (LbryUriException ex) {
                        // pass
                    }
                }

                inPictureInPictureMode = false;
                renderFullMode();
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        if (startingShareActivity) {
            // share activity triggered this, so reset the flag at this point
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startingShareActivity = false;
                }
            }, 1000);
            return;
        }
        if (startingPermissionRequest) {
            return;
        }
        if (playerManager != null && playerManager.getCurrentPlayer().isPlaying()) {
            enterPIPMode();
        }
    }

    protected boolean enterPIPMode() {
        if (enteringPIPMode) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                playerManager != null &&
                !startingFilePickerActivity &&
                !startingSignInFlowActivity) {
            enteringPIPMode = true;
            PictureInPictureParams params = new PictureInPictureParams.Builder().build();

            try {
                enterPictureInPictureMode(params);
                return true;
            } catch (IllegalStateException ex) {
                // pass
                enteringPIPMode = false;
            }
        }

        return false;
    }

    private void checkNotificationOpenIntent(Intent intent) {
        if (intent != null) {
            String notificationName = intent.getStringExtra("notification_name");
            if (notificationName != null) {
                logNotificationOpen(notificationName);
            }
        }
    }

    private void logNotificationOpen(String name) {
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_LBRY_NOTIFICATION_OPEN, bundle);
    }

    private void checkSendToIntent(Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction != null && intentAction.equals("android.intent.action.SEND")) {
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                Uri uri = clipData.getItemAt(0).getUri();

                String path = Helper.getRealPathFromURI_API19(this, uri);
                openSendTo(path);
            }
        }
    }

    private void unregisterReceivers() {
        if (uaReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uaReceiver);
        }
        Helper.unregisterReceiver(requestsReceiver, this);
    }

    public void setNowPlayingClaim(Claim claim, String url) {
        nowPlayingClaim = claim;
        nowPlayingClaimUrl = url;
        if (claim != null) {
            ((TextView) findViewById(R.id.global_now_playing_title)).setText(nowPlayingClaim.getTitle());
            ((TextView) findViewById(R.id.global_now_playing_channel_title)).setText(nowPlayingClaim.getPublisherTitle());
        }
    }

    public void clearNowPlayingClaim() {
        nowPlayingClaim = null;
        nowPlayingClaimUrl = null;
        nowPlayingClaimBitmap = null;
        findViewById(R.id.miniplayer).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.global_now_playing_title)).setText(null);
        ((TextView) findViewById(R.id.global_now_playing_channel_title)).setText(null);
        if (playerManager != null) {
            playerManager.getCurrentPlayer().setPlayWhenReady(false);
        }
    }

    public void hideSearchBar() {
//        findViewById(R.id.wunderbar_container).setVisibility(View.GONE);
    }

    public void showSearchBar() {
//        findViewById(R.id.wunderbar_container).setVisibility(View.VISIBLE);
//        clearWunderbarFocus(findViewById(R.id.wunderbar));
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        inPictureInPictureMode = isInPictureInPictureMode;
        enteringPIPMode = false;
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            renderPictureInPictureMode();
        } else {
            // Restore the full-screen UI.
            renderFullMode();
        }
    }

    protected void onStop() {
        if (playerManager != null && inPictureInPictureMode &&
                (isBackgroundPlaybackEnabled() && !isContinueBackgroundPlaybackPIPModeEnabled())) {
            playerManager.getCurrentPlayer().setPlayWhenReady(false);
        }
        if (inPictureInPictureMode) {
            MainActivity.playerReassigned = true;
        }
        accountManager.removeOnAccountsUpdatedListener(this);
        super.onStop();
    }

    /**
     * Used for search and on startup
     * @param fragment
     * @param allowNavigateBack
     */
    public void openFragment(Fragment fragment, boolean allowNavigateBack) {
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null && currentFragment.equals(fragment)) {
            return;
        }

        try {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction().replace(R.id.content_main, fragment);
            if (allowNavigateBack) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
            currentDisplayFragment = fragment;
        } catch (Exception ex) {
            // pass
        }
    }

    public void openFragment(Class fragmentClass, boolean allowNavigateBack, @Nullable Map<String, Object> params) {
        try {
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            if (fragment instanceof BaseFragment) {
                ((BaseFragment) fragment).setParams(params);
            }
            Fragment currentFragment = getCurrentFragment();
            if (currentFragment != null && currentFragment.equals(fragment)) {
                return;
            }

            if (currentFragment != null && ((BaseFragment) currentFragment).getParams() != null
                    && ((BaseFragment) currentFragment).getParams().containsKey("source")) {
                String sourceParam = (String) ((BaseFragment) currentFragment).getParams().get("source");

                if (sourceParam != null && sourceParam.equals("notification")) {
                    Map<String, Object> currentParams = new HashMap<>(1);

                    if (((BaseFragment) currentFragment).getParams().containsKey("url")) {
                        currentParams.put("url", ((BaseFragment) currentFragment).getParams().get("url"));
                    }

                    ((BaseFragment) currentFragment).setParams(currentParams);
                }
            }

            //fragment.setRetainInstance(true);
            FragmentManager manager = getSupportFragmentManager();

            if (fragment instanceof FileViewFragment || fragment instanceof ChannelFragment)
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);

            FragmentTransaction transaction;
            if (fragment instanceof FileViewFragment) {
                Slide enterTransition = new Slide(Gravity.BOTTOM);
                enterTransition.setDuration(this.getResources().getInteger(android.R.integer.config_mediumAnimTime));
                fragment.setEnterTransition(enterTransition);
                transaction = manager.beginTransaction().replace(R.id.main_activity_other_fragment, fragment, FILE_VIEW_TAG);
            } else {
                transaction = manager.beginTransaction().replace(R.id.main_activity_other_fragment, fragment);
            }
            if (allowNavigateBack) {
                transaction.addToBackStack(null);
            }

            ActionBar actionBar = getSupportActionBar();

            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(!(fragment instanceof FileViewFragment) && allowNavigateBack);
            }

            transaction.commit();

            currentDisplayFragment = fragment;
            findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
            findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
            findViewById(R.id.title).setVisibility(View.GONE);
            findViewById(R.id.toolbar_balance_and_tools_layout).setVisibility(View.GONE);
        } catch (Exception ex) {
            ex.printStackTrace();
            // pass
        }
    }

    public void refreshChannelCreationRequired(View root) {
        if (isSignedIn()) {
            fetchOwnChannels();
            root.findViewById(R.id.user_not_signed_in).setVisibility(View.GONE);

            if (!Lbry.ownChannels.isEmpty()) {
                root.findViewById(R.id.has_channels).setVisibility(View.VISIBLE);
                root.findViewById(R.id.no_channels).setVisibility(View.GONE);
            } else {
                root.findViewById(R.id.has_channels).setVisibility(View.GONE);
                root.findViewById(R.id.no_channels).setVisibility(View.VISIBLE);
            }
        } else {
            root.findViewById(R.id.user_not_signed_in).setVisibility(View.VISIBLE);

            root.findViewById(R.id.has_channels).setVisibility(View.GONE);
            root.findViewById(R.id.no_channels).setVisibility(View.GONE);
        }
    }

    /**
     * This shows the bottom sheet with the channel creator UI to quickly create a channel when one is required.
     *
     * There is no need for any method to hide it as it is either done by Android framework or programmatically
     * by our BottomSheetDialog implementation.
     * @param listener ChannelCreateDialogFragment.ChannelCreateListener implementation to run when a channel has been created
     */
    public void showChannelCreator(ChannelCreateDialogFragment.ChannelCreateListener listener) {
        if (channelCreationBottomSheet == null) {
            channelCreationBottomSheet = ChannelCreateDialogFragment.newInstance(listener);
        }

        channelCreationBottomSheet.show(getSupportFragmentManager(), "ModalChannelCreateBottomSheet");
    }

    /**
     * Call this to nullify the bottom sheet object so listener is always assigned from the calling class
     */
    public void destroyChannelCreator() {
        channelCreationBottomSheet = null;
    }
    public void fetchOwnChannels() {
        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, null, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownChannels = Helper.filterDeletedClaims(new ArrayList<>(claims));
                for (FetchChannelsListener listener : fetchChannelsListeners) {
                    listener.onChannelsFetched(claims);
                }
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void fetchOwnClaims() {
        Map<String, Object> options = Lbry.buildClaimListOptions(
                Arrays.asList(Claim.TYPE_STREAM, Claim.TYPE_REPOST), 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, null, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownClaims = Helper.filterDeletedClaims(new ArrayList<>(claims));
                for (FetchClaimsListener listener : fetchClaimsListeners) {
                    listener.onClaimsFetched(claims);
                }
            }

            @Override
            public void onError(Exception error) { }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void unlockTips() {
        if (unlockingTips) {
            return;
        }

        Map<String, Object> options = new HashMap<>();
        options.put("type", "support");
        options.put("is_not_my_input", true);
        options.put("blocking", true);

        AccountManager am = AccountManager.get(getApplicationContext());
        String authToken = am.peekAuthToken(am.getAccounts()[0], "auth_token_type");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            unlockingTips = true;

            Supplier<Boolean> task = new UnlockingTipsSupplier(options, authToken);
            CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(task, ((OdyseeApp) getApplication()).getExecutor());
            completableFuture.thenAccept(result -> unlockingTips = false);
        } else {
            Thread unlockingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<Boolean> callable = () -> {
                        try {
                            Lbry.directApiCall(Lbry.METHOD_TXO_SPEND, options, authToken);
                            return true;
                        } catch (ApiCallException | ClassCastException ex) {
                            ex.printStackTrace();
                            return false;
                        }
                    };

                    Future<Boolean> future = ((OdyseeApp) getApplication()).getExecutor().submit(callable);

                    try {
                        unlockingTips = true;
                        future.get(); // This doesn't block main thread as it is called from a different thread
                        unlockingTips = false;
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            unlockingThread.start();
        }
    }

    private void displayUnseenNotificationCount(int count) {
        String text = count > 99 ? "99+" : String.valueOf(count);

        TextView badge = findViewById(R.id.notifications_badge_count);
        badge.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
        badge.setText(text);
    }

    private void loadUnseenNotificationsCount() {
        Activity activity = this;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    int count = DatabaseHelper.getUnseenNotificationsCount(db);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayUnseenNotificationCount(count);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void loadRemoteNotifications(boolean markRead) {
        findViewById(R.id.notification_list_empty_container).setVisibility(View.GONE);

        Map<String, String> options = new HashMap<>(1);
        AccountManager am = AccountManager.get(this);
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        if (odyseeAccount != null) {
            options.put("auth_token", am.peekAuthToken(odyseeAccount, "auth_token_type"));
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Activity activity = this;
            Supplier<List<LbryNotification>> supplier = new NotificationListSupplier(options);
            CompletableFuture<List<LbryNotification>> cf = CompletableFuture.supplyAsync(supplier, ((OdyseeApp) getApplication()).getExecutor());
            cf.thenAcceptAsync(result -> {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (notificationsSwipeContainer != null) {
                            notificationsSwipeContainer.setRefreshing(false);
                        }
                    }
                });
                if (result != null) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    for (LbryNotification notification : result) {
                        DatabaseHelper.createOrUpdateNotification(notification, db);
                    }
                    remoteNotifcationsLastLoaded = new Date();

                    loadUnseenNotificationsCount();
                    loadLocalNotifications();
                    if (markRead && findViewById(R.id.notifications_container).getVisibility() == View.VISIBLE) {
                        markNotificationsSeen();
                    }
                }

            }, ((OdyseeApp) activity.getApplication()).getExecutor());
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<List<LbryNotification>> callable = new Callable<List<LbryNotification>>() {
                        @Override
                        public List<LbryNotification> call() {
                            List<LbryNotification> notifications = new ArrayList<>();
                            try {
                                JSONArray array = (JSONArray) Lbryio.parseResponse(Lbryio.call("notification", "list", options, null));
                                if (array != null) {
                                    for (int i = 0; i < array.length(); i++) {
                                        JSONObject item = array.getJSONObject(i);
                                        if (item.has("notification_parameters")) {
                                            LbryNotification notification = new LbryNotification();

                                            JSONObject notificationParams = item.getJSONObject("notification_parameters");
                                            if (notificationParams.has("device")) {
                                                JSONObject device = notificationParams.getJSONObject("device");
                                                notification.setTitle(Helper.getJSONString("title", null, device));
                                                notification.setDescription(Helper.getJSONString("text", null, device));
                                                notification.setTargetUrl(Helper.getJSONString("target", null, device));
                                            }
                                            if (notificationParams.has("dynamic") && !notificationParams.isNull("dynamic")) {
                                                JSONObject dynamic = notificationParams.getJSONObject("dynamic");
                                                if (dynamic.has("comment_author")) {
                                                    notification.setAuthorThumbnailUrl(Helper.getJSONString("comment_author", null, dynamic));
                                                }
                                                if (dynamic.has("channelURI")) {
                                                    String channelUrl = Helper.getJSONString("channelURI", null, dynamic);
                                                    if (!Helper.isNullOrEmpty(channelUrl)) {
                                                        notification.setTargetUrl(channelUrl);
                                                    }
                                                }
                                                if (dynamic.has("hash") && "comment".equalsIgnoreCase(Helper.getJSONString("notification_rule", null, item))) {
                                                    notification.setTargetUrl(String.format("%s?comment_hash=%s", notification.getTargetUrl(), dynamic.getString("hash")));
                                                }
                                            }

                                            notification.setRule(Helper.getJSONString("notification_rule", null, item));
                                            notification.setRemoteId(Helper.getJSONLong("id", 0, item));
                                            notification.setRead(Helper.getJSONBoolean("is_read", false, item));
                                            notification.setSeen(Helper.getJSONBoolean("is_seen", false, item));

                                            try {
                                                SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.ISO_DATE_FORMAT_JSON, Locale.US);
                                                notification.setTimestamp(dateFormat.parse(Helper.getJSONString("created_at", dateFormat.format(new Date()), item)));
                                            } catch (ParseException ex) {
                                                notification.setTimestamp(new Date());
                                            }

                                            if (notification.getRemoteId() > 0 && !Helper.isNullOrEmpty(notification.getDescription())) {
                                                notifications.add(notification);
                                            }
                                        }
                                    }
                                }
                            } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException | IllegalStateException ex) {
                                ex.printStackTrace();
                                return null;
                            }

                            return notifications;
                        }
                    };
                    Future<List<LbryNotification>> future = ((OdyseeApp) getApplication()).getExecutor().submit(callable);

                    try {
                        List<LbryNotification> notifications = future.get();

                        if (notifications != null) {
                            remoteNotifcationsLastLoaded = new Date();

                            loadUnseenNotificationsCount();
                            loadLocalNotifications();
                            if (markRead && findViewById(R.id.notifications_container).getVisibility() == View.VISIBLE) {
                                markNotificationsSeen();
                            }

                            if (notificationsSwipeContainer != null) {
                                notificationsSwipeContainer.setRefreshing(false);
                            }
                        } else {
                            loadLocalNotifications();
                            if (notificationsSwipeContainer != null) {
                                notificationsSwipeContainer.setRefreshing(false);
                            }
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    @AnyThread
    private void loadLocalNotifications() {
        // Path to here could be not from the main thread, so let's ensure changing visibility
        // is requested from the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.notification_list_empty_container).setVisibility(View.GONE);
                findViewById(R.id.notifications_progress).setVisibility(View.VISIBLE);
            }
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<List<LbryNotification>> task = new GetLocalNotificationsSupplier();
            CompletableFuture<List<LbryNotification>> completableFuture = CompletableFuture.supplyAsync(task);
            completableFuture.thenAccept(n -> {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateLocalNotifications(n);
                    }
                });
            });
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<List<LbryNotification>> callable = new Callable<List<LbryNotification>>() {
                        @Override
                        public List<LbryNotification> call() {
                            List<LbryNotification> notifications = new ArrayList<>();

                            try {
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                notifications = DatabaseHelper.getNotifications(db);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return notifications;
                        }
                    };

                    Future<List<LbryNotification>> futureNotifications = ((OdyseeApp) getApplication()).getExecutor().submit(callable);
                    try {
                        List<LbryNotification> notifications = futureNotifications.get();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateLocalNotifications(notifications);
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        }
    }

    private void updateLocalNotifications(List<LbryNotification> notifications){
        findViewById(R.id.notification_list_empty_container).setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.notifications_progress).setVisibility(View.GONE);
        loadUnseenNotificationsCount();

        if (notificationListAdapter == null) {
            notificationListAdapter = new NotificationListAdapter(notifications, MainActivity.this);
            notificationListAdapter.setSelectionModeListener(MainActivity.this);
            ((RecyclerView) findViewById(R.id.notifications_list)).setAdapter(notificationListAdapter);
        } else {
            notificationListAdapter.addNotifications(notifications);
        }

        resolveCommentAuthors(notificationListAdapter.getAuthorUrls());
        notificationListAdapter.setClickListener(new NotificationListAdapter.NotificationClickListener() {
            @Override
            public void onNotificationClicked(LbryNotification notification) {
                // set as seen and read
                Map<String, String> options = new HashMap<>();
                options.put("notification_ids", String.valueOf(notification.getRemoteId()));
                options.put("is_seen", "true");
                // Odysee Android is not yet able to display a list with user's subscriptions,
                // so let's not mark the notification as read
                if (!notification.getTargetUrl().equalsIgnoreCase("lbry://?subscriptions")) {
                    options.put("is_read", "true");
                } else {
                    options.put("is_read", "false");
                }

                AccountManager am = AccountManager.get(getApplicationContext());

                if (am != null) {
                    String at = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    if (at != null)
                        options.put("auth_token", at);
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    Supplier<Boolean> supplier = new NotificationUpdateSupplier(options);
                    CompletableFuture.supplyAsync(supplier);
                } else {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Lbryio.call("notification", "edit", options, null);
                            } catch (LbryioResponseException | LbryioRequestException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
                if (!notification.getTargetUrl().equalsIgnoreCase("lbry://?subscriptions")) {
                    markNotificationReadAndSeen(notification.getId());
                }

                String targetUrl = notification.getTargetUrl();
                if (targetUrl.startsWith(SPECIAL_URL_PREFIX)) {
                    openSpecialUrl(targetUrl, "notification");
                } else {
                    LbryUri target = LbryUri.tryParse(notification.getTargetUrl());
                    if (target != null) {
                        if (target.isChannel()) {
                            openChannelUrl(notification.getTargetUrl(), "notification");
                        } else {
                            openFileUrl(notification.getTargetUrl(), "notification");
                        }
                    }
                }
                hideNotifications(false);
            }
        });
    }

    private void markNotificationReadAndSeen(long notificationId) {
        Activity activity = this;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (dbHelper != null) {
                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        DatabaseHelper.markNotificationReadAndSeen(notificationId, db);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadUnseenNotificationsCount();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    public void showRewardsVerification() {
        openFragment(RewardVerificationFragment.class, true, null);
        rewardVerificationActive = true;
    }

    public void dismissRewardsVerification() {
        if (rewardVerificationActive && (currentDisplayFragment instanceof RewardVerificationFragment)) {

        }
    }

    private void resolveCommentAuthors(List<String> urls) {
        if (urls != null && !urls.isEmpty()) {
            ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, null, new ResolveResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims) {
                    if (notificationListAdapter != null) {
                        notificationListAdapter.updateAuthorClaims(claims);
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

    // create list dialog
    public void handleAddUrlToCustomList(String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.playlist_title);

        builder.setTitle(R.string.new_list).
                setCancelable(true).
                setView(dialogView).
                setPositiveButton(R.string.create, null).
                setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setEnabled(false);

                if (titleInput != null) {
                    titleInput.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            Editable text = titleInput.getText();
                            if (text != null) {
                                button.setEnabled(text.length() > 0);
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    });
                }

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (titleInput != null) {
                            String title = Helper.getValue(titleInput.getText());
                            if (Helper.isNullOrEmpty(title)) {
                                // show error
                                showError(getString(R.string.enter_title));
                                return;
                            }

                            OdyseeCollection collection = OdyseeCollection.createPrivatePlaylist(title);
                            collection.setItems(Arrays.asList(url));
                            handleAddUrlToList(url, collection, true);
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    public void handleAddUrlToList(String url, OdyseeCollection collection, boolean showMessage) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    if (Helper.isNullOrEmpty(collection.getId())) {
                        DatabaseHelper.saveCollection(collection, db);
                    } else {
                        DatabaseHelper.addCollectionItem(collection.getId(), url, db);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCollectionUpdated(collection, showMessage);
                        }
                    });

                    // initiate sync afterwards
                    saveSharedUserState();
                } catch (SQLiteException ex) {
                    // failed
                    if (showMessage) {
                        showError(getString(R.string.could_not_add_to_list, collection.getName()));
                    }
                }
            }
        });
    }

    private void onCollectionUpdated(OdyseeCollection collection, boolean showMessage) {
        if (showMessage) {
            showMessage(getString(R.string.added_to_list, collection.getName()),
                getString(R.string.see_list),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // open the playlist fragment with the id
                        openPlaylistFragment(collection.getId());
                    }
                });
        }
    }

    public void handleRemoveUrlFromList(String url, OdyseeCollection collection) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.removeCollectionItem(collection.getId(), url, db);

                    // initiate sync afterwards
                    saveSharedUserState();
                } catch (SQLiteException ex) {
                    // pass
                }
            }
        });
    }

    public void openPlaylistFragment(String playlistId) {
        Map<String, Object> params = new HashMap<>();
        params.put("collectionId", playlistId);
        openFragment(PlaylistFragment.class, true, params);
    }

    public void handleAddUrlToList(String url, String builtInId) {
        if (!Arrays.asList(OdyseeCollection.BUILT_IN_ID_FAVORITES,  OdyseeCollection.BUILT_IN_ID_WATCHLATER).contains(builtInId)) {
            // add to list. show bottom sheet dialog with playlists
            AddToListsDialogFragment dialog = AddToListsDialogFragment.newInstance();
            dialog.setUrl(url);
            dialog.show(getSupportFragmentManager(), AddToListsDialogFragment.TAG);
            return;
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.addCollectionItem(builtInId, url, db);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessage(getString(R.string.added_to_list,
                                    OdyseeCollection.BUILT_IN_ID_FAVORITES.equalsIgnoreCase(builtInId) ? getString(R.string.favorites) : getString(R.string.watch_later)),
                                    getString(R.string.see_list),
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            // open the playlist fragment with the id
                                            openPlaylistFragment(builtInId);
                                        }
                                    });
                        }
                    });

                    // initiate sync afterwards
                    saveSharedUserState();
                } catch (SQLiteException ex) {
                    // failed
                    showError(getString(R.string.could_not_add_to_list,
                            OdyseeCollection.BUILT_IN_ID_FAVORITES.equalsIgnoreCase(builtInId) ? getString(R.string.favorites) : getString(R.string.watch_later)));
                }
            }
        });
    }

    public void handleMuteChannel(final Claim channel) {
        if (channel != null) {
            // show confirm dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this).
                    setTitle(getString(R.string.confirm_mute_channel_title, channel.getName())).
                    setMessage(R.string.confirm_mute_channel)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            muteChannel(channel);
                        }
                    }).setNegativeButton(R.string.no, null);
            builder.show();
        }
    }

    public void handleUnmuteChannel(final Claim channel) {
        ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.removeBlockedChannel(channel.getClaimId(), db);
                    Lbryio.mutedChannels = new ArrayList<>(DatabaseHelper.getBlockedChannels(db));

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelUnmuting(true, channel);
                        }
                    });
                } catch (SQLiteException ex) {
                    // pass
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelUnmuting(false, null);
                        }
                    });
                }
            }
        });
    }

    public void muteChannel(Claim channel) {
        ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.createOrUpdateBlockedChannel(channel.getClaimId(), channel.getName(), db);
                    Lbryio.mutedChannels = new ArrayList<>(DatabaseHelper.getBlockedChannels(db));

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelMuting(true, channel);
                        }
                    });
                } catch (SQLiteException ex) {
                    // pass
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelMuting(false);
                        }
                    });
                }
            }
        });
    }


    public void finishChannelMuting(boolean success) {
        finishChannelMuting(success, null);
    }

    public void finishChannelMuting(boolean success, Claim channel) {
        if (!success) {
            showMessage(getString(R.string.channel_could_not_be_muted));
            return;
        }

        if (channel != null) {
            showMessage(getString(R.string.channel_muted, channel.getName()));
        }

        // refresh claim adapters where appropriate
        applyMutedChannelFilters();
        saveSharedUserState();
    }

    public void finishChannelUnmuting(boolean success, Claim channel) {
        if (!success) {
            showMessage(getString(R.string.channel_could_not_be_unmuted));
            return;
        }

        if (channel != null) {
            showMessage(getString(R.string.channel_unmuted, channel.getName()));
        }

        applyMutedChannelFilters();
        saveSharedUserState();
    }

    private void applyBlockedChannelFilters() {
        Fragment current = getCurrentFragment();
        if (current != null) {
            if (current instanceof ChannelFragment) {
                ((ChannelFragment) current).applyFilterForBlockedChannels(Lbryio.blockedChannels); // channel comments
            }
        }
    }

    private void applyMutedChannelFilters() {
        Fragment current = getCurrentFragment();
        if (current != null) {
            if (current instanceof AllContentFragment) {
                ((AllContentFragment) current).applyFilterForMutedChannels(Lbryio.mutedChannels); // content view
            } else if (current instanceof FileViewFragment) {
                ((FileViewFragment) current).applyFilterForMutedChannels(Lbryio.mutedChannels); // related content and comments view
            } else if (current instanceof SearchFragment) {
                ((SearchFragment) current).applyFilterForMutedChannels(Lbryio.mutedChannels); // search results
            } else if (current instanceof ChannelFragment) {
                ((ChannelFragment) current).applyFilterForMutedChannels(Lbryio.mutedChannels); // channel comments
            }
        }
    }

    public void handleBlockChannel(Claim channel, Claim modChannel) {
        if (channel != null) {
            if (modChannel == null) {
                String defaultChannelName = Helper.getDefaultChannelName(this);
                if (!Helper.isNullOrEmpty(defaultChannelName)) {
                    modChannel = Lbry.ownChannels.stream().filter(
                            claim -> defaultChannelName.equalsIgnoreCase(claim.getName())).findFirst().orElse(null);
                } else {
                    // no default channel set, use the first found owned channel
                    modChannel = Lbry.ownChannels.size() > 0 ? Lbry.ownChannels.get(0) : null;
                }

                // if it's still null after this point, return
                if (modChannel == null) {
                    return;
                }
            }

            final Claim actualModChannel = modChannel;

            // show confirm dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this).
                    setTitle(getString(R.string.confirm_block_channel_title, channel.getName())).
                    setMessage(getString(R.string.confirm_block_channel, actualModChannel.getName()))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            blockChannel(channel, actualModChannel);
                        }
                    }).setNegativeButton(R.string.no, null);
            builder.show();
        }
    }

    public void handleUnblockChannel(final Claim channel, Claim modChannel) {
        if (modChannel == null) {
            String defaultChannelName = Helper.getDefaultChannelName(this);
            if (!Helper.isNullOrEmpty(defaultChannelName)) {
                modChannel = Lbry.ownChannels.stream().filter(
                        claim -> defaultChannelName.equalsIgnoreCase(claim.getName())).findFirst().orElse(null);
            } else {
                // no default channel set, use the first found owned channel
                modChannel = Lbry.ownChannels.size() > 0 ? Lbry.ownChannels.get(0) : null;
            }

            // if it's still null after this point, return
            if (modChannel == null) {
                return;
            }
        }

        final Claim actualModChannel = modChannel;

        ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // perform moderation.UnBlock request
                    JSONObject params = new JSONObject();
                    params.put("channel_id", actualModChannel.getClaimId());
                    params.put("channel_name", actualModChannel.getName());
                    params.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);
                    JSONObject jsonChannelSign = Comments.channelSignName(params, actualModChannel.getClaimId(), actualModChannel.getName());

                    Map<String, Object> options = new HashMap<>();
                    options.put("mod_channel_id", actualModChannel.getClaimId());
                    options.put("mod_channel_name", actualModChannel.getName());
                    options.put("un_blocked_channel_id", channel.getClaimId());
                    options.put("un_blocked_channel_name", channel.getName().substring(1)); // strip the @?
                    options.put("signature", Helper.getJSONString("signature", "", jsonChannelSign));
                    options.put("signing_ts", Helper.getJSONString("signing_ts", "", jsonChannelSign));
                    options.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "moderation.UnBlock");
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelUnblocking(true, channel, actualModChannel);
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // pass
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelUnblocking(false, channel, actualModChannel);
                        }
                    });
                }
            }
        });
    }

    public void blockChannel(Claim channel, Claim modChannel) {
        ((OdyseeApp) getApplication()).getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // perform moderation.Block request
                    JSONObject params = new JSONObject();
                    params.put("channel_id", modChannel.getClaimId());
                    params.put("channel_name", modChannel.getName());
                    params.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);
                    JSONObject jsonChannelSign = Comments.channelSignName(params, modChannel.getClaimId(), modChannel.getName());

                    Map<String, Object> options = new HashMap<>();
                    options.put("mod_channel_id", modChannel.getClaimId());
                    options.put("mod_channel_name", modChannel.getName());
                    options.put("blocked_channel_id", channel.getClaimId());
                    options.put("blocked_channel_name", channel.getName().substring(1)); // strip the @?
                    options.put("signature", Helper.getJSONString("signature", "", jsonChannelSign));
                    options.put("signing_ts", Helper.getJSONString("signing_ts", "", jsonChannelSign));
                    options.put(Lbryio.AUTH_TOKEN_PARAM, Lbryio.AUTH_TOKEN);

                    okhttp3.Response response = Comments.performRequest(Lbry.buildJsonParams(options), "moderation.Block");
                    ResponseBody responseBody = response.body();
                    JSONObject jsonResponse = new JSONObject(responseBody.string());
                    if (!jsonResponse.has("result") || jsonResponse.isNull("result")) {
                        throw new ApiCallException("invalid json response");
                    }

                    JSONObject result = Helper.getJSONObject("result", jsonResponse);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelBlocking(true, channel, modChannel);
                        }
                    });
                } catch (JSONException | ApiCallException | IOException ex) {
                    // pass
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishChannelBlocking(false, channel, modChannel);
                        }
                    });
                }
            }
        });
    }

    public void finishChannelBlocking(boolean success, Claim channel, Claim modChannel) {
        if (!success) {
            showMessage(getString(R.string.channel_could_not_be_blocked));
            return;
        }

        if (channel != null) {
            LbryUri url = LbryUri.tryParse(channel.getPermanentUrl());
            if (url != null && !Lbryio.blockedChannels.contains(url)) {
                Lbryio.blockedChannels.add(url);
            }
            showMessage(getString(R.string.channel_blocked, modChannel.getName()));
        }

        applyBlockedChannelFilters();
    }

    public void finishChannelUnblocking(boolean success, Claim channel, Claim modChannel) {
        if (!success) {
            showMessage(getString(R.string.channel_could_not_be_unblocked));
            return;
        }

        if (channel != null) {
            LbryUri url = LbryUri.tryParse(channel.getPermanentUrl());
            if (url != null) {
                Lbryio.blockedChannels.remove(url);
            }
            showMessage(getString(R.string.channel_unblocked, modChannel.getName()));
        }

        applyBlockedChannelFilters();
    }

    private void checkSyncedWallet() {
        // FIXME
/*
        String password = Utils.getSecureValue(SECURE_VALUE_KEY_SAVED_PASSWORD, this, Lbry.KEYSTORE);
        // Just check if the current user has a synced wallet, no need to do anything else here
        SyncGetTask task = new SyncGetTask(password, false, null, new DefaultSyncTaskHandler() {
            @Override
            public void onSyncGetSuccess(WalletSync walletSync) {
                Lbryio.userHasSyncedWallet = true;
                Lbryio.setLastWalletSync(walletSync);
                Lbryio.setLastRemoteHash(walletSync.getHash());
            }

            @Override
            public void onSyncGetWalletNotFound() { }
            @Override
            public void onSyncGetError(Exception error) { }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
*/
    }

    public static void requestPermission(String permission, int requestCode, String rationale, Context context, boolean forceRequest) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission)) {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).showMessage(rationale);
                }
            } else if (forceRequest) {
                startingPermissionRequest = true;
                ActivityCompat.requestPermissions((Activity) context, new String[] { permission }, requestCode);
            } else {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).showError(rationale);
                }
            }
        }
    }

    public static boolean hasPermission(String permission, Context context) {
        if (context == null) {
            return false;
        }
        return (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }

    public void updateCurrentDisplayFragment(Fragment fragment) {
        this.currentDisplayFragment = fragment;
    }

    public void resetCurrentDisplayFragment() {
        currentDisplayFragment = null;
    }

    @Override
    public void onEmailAdded(String email) {

    }

    @Override
    public void onEmailEdit() {

    }

    @Override
    public void onEmailVerified() {

    }

    @Override
    public void onPhoneAdded(String countryCode, String phoneNumber) {

    }

    @Override
    public void onPhoneVerified() {

    }

    @Override
    public void onManualVerifyContinue() {

    }

    @Override
    public void onSkipQueueAction() {
        verificationSkipQueue.onSkipQueueAction(this);
    }

    @Override
    public void onTwitterVerified() {

    }

    @Override
    public void onManualProgress(boolean progress) {

    }

    public interface BackPressInterceptor {
        boolean onBackPressed();
    }
}
