package com.odysee.app;

import static android.os.Build.VERSION_CODES.M;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.listener.YouTubeSyncListener;
import com.odysee.app.model.YouTubeSyncItem;
import com.odysee.app.ui.ytsync.YouTubeSyncSetupFragment;
import com.odysee.app.ui.ytsync.YouTubeSyncStatusFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.SneakyThrows;

public class YouTubeSyncActivity extends AppCompatActivity implements YouTubeSyncListener {
    private static final String RETURN_URL = "https://odysee.com/ytsync";

    private boolean oauthInProgress;
    private ViewPager2 viewPager;
    private PopupWindow popup;

    private ProgressBar mainProgress;
    private View skip;
    private View inputSource;
    private View eventSource;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        if (Build.VERSION.SDK_INT >= M && !isDarkMode()) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(R.layout.activity_youtube_sync);

        mainProgress = findViewById(R.id.youtube_sync_main_progress);
        viewPager = findViewById(R.id.youtube_sync_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setSaveEnabled(false);
        viewPager.setAdapter(new YouTubeSyncPagerAdapter(this));
    }

    public void onResume() {
        super.onResume();
        if (!oauthInProgress) {
            checkStatus();
        }
    }

    public boolean isDarkMode() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(MainActivity.PREFERENCE_KEY_DARK_MODE, false);
    }

    private void checkStatus() {
        viewPager.setVisibility(View.INVISIBLE);
        mainProgress.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray status = (JSONArray) Lbryio.parseResponse(Lbryio.call("yt", "transfer", null, Helper.METHOD_POST, null));
                    if (status.length() > 0 && containsTransferableChannel(status)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setFirstYouTubeSyncDone();
                                viewPager.setCurrentItem(1); // show the status page
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainProgress.setVisibility(View.GONE);
                                        viewPager.setVisibility(View.VISIBLE);
                                    }
                                }, 500);
                            }
                        });
                        return;
                    }

                    // no transferable channels, show setup screen
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // pass
                            mainProgress.setVisibility(View.GONE);
                            viewPager.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // pass
                            mainProgress.setVisibility(View.GONE);
                            viewPager.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    private boolean containsTransferableChannel(JSONArray status) {
        try {
            for (int i = 0; i < status.length(); i++) {
                JSONObject object = status.getJSONObject(i);
                Type type = new TypeToken<YouTubeSyncItem>() {}.getType();
                Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
                YouTubeSyncItem item = gson.fromJson(object.toString(), type);

                YouTubeSyncItem.Channel channel = item.getChannel();
                String transferState = channel.getTransferState();
                boolean transferable = !YouTubeSyncStatusFragment.TRANSFER_STATE_COMPLETED_TRANSFER.equalsIgnoreCase(transferState) &&
                        !YouTubeSyncStatusFragment.TRANSFER_STATE_TRANSFERRED.equalsIgnoreCase(transferState);
                if (transferable) {
                    return true;
                }
            }
        } catch (JSONException ex) {
            // pass
        }

        return false;
    }

    @Override
    public void onSkipPressed() {
        onDonePressed();
    }

    @Override
    public void onNewSyncPressed() {
        viewPager.setCurrentItem(0);
    }

    @Override
    public void onClaimNowPressed(String channelName, View skip, View inputSource, View eventSource, View progress) {
        // start request
        this.skip = skip;
        this.inputSource = inputSource;
        this.eventSource = eventSource;
        this.progress = progress;

        if (!channelName.startsWith("@")) {
            channelName = String.format("@%s", channelName);
        }

        if (Lbry.ownChannels.contains(channelName)) {
            showError(getString(R.string.channel_name_already_created));
            return;
        }

        Helper.setViewEnabled(inputSource, false);
        Helper.setViewVisibility(skip, View.INVISIBLE);
        Helper.setViewVisibility(eventSource, View.INVISIBLE);
        Helper.setViewVisibility(progress, View.VISIBLE);

        final String desiredChannelName = channelName;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> options = new HashMap<>();
                    options.put("type", "sync");
                    options.put("immediate_sync", "true");
                    options.put("desired_lbry_channel_name", desiredChannelName);
                    options.put("return_url", RETURN_URL);
                    String oauthUrl = (String) Lbryio.parseResponse(Lbryio.call("yt", "new", options, null));
                    onOauthRequestCompleted(oauthUrl);
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    showError(ex.getMessage());
                    onOauthRequestFailed();
                }
            }
        });
    }

    private void onOauthRequestCompleted(String oauthUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showOauthWebView(oauthUrl);
            }
        });
    }

    private void onOauthRequestFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                restoreSetupControls();
            }
        });
    }

    private void restoreSetupControls() {
        Helper.setViewEnabled(inputSource, true);
        Helper.setViewVisibility(skip, View.VISIBLE);
        Helper.setViewVisibility(eventSource, View.VISIBLE);
        Helper.setViewVisibility(progress, View.GONE);
    }

    @Override
    public void onDonePressed() { // use odysee
        setFirstYouTubeSyncDone();
        finish();
    }

    private void setFirstYouTubeSyncDone() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean(MainActivity.PREFERENCE_KEY_INTERNAL_FIRST_YOUTUBE_SYNC_DONE, true).apply();
    }

    public void showError(String message) {
        Snackbar.make(findViewById(R.id.youtube_sync_main), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }

    private void finishYouTubeSyncSetup() {
        // YouTube sync connection successful, change to status page
        setFirstYouTubeSyncDone();
        viewPager.setCurrentItem(1);
    }

    private void showOauthWebView(String oauthUrl) {
        WebView webView = new WebView(this);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 12) Chrome/97.0.4692.98");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        webView.loadUrl(oauthUrl);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(RETURN_URL) || url.equalsIgnoreCase(RETURN_URL)) {
                    finishYouTubeSyncSetup();
                    if (popup != null) {
                        popup.dismiss();
                    }
                    oauthInProgress = false;
                    return false;
                }

                view.loadUrl(url);
                return true;
            }
        });

        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_webview, null);
        ((LinearLayout) popupView.findViewById(R.id.popup_webivew_container)).addView(webView);
        popupView.findViewById(R.id.popup_cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (popup != null) {
                    popup.dismiss();
                }
                oauthInProgress = false;
                restoreSetupControls();
            }
        });

        float scale = getResources().getDisplayMetrics().density;
        popup = new PopupWindow(this);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popup = null;
                oauthInProgress = false;
                restoreSetupControls();
            }
        });
        popup.setWidth(Helper.getScaledValue(340, scale));
        popup.setHeight(Helper.getScaledValue(480, scale));
        popup.setContentView(popupView);

        View parent = findViewById(R.id.youtube_sync_main);
        popup.setFocusable(true);
        popup.showAtLocation(parent, Gravity.CENTER, 0, 0);
        popup.update();
        oauthInProgress = true;
    }

    private static class YouTubeSyncPagerAdapter extends FragmentStateAdapter {
        private final FragmentActivity activity;

        public YouTubeSyncPagerAdapter(FragmentActivity activity) {
            super(activity);
            this.activity = activity;
        }

        @NonNull
        @Override
        @SneakyThrows
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                default:
                    YouTubeSyncSetupFragment setupFragment = YouTubeSyncSetupFragment.class.newInstance();
                    if (activity instanceof YouTubeSyncListener) {
                        setupFragment.setListener((YouTubeSyncListener) activity);
                    }
                    return setupFragment;
                case 1:
                    YouTubeSyncStatusFragment statusFragment = YouTubeSyncStatusFragment.class.newInstance();
                    if (activity instanceof YouTubeSyncListener) {
                        statusFragment.setListener((YouTubeSyncListener) activity);
                    }
                    return statusFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

}
