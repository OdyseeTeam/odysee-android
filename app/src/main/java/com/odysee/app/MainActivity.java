package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
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
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
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
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.odysee.app.model.lbryinc.RewardVerified;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.supplier.GetLocalNotificationsSupplier;
import com.odysee.app.supplier.NotificationUpdateSupplier;
import com.odysee.app.supplier.UnlockingTipsSupplier;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.tasks.RewardVerifiedHandler;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.lbryinc.AndroidPurchaseTask;
import com.odysee.app.tasks.lbryinc.ClaimRewardTask;
import com.odysee.app.tasks.lbryinc.FetchRewardsTask;
import com.odysee.app.tasks.MergeSubscriptionsTask;
import com.odysee.app.tasks.claim.ResolveTask;
import com.odysee.app.tasks.lbryinc.NotificationDeleteTask;
import com.odysee.app.tasks.lbryinc.NotificationListTask;
import com.odysee.app.tasks.localdata.FetchRecentUrlHistoryTask;
import com.odysee.app.tasks.wallet.DefaultSyncTaskHandler;
import com.odysee.app.tasks.wallet.LoadSharedUserStateTask;
import com.odysee.app.tasks.wallet.SaveSharedUserStateTask;
import com.odysee.app.tasks.wallet.SyncApplyTask;
import com.odysee.app.tasks.wallet.SyncGetTask;
import com.odysee.app.tasks.wallet.SyncSetTask;
import com.odysee.app.tasks.wallet.WalletBalanceTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.channel.ChannelFragment;
import com.odysee.app.ui.channel.ChannelManagerFragment;
import com.odysee.app.ui.findcontent.FileViewFragment;
import com.odysee.app.ui.findcontent.FollowingFragment;
import com.odysee.app.ui.library.LibraryFragment;
import com.odysee.app.ui.publish.PublishFragment;
import com.odysee.app.ui.publish.PublishesFragment;
import com.odysee.app.ui.findcontent.AllContentFragment;
import com.odysee.app.ui.findcontent.SearchFragment;
import com.odysee.app.ui.wallet.InvitesFragment;
import com.odysee.app.ui.wallet.RewardsFragment;
import com.odysee.app.ui.wallet.WalletFragment;
import com.odysee.app.utils.ContentSources;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.Utils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;

import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener,
        ActionMode.Callback, SelectionModeListener, OnAccountsUpdateListener {
    private static final String CHANNEL_ID_PLAYBACK = "com.odysee.app.LBRY_PLAYBACK_CHANNEL";
    private static final int PLAYBACK_NOTIFICATION_ID = 3;
    private static final String SPECIAL_URL_PREFIX = "lbry://?";
    private static final int REMOTE_NOTIFICATION_REFRESH_TTL = 300000; // 5 minutes
    public static final String SKU_SKIP = "lbryskip";

    public static final int SOURCE_NOW_PLAYING_FILE = 1;
    public static final int SOURCE_NOW_PLAYING_SHUFFLE = 2;
    public static MainActivity instance;
    private int pendingSourceTabId;

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

    public static SimpleExoPlayer appPlayer;
    public static Cache playerCache;
    public static boolean playerReassigned;
    public CastContext castContext;
    public static CastPlayer castPlayer;
    public static int nowPlayingSource;
    public static Claim nowPlayingClaim;
    public static String nowPlayingClaimUrl;
    public static boolean startingFilePickerActivity = false;
    public static boolean startingShareActivity = false;
    public static boolean startingPermissionRequest = false;
    public static final boolean startingSignInFlowActivity = false;

    @Getter
    private boolean userInstallInitialised;
    @Getter
    private boolean initialCategoriesLoaded;
    private ActionMode actionMode;
    private BillingClient billingClient;
    @Getter
    private boolean enteringPIPMode = false;
    private boolean fullSyncInProgress = false;
    private int queuedSyncCount = 0;
    private String cameraOutputFilename;
    private Bitmap nowPlayingClaimBitmap;

    @Setter
    private BackPressInterceptor backPressInterceptor;
    private WebSocketClient webSocketClient;

    private int bottomNavigationHeight = 0;

    @Getter
    private String firebaseMessagingToken;

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
    public static final String PREFERENCE_KEY_MEDIA_AUTOPLAY = "com.odysee.app.preference.userinterface.MediaAutoplay";
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

    public static final String PREFERENCE_KEY_INTERNAL_FIRST_RUN_COMPLETED = "com.odysee.app.preference.internal.FirstRunCompleted";
    public static final String PREFERENCE_KEY_INTERNAL_FIRST_AUTH_COMPLETED = "com.odysee.app.preference.internal.FirstAuthCompleted";

    public static final String PREFERENCE_KEY_AUTH_TOKEN = "com.odysee.app.Preference.AuthToken";

    public static final String SECURE_VALUE_KEY_SAVED_PASSWORD = "com.odysee.app.PX";
    public static final String SECURE_VALUE_FIRST_RUN_PASSWORD = "firstRunPassword";

    private static final String TAG = "OdyseeMain";

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
    @Getter
    private DatabaseHelper dbHelper;
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
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledWalletUpdater;
    private boolean walletSyncScheduled;

    AccountManager accountManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        // workaround to fix dark theme because https://issuetracker.google.com/issues/37124582
        try {
            new WebView(this);
        } catch (Exception ex) {
            // pass (don't fail initialization on some _weird_ device implementations)
        }
//        AppCompatDelegate.setDefaultNightMode(isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        initKeyStore();
        loadAuthToken();

//        if (Build.VERSION.SDK_INT >= M && !isDarkMode()) {
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//        }
        initSpecialRouteMap();

        LbryAnalytics.init(this);
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    firebaseMessagingToken = task.getResult();
                }
            });
        } catch (IllegalStateException ex) {
            // pass
        }

        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);
        checkNotificationOpenIntent(getIntent());
        setContentView(R.layout.activity_main);

        findViewById(R.id.root).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        findViewById(R.id.launch_splash).setVisibility(View.VISIBLE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateMiniPlayerMargins();
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // setup the billing client in main activity (to handle cases where the verification purchase flow may have been interrupted)
        billingClient = BillingClient.newBuilder(this)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                        int responseCode = billingResult.getResponseCode();
                        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
                        {
                            for (Purchase purchase : purchases) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                })
                .enablePendingPurchases()
                .build();
        establishBillingClientConnection();

        playerNotificationManager = new PlayerNotificationManager.Builder(
                this, PLAYBACK_NOTIFICATION_ID, "io.lbry.browser.DAEMON_NOTIFICATION_CHANNEL", new PlayerNotificationDescriptionAdapter()).build();

        // TODO: Check Google Play Services availability
        // castContext = CastContext.getSharedInstance(this);

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

        // register receivers
        registerRequestsReceiver();
        registerUAReceiver();

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    // not fullscreen
                }
            }
        });

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

        // Create Fragment instances here so they are not recreated when selected on the bottom navigation bar
        Fragment homeFragment = new AllContentFragment();
        Fragment followingFragment = new FollowingFragment();
        Fragment walletFragment = new WalletFragment();
        Fragment libraryFragment = new LibraryFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(backStackChangedListener);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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
                // Hide bottom navigation
                // Hide main bar
                // Show PublishFragment.class
                // hideNotifications(); // Avoid showing Notifications fragment when clicking Publish when Notification panel is opened
//                fragmentManager.beginTransaction().replace(R.id.main_activity_other_fragment, new PublishFragment(), "PUBLISH").addToBackStack("publish_claim").commit();
//                findViewById(R.id.main_activity_other_fragment).setVisibility(View.VISIBLE);
//                findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
//                hideActionBar();
                startActivity(new Intent(view.getContext(), ComingSoon.class));
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
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container_search, SearchFragment.class.newInstance(), "SEARCH").commit();
                        findViewById(R.id.fragment_container_search).setVisibility(View.VISIBLE);
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
                    ((SearchFragment) getSupportFragmentManager().findFragmentByTag("SEARCH")).search(query, 0);
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
                getSupportFragmentManager().beginTransaction()
                        .remove(getSupportFragmentManager().findFragmentByTag("SEARCH")).commit();
                ((EditText)findViewById(R.id.search_query_text)).setText("");
                showBottomNavigation();
                switchToolbarForSearch(false);

                // On tablets, multiple fragments could be visible. Don't show Home Screen when File View is visible
                if (findViewById(R.id.main_activity_other_fragment).getVisibility() != View.VISIBLE)
                    findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);

                showWalletBalance();
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.profile_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.profile_button).setEnabled(false);
                LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View customView = layoutInflater.inflate(R.layout.popup_user,null);
                PopupWindow popupWindow = new PopupWindow(customView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

                ImageButton closeButton = customView.findViewById(R.id.popup_user_close_button);
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        findViewById(R.id.profile_button).setEnabled(true);
                        popupWindow.dismiss();
                    }
                });
                MaterialButton signUserButton = customView.findViewById(R.id.button_sign_user);
                Button buttonShowRewards = customView.findViewById(R.id.button_show_rewards);
                TextView userIdText = customView.findViewById(R.id.user_id);

                AccountManager am = AccountManager.get(getApplicationContext());
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
                final boolean isSignedIn = odyseeAccount != null;
                if (isSignedIn) {
                    userIdText.setVisibility(View.VISIBLE);
                    buttonShowRewards.setVisibility(View.VISIBLE);
                    signUserButton.setText("Sign out");
                    userIdText.setText(am.getUserData(odyseeAccount, "email"));
                    SharedPreferences sharedPref = getSharedPreferences("lbry_shared_preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("auth_token", Lbryio.AUTH_TOKEN);
                    editor.apply();
                } else {
                    userIdText.setVisibility(View.GONE);
                    userIdText.setText("");
                    buttonShowRewards.setVisibility(View.GONE);
                    signUserButton.setText(getString(R.string.sign_in));
                }

                buttonShowRewards.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        closeButton.performClick();
//                        hideNotifications();
//                        openFragment(RewardsFragment.class, true, null);
                        startActivity(new Intent(view.getContext(), ComingSoon.class));
                    }
                });
                signUserButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Close the popup window so its status gets updated when user opens it again
                        closeButton.performClick();
                        if (isSignedIn) {
                            signOutUser();
                        } else {
                            simpleSignIn(R.id.action_home_menu);
                        }
                    }
                });

                int height = findViewById(R.id.toolbar).getLayoutParams().height + 32;
                popupWindow.showAtLocation(findViewById(R.id.fragment_container_main_activity), Gravity.TOP|Gravity.END, 0, height);

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
                    showNotifications();
                } else {
                    hideNotifications();
                }
            }
        });

        notificationsSwipeContainer = findViewById(R.id.notifications_list_swipe_container);
        notificationsSwipeContainer.setColorSchemeResources(R.color.nextLbryGreen);
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
                if (nowPlayingClaim != null && !Helper.isNullOrEmpty(nowPlayingClaimUrl)) {
                    hideNotifications();
                    openFileUrl(nowPlayingClaimUrl);
                }
            }
        });

        accountManager = AccountManager.get(this);
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

    public void hideToolbar() {
        findViewById(R.id.toolbar).setVisibility(View.GONE);
    }
    private void updateMiniPlayerMargins() {
        // mini-player bottom margin setting
        /*SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int miniPlayerBottomMargin = Helper.parseInt(
                sp.getString(PREFERENCE_KEY_MINI_PLAYER_BOTTOM_MARGIN, String.valueOf(DEFAULT_MINI_PLAYER_MARGIN)), DEFAULT_MINI_PLAYER_MARGIN);
        */
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.miniplayer).getLayoutParams();
        int scaledMiniPlayerMargin = getScaledValue(DEFAULT_MINI_PLAYER_MARGIN);
        int scaledMiniPlayerBottomMargin = bottomNavigationHeight + getScaledValue(2);
        if (lp.leftMargin != scaledMiniPlayerMargin || lp.rightMargin != scaledMiniPlayerMargin || lp.bottomMargin != scaledMiniPlayerBottomMargin) {
            lp.setMargins(scaledMiniPlayerMargin, 0, scaledMiniPlayerMargin, scaledMiniPlayerBottomMargin);
        }
    }

    public boolean isDarkMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_DARK_MODE, false);
    }

    public boolean isBackgroundPlaybackEnabled() {
        return false; // TODO This is a workaround for audio keep playing after app is no longer on the foreground
//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
//        return sp.getBoolean(PREFERENCE_KEY_BACKGROUND_PLAYBACK, true);
    }

    public boolean isMediaAutoplayEnabled() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_MEDIA_AUTOPLAY, true);
    }

    public boolean initialSubscriptionMergeDone() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(PREFERENCE_KEY_INTERNAL_INITIAL_SUBSCRIPTION_MERGE_DONE, false);
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

    public void setActionBarTitle(int stringResourceId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            SpannableString spannable = new SpannableString(getString(stringResourceId));
            spannable.setSpan(new TypefaceSpan("inter"), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            actionBar.setTitle(spannable);
        }
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
//        openFragment(ChannelFormFragment.class, true, NavMenuItem.ID_ITEM_CHANNELS, params);
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
//        openFragment(PublishFormFragment.class, true, NavMenuItem.ID_ITEM_NEW_PUBLISH, params);
    }

    public void openChannelUrl(String url, String source) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        params.put("claim", getCachedClaimForUrl(url));
        if (!Helper.isNullOrEmpty(source)) {
            params.put("source", source);
        }
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

    private void openSpecialUrl(String url, String source) {
        String specialPath = url.substring(8).toLowerCase();
        if (specialRouteFragmentClassMap.containsKey(specialPath)) {
            Class fragmentClass = specialRouteFragmentClassMap.get(specialPath);
            if (fragmentClassNavIdMap.containsKey(fragmentClass)) {
                Map<String, Object> params = null;
                if (!Helper.isNullOrEmpty(source)) {
                    params = new HashMap<>();
                    params.put("source",  source);
                }

//                openFragment(specialRouteFragmentClassMap.get(specialPath), true, fragmentClassNavIdMap.get(fragmentClass), params);
            }
        }
    }

    public void openSendTo(String path) {
        Map<String, Object> params = new HashMap<>();
        params.put("directFilePath", path);
//        openFragment(PublishFormFragment.class, true, NavMenuItem.ID_ITEM_NEW_PUBLISH, params);
    }

    public void openFileClaim(Claim claim) {
        Map<String, Object> params = new HashMap<>();
        params.put("claimId", claim.getClaimId());
        params.put("url", !Helper.isNullOrEmpty(claim.getShortUrl()) ? claim.getShortUrl() : claim.getPermanentUrl());
        openFragment(FileViewFragment.class, true, params);
    }

    public void openRewards() {
        openFragment(RewardsFragment.class, true, null);
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

    private void renderPictureInPictureMode() {
        findViewById(R.id.main_activity_other_fragment).setVisibility(View.GONE);
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
        findViewById(R.id.miniplayer).setVisibility(View.GONE);
        findViewById(R.id.appbar).setVisibility(View.GONE);
        hideBottomNavigation();
        hideNotifications();
        hideActionBar();
        dismissActiveDialogs();

        View pipPlayerContainer = findViewById(R.id.pip_player_container);
        PlayerView pipPlayer = findViewById(R.id.pip_player);
        pipPlayer.setPlayer(appPlayer);
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

        findViewById(R.id.main_activity_other_fragment).setVisibility(View.GONE);
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
        findViewById(R.id.appbar).setVisibility(View.VISIBLE);
        showBottomNavigation();

        findViewById(R.id.content_main).setVisibility(View.GONE);
        Fragment fragment = getCurrentFragment();
        if (!(fragment instanceof FileViewFragment) && !inFullscreenMode && nowPlayingClaim != null) {
            findViewById(R.id.miniplayer).setVisibility(View.VISIBLE);
        }

        View pipPlayerContainer = findViewById(R.id.pip_player_container);
        PlayerView pipPlayer = findViewById(R.id.pip_player);
        pipPlayer.setPlayer(null);
        pipPlayerContainer.setVisibility(View.GONE);
        playerReassigned = true;
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
        if (appPlayer != null) {
            appPlayer.stop(true);
            appPlayer.release();
            appPlayer = null;
        }
        if (playerCache != null) {
            playerCache.release();
            playerCache = null;
        }
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

    public void updateWalletBalance() {
        if (isSignedIn()) {
            WalletBalanceTask task = new WalletBalanceTask(getAuthToken(), new WalletBalanceTask.WalletBalanceHandler() {
                @Override
                public void onSuccess(WalletBalance walletBalance) {
                    Lbry.walletBalance = walletBalance;
                    for (WalletBalanceListener listener : walletBalanceListeners) {
                        if (listener != null) {
                            listener.onWalletBalanceUpdated(walletBalance);
                        }
                    }
                    sendBroadcast(new Intent(ACTION_WALLET_BALANCE_UPDATED));
                    ((TextView) findViewById(R.id.floating_balance_value)).setText(Helper.shortCurrencyFormat(
                            Lbry.walletBalance == null ? 0 : Lbry.walletBalance.getTotal().doubleValue()));
                }

                @Override
                public void onError(Exception error) {
                    error.printStackTrace();
                    // pass
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Lbry.walletBalance = new WalletBalance();

            for (WalletBalanceListener listener : walletBalanceListeners) {
                if (listener != null) {
                    listener.onWalletBalanceUpdated(Lbry.walletBalance);
                }
            }
            sendBroadcast(new Intent(ACTION_WALLET_BALANCE_UPDATED));
            ((TextView) findViewById(R.id.floating_balance_value)).setText(Helper.shortCurrencyFormat(
                    Lbry.walletBalance == null ? 0 : Lbry.walletBalance.getTotal().doubleValue()));
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        checkPurchases();
        checkWebSocketClient();
        checkBottomNavigationHeight();
        updateMiniPlayerMargins();
        enteringPIPMode = false;

        checkFirstRun();
        checkNowPlaying();

        scheduleWalletBalanceUpdate();

        if (pendingSourceTabId != 0) {
            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            bottomNavigation.setSelectedItemId(pendingSourceTabId);
            pendingSourceTabId = 0;
        }

        initialiseUserInstall();
        // scheduleWalletSyncTask();
        // checkPendingOpens();
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
                            updateMiniPlayerMargins();
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

    public void checkPurchases() {
        if (billingClient != null) {
            Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getPurchasesList() != null) {
                for (Purchase purchase : result.getPurchasesList()) {
                    handlePurchase(purchase);
                }
            }
        }
    }

    public void checkPurchases(GenericTaskHandler handler) {
        boolean purchaseFound = false;
        if (billingClient != null) {
            Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getPurchasesList() != null) {
                for (Purchase purchase : result.getPurchasesList()) {
                    handlePurchase(purchase, handler);
                    purchaseFound = true;
                    return;
                }
            }
        }

        if (!purchaseFound) {
            handler.onError(new Exception(getString(R.string.skip_queue_purchase_not_found)));
        }
    }

    private void handlePurchase(Purchase purchase) {
        handleBillingPurchase(purchase, billingClient, MainActivity.this, null, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }
            }

            @Override
            public void onError(Exception error) {
                // pass
            }
        });
    }

    private void handlePurchase(Purchase purchase, GenericTaskHandler handler) {
        handleBillingPurchase(purchase, billingClient, MainActivity.this, null, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }

                if (handler != null) {
                    handler.onSuccess();
                }
            }

            @Override
            public void onError(Exception error) {
                if (handler != null) {
                    handler.onError(error);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        if (!enteringPIPMode && !inPictureInPictureMode && appPlayer != null && !isBackgroundPlaybackEnabled()) {
            appPlayer.setPlayWhenReady(false);
        }
        super.onPause();
    }

    public static void suspendGlobalPlayer(Context context) {
        if (MainActivity.appPlayer != null) {
            MainActivity.appPlayer.setPlayWhenReady(false);
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
        ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, null, new ClaimListResultHandler() {
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
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof FileViewFragment) {
            return;
        }

        // Don't show the toolbar when returning from the Share Activity
        if (getSupportFragmentManager().findFragmentByTag("FileView") == null)
            findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        if (nowPlayingClaim != null) {
            findViewById(R.id.miniplayer).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.global_now_playing_title)).setText(nowPlayingClaim.getTitle());
            ((TextView) findViewById(R.id.global_now_playing_channel_title)).setText(nowPlayingClaim.getPublisherTitle());
        }
        if (appPlayer != null) {
            // TODO This will reset player when changing tabs. See if it is needed
            PlayerView playerView = findViewById(R.id.global_now_playing_player_view);
            playerView.setPlayer(null);
            playerView.setPlayer(appPlayer);
            playerView.setUseController(false);
            playerReassigned = true;

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void hideGlobalNowPlaying() {
        findViewById(R.id.miniplayer).setVisibility(View.GONE);
    }

    public void unsetFitsSystemWindows(View view) {
        view.setFitsSystemWindows(false);
    }

    public void enterFullScreenMode() {
        inFullscreenMode = true;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        findViewById(R.id.appbar).setFitsSystemWindows(false);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public int getStatusBarHeight() {
        int height = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = getResources().getDimensionPixelSize(resourceId);
        }
        return height;
    }

    public void exitFullScreenMode() {
        View appBarMainContainer = findViewById(R.id.appbar);
        View decorView = getWindow().getDecorView();
        int flags = isDarkMode() ? (View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE) :
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE);

        if (!isDarkMode() && Build.VERSION.SDK_INT > LOLLIPOP_MR1)
            flags = flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

        appBarMainContainer.setFitsSystemWindows(false);
        decorView.setSystemUiVisibility(flags);
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRunCompleted = sp.getBoolean(PREFERENCE_KEY_INTERNAL_FIRST_RUN_COMPLETED, false);
        if (!firstRunCompleted) {
            startActivity(new Intent(this, FirstRunActivity.class));
            return;
        }

        if (!appStarted) {
            // first run completed, startup
            startup();
            return;
        }

        fetchRewards();
    }

    private void loadAuthToken() {
        // Check if an auth token is present and then set it for Lbryio
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String encryptedAuthToken = sp.getString(PREFERENCE_KEY_AUTH_TOKEN, null);
        if (!Helper.isNullOrEmpty(encryptedAuthToken)) {
            try {
                Lbryio.AUTH_TOKEN = new String(decrypt(Base64.decode(encryptedAuthToken, Base64.NO_WRAP), this, Lbry.KEYSTORE), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                // pass. A new auth token would have to be generated if the old one cannot be decrypted
                Log.e(TAG, "Could not decrypt existing auth token.", ex);
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
                    this,
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
        mediaSession = new MediaSessionCompat(getApplicationContext(), "LBRYMediaSession", mediaButtonReceiver, null);
        MediaSessionConnector connector = new MediaSessionConnector(mediaSession);
        connector.setPlayer(MainActivity.appPlayer);
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
            playerNotificationManager.setPlayer(MainActivity.appPlayer);
            if (mediaSession != null) {
                playerNotificationManager.setMediaSessionToken(mediaSession.getSessionToken());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREFERENCE_KEY_MINI_PLAYER_BOTTOM_MARGIN.equalsIgnoreCase(key)) {
            updateMiniPlayerMargins();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.actionMode = mode;
        if (isDarkMode()) {
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
        if (Build.VERSION.SDK_INT >= M && isDarkMode()) {
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
            scheduleWalletBalanceUpdate();
            loadRemoteNotifications(false);
        } else {
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.action_home_menu);

            if (scheduledWalletUpdater != null)
                scheduledWalletUpdater.cancel(true);

            updateWalletBalance();
        }

        if (initialCategoriesLoaded && userInstallInitialised) {
            hideLaunchScreen();
        }
    }

    private void initialiseUserInstall() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        if (!userInstallInitialised) {
            service.execute(new Runnable() {
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
                    android.util.Log.d(TAG, String.format("InstallationID: %s", Lbry.INSTALLATION_ID));

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

                    if (!initialCategoriesLoaded) {
                        loadInitialCategories();
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

    private void loadInitialCategories() {
        ExecutorService service = Executors.newSingleThreadExecutor();

        if (!initialCategoriesLoaded) {
            ContentSources.loadCategories(service, new ContentSources.CategoriesLoadedHandler() {
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
        // Animate?
        View launchSplash = findViewById(R.id.launch_splash);
        if (launchSplash.getVisibility() == View.VISIBLE) {
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
    }

    private class PlayerNotificationDescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {

        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return nowPlayingClaim != null ? nowPlayingClaim.getTitle() : "";
        }

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            if (nowPlayingClaimUrl != null) {
                Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(nowPlayingClaimUrl));
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return PendingIntent.getActivity(MainActivity.this, 0, launchIntent, 0);
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
                Glide.with(getApplicationContext()).asBitmap().load(nowPlayingClaim.getThumbnailUrl(0, 0, 75)).into(new CustomTarget<Bitmap>() {
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
        if (isSignedIn() && scheduler != null && (scheduledWalletUpdater == null || scheduledWalletUpdater.isDone() || scheduledWalletUpdater.isCancelled())) {
            scheduledWalletUpdater = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        updateWalletBalance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    private void scheduleWalletSyncTask() {
        if (scheduler != null && !walletSyncScheduled) {
            scheduler.scheduleAtFixedRate(new Runnable() {
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
        SaveSharedUserStateTask saveTask = new SaveSharedUserStateTask(new SaveSharedUserStateTask.SaveSharedUserStateHandler() {
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
            public void onSuccess(List<Subscription> subscriptions, List<Tag> followedTags) {
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
                            if (diff != null && diff.size() > 0) {
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

                    if (f!= null) {
                        if (!f.isSingleTagView() &&
                                f.getCurrentContentScope() == ContentScopeDialogFragment.ITEM_TAGS &&
                                !previousTags.equals(followedTags)) {
                            f.fetchClaimSearchContent(true);
                        }
                    }
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, String.format("load shared user state failed: %s", error != null ? error.getMessage() : "no error message"), error);
            }
        });
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean walletSyncEnabled = sp.getBoolean(PREFERENCE_KEY_INTERNAL_WALLET_SYNC_ENABLED, false);
        return walletSyncEnabled && Lbryio.isSignedIn();
    }

    public void syncSet(String hash, String data) {
        if (syncSetTask == null || syncSetTask.getStatus() == AsyncTask.Status.FINISHED) {
            syncSetTask = new SyncSetTask(Lbryio.lastRemoteHash, hash, data, new DefaultSyncTaskHandler() {
                @Override
                public void onSyncSetSuccess(String hash) {
                    Lbryio.lastRemoteHash = hash;
                    Lbryio.lastWalletSync = new WalletSync(hash, data);

                    if (pendingSyncSetQueue.size() > 0) {
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
                    if (pendingSyncSetQueue.size() > 0) {
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
            }

            @Override
            public void onSyncGetWalletNotFound() {
                // pass. This actually shouldn't happen at this point.
                // But if it does, send what we have
                if (Lbryio.isSignedIn() && userSyncEnabled()) {
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
                // store the value
                String encryptedAuthToken = intent.getStringExtra("authToken");
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                sp.edit().putString(PREFERENCE_KEY_AUTH_TOKEN, encryptedAuthToken).apply();
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

    public void showMessage(int stringResourceId) {
        Snackbar.make(findViewById(R.id.content_main), stringResourceId, Snackbar.LENGTH_LONG).show();
    }
    public void showMessage(String message) {
        Snackbar.make(findViewById(R.id.content_main), message, Snackbar.LENGTH_LONG).show();
    }
    public void showError(String message) {
        Snackbar.make(findViewById(R.id.content_main), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }

    public void showNotifications() {
        findViewById(R.id.content_main_container).setVisibility(View.GONE);
        findViewById(R.id.notifications_container).setVisibility(View.VISIBLE);
        findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
        hideBottomNavigation();
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
            if (unseenIds.size() > 0) {
                NotificationUpdateTask task = new NotificationUpdateTask(unseenIds, true);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }


        (new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DatabaseHelper.markNotificationsSeen(db);
                } catch (Exception ex) {
                    // pass
                }
                return null;
            }
            protected void onPostExecute(Void result) {
                loadUnseenNotificationsCount();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                findViewById(R.id.main_activity_other_fragment).setVisibility(View.GONE);
                findViewById(R.id.toolbar_balance_and_tools_layout).setVisibility(View.VISIBLE);
                showActionBar();
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        } else if (!enterPIPMode()) {
            // we're at the top of the stack
            if (isSearchUIActive()) {
                // Close Search UI
                getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentByTag("SEARCH")).commit();
                switchToolbarForSearch(false);
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);
                findViewById(R.id.fragment_container_main_activity).setVisibility(View.VISIBLE);
            } else {
                moveTaskToBack(true);
            }

            return;
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
        }
    }

    private boolean isSearchUIActive() {
        return getSupportFragmentManager().findFragmentByTag("SEARCH") != null;
    }

    public void signOutUser() {
        Lbryio.currentUser = null;
        Lbryio.AUTH_TOKEN = "";
        SharedPreferences sharedPref = getSharedPreferences("lbry_shared_preferences", Context.MODE_PRIVATE);
        sharedPref.edit().remove("auth_token").commit();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().remove(MainActivity.PREFERENCE_KEY_AUTH_TOKEN).apply();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            accountManager.removeAccountExplicitly(Helper.getOdyseeAccount(accountManager.getAccounts()));
        } else {
            accountManager.removeAccount(Helper.getOdyseeAccount(accountManager.getAccounts()), null, null);
        }

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            DatabaseHelper.clearNotifications(db);
            notificationListAdapter.clearNotifications();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isSignedIn() {
        AccountManager am = AccountManager.get(this);
        return Helper.getOdyseeAccount(am.getAccounts()) != null;
    }

    public void simpleSignIn(int sourceTabId) {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra("sourceTabId", sourceTabId);
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
//                openFragment(PublishFormFragment.class, true, NavMenuItem.ID_ITEM_NEW_PUBLISH, params);
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

                    // (light) fetch subscriptions
                    if (Lbryio.subscriptions.size() == 0) {
                        List<Subscription> subscriptions = new ArrayList<>();
                        List<String> subUrls = new ArrayList<>();
                        JSONArray array = (JSONArray) Lbryio.parseResponse(Lbryio.call("subscription", "list", context));
                        if (array != null) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject item = array.getJSONObject(i);
                                String claimId = item.getString("claim_id");
                                String channelName = item.getString("channel_name");
                                boolean isNotificationsDisabled = item.getBoolean("is_notifications_disabled");

                                LbryUri url = new LbryUri();
                                url.setChannelName(channelName);
                                url.setClaimId(claimId);
                                subscriptions.add(new Subscription(channelName, url.toString(), isNotificationsDisabled));
                                subUrls.add(url.toString());
                            }
                            Lbryio.subscriptions = subscriptions;
                            startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_LOADED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_LOADED, true));

                            // resolve subscriptions
                            if (subUrls.size() > 0 && Lbryio.cacheResolvedSubscriptions.size() != Lbryio.subscriptions.size()) {
                                Lbryio.cacheResolvedSubscriptions = Lbry.resolve(subUrls, Lbry.API_CONNECTION_STRING);
                            }
                            // if no exceptions occurred here, subscriptions have been loaded and resolved
                            startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED, true));
                        } else {
                            // user has not subscribed to anything
                            startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_LOADED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_LOADED, true));
                            startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED, true));
                        }
                    } else {
                        startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_LOADED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_LOADED, true));
                        startupStages.set(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED - 1, new StartupStage(STARTUP_STAGE_SUBSCRIPTIONS_RESOLVED, true));
                    }

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
        FetchRewardsTask task = new FetchRewardsTask(null, new FetchRewardsTask.FetchRewardsHandler() {
            @Override
            public void onSuccess(List<Reward> rewards) {
                Lbryio.updateRewardsLists(rewards);

                if (Lbryio.totalUnclaimedRewardAmount > 0) {
                    updateRewardsUsdValue();
                }
            }

            @Override
            public void onError(Exception error) {
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        if (appPlayer != null && appPlayer.isPlaying()) {
            enterPIPMode();
        }
    }

    protected boolean enterPIPMode() {
        if (enteringPIPMode) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                appPlayer != null &&
                !startingFilePickerActivity &&
                !startingSignInFlowActivity &&
                !isSearchUIActive()) {
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
        if (appPlayer != null) {
            appPlayer.setPlayWhenReady(false);
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
        if (appPlayer != null && inPictureInPictureMode && !isBackgroundPlaybackEnabled()) {
            appPlayer.setPlayWhenReady(false);
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
                    && ((BaseFragment) currentFragment).getParams().containsKey("source")
                    && ((BaseFragment) currentFragment).getParams().get("source").equals("notification")) {

                Map<String, Object> currentParams = new HashMap<>(1);

                if (((BaseFragment) currentFragment).getParams().containsKey("url"))
                    currentParams.put("url", ((BaseFragment) currentFragment).getParams().get("url"));

                ((BaseFragment) currentFragment).setParams(currentParams);
            }

            //fragment.setRetainInstance(true);
            FragmentManager manager = getSupportFragmentManager();

            if (fragment instanceof FileViewFragment || fragment instanceof ChannelFragment)
                findViewById(R.id.fragment_container_search).setVisibility(View.GONE);

            FragmentTransaction transaction;
            if (fragment instanceof FileViewFragment) {
                transaction = manager.beginTransaction().replace(R.id.main_activity_other_fragment, fragment, "FileView");
            } else {
                transaction = manager.beginTransaction().replace(R.id.main_activity_other_fragment, fragment);
            }
            if (allowNavigateBack) {
                transaction.addToBackStack(null);
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(!(fragment instanceof FileViewFragment) && allowNavigateBack);

            transaction.commit();
            findViewById(R.id.main_activity_other_fragment).setVisibility(View.VISIBLE);
            findViewById(R.id.fragment_container_main_activity).setVisibility(View.GONE);
            findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
            findViewById(R.id.toolbar_balance_and_tools_layout).setVisibility(View.GONE);
        } catch (Exception ex) {
            // pass
        }
    }

    public void fetchOwnChannels() {
        AccountManager am = AccountManager.get(this);
        ClaimListTask task = new ClaimListTask(Claim.TYPE_CHANNEL, null, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                Lbry.ownChannels = Helper.filterDeletedClaims(new ArrayList<>(claims));
                for (FetchChannelsListener listener : fetchChannelsListeners) {
                    listener.onChannelsFetched(claims);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e("FetchingChannels", "onError: ".concat(error.getLocalizedMessage()));
            }
        }, am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type"));
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void fetchOwnClaims() {
        ClaimListTask task = new ClaimListTask(Arrays.asList(Claim.TYPE_STREAM, Claim.TYPE_REPOST), null, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
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
            CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(task);
            completableFuture.thenAccept(result -> {
                unlockingTips = false;
            });
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

                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    Future<Boolean> future = executorService.submit(callable);

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
        (new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    return DatabaseHelper.getUnseenNotificationsCount(db);
                } catch (Exception ex) {
                    return 0;
                }
            }
            protected void onPostExecute(Integer count) {
                displayUnseenNotificationCount(count);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void loadRemoteNotifications(boolean markRead) {
        findViewById(R.id.notification_list_empty_container).setVisibility(View.GONE);
        NotificationListTask task = new NotificationListTask(this, findViewById(R.id.notifications_progress), new NotificationListTask.ListNotificationsHandler() {
            @Override
            public void onSuccess(List<LbryNotification> notifications) {
                remoteNotifcationsLastLoaded = new Date();

                loadUnseenNotificationsCount();
                loadLocalNotifications();
                if (markRead && findViewById(R.id.notifications_container).getVisibility() == View.VISIBLE) {
                    markNotificationsSeen();
                }

                if (notificationsSwipeContainer != null) {
                    notificationsSwipeContainer.setRefreshing(false);
                }
            }

            @Override
            public void onError(Exception exception) {
                // pass
                Log.e(TAG, "error loading remote notifications", exception);
                loadLocalNotifications();
                if (notificationsSwipeContainer != null) {
                    notificationsSwipeContainer.setRefreshing(false);
                }
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void loadLocalNotifications() {
        findViewById(R.id.notification_list_empty_container).setVisibility(View.GONE);
        findViewById(R.id.notifications_progress).setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<List<LbryNotification>> task = new GetLocalNotificationsSupplier();
            CompletableFuture<List<LbryNotification>> completableFuture = CompletableFuture.supplyAsync(task);
            completableFuture.thenAccept(n -> {
                updateLocalNotifications(n);
            });
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newSingleThreadExecutor();

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

                    Future<List<LbryNotification>> futureNotifications = executor.submit(callable);
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
        findViewById(R.id.notification_list_empty_container).setVisibility(notifications.size() == 0 ? View.VISIBLE : View.GONE);
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
                options.put("is_read", "true");

                AccountManager am = AccountManager.get(getApplicationContext());

                if (am != null) {
                    String at = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                    if (at != null)
                        options.put("auth_token", at);
                }

                if (Build.VERSION.SDK_INT > M) {
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
                markNotificationReadAndSeen(notification.getId());

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
        (new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                if (dbHelper != null) {
                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        DatabaseHelper.markNotificationReadAndSeen(notificationId, db);
                    } catch (Exception ex) {
                        // pass
                    }
                }
                return null;
            }
            protected void onPostExecute() {
                loadUnseenNotificationsCount();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void resolveCommentAuthors(List<String> urls) {
        if (urls != null && urls.size() > 0) {
            ResolveTask task = new ResolveTask(urls, Lbry.API_CONNECTION_STRING, null, new ClaimListResultHandler() {
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

    private void establishBillingClientConnection() {
        if (billingClient != null) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // no need to do anything here. purchases are always checked server-side
                        checkPurchases();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    establishBillingClientConnection();
                }
            });
        }
    }

    public static void handleBillingPurchase(
            Purchase purchase,
            BillingClient billingClient,
            Context context,
            View progressView,
            RewardVerifiedHandler handler) {
        String sku = purchase.getSku();
        if (SKU_SKIP.equalsIgnoreCase(sku)) {
            // send purchase token for verification
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                /*&& isSignatureValid(purchase)*/) {
                // consume the purchase
                String purchaseToken = purchase.getPurchaseToken();
                ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build();
                billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {

                    }
                });

                // send the purchase token to the backend to complete verification
                AndroidPurchaseTask task = new AndroidPurchaseTask(purchaseToken, progressView, context, handler);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    public interface BackPressInterceptor {
        boolean onBackPressed();
    }
}
