package com.odysee.app;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;

import com.odysee.app.adapter.VerificationPagerAdapter;
import com.odysee.app.listener.VerificationListener;
import com.odysee.app.listener.WalletSyncListener;
import com.odysee.app.model.lbryinc.RewardVerified;
import com.odysee.app.model.lbryinc.User;
import com.odysee.app.tasks.RewardVerifiedHandler;
import com.odysee.app.tasks.lbryinc.FetchCurrentUserTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.utils.VerificationSkipQueue;

public class VerificationActivity extends FragmentActivity implements VerificationListener, WalletSyncListener {

    public static final int VERIFICATION_FLOW_SIGN_IN = 1;
    public static final int VERIFICATION_FLOW_REWARDS = 2;
    public static final int VERIFICATION_FLOW_WALLET = 3;

    private VerificationSkipQueue verificationSkipQueue;
    private BroadcastReceiver sdkReceiver;
    private String email;
    private boolean signedIn;
    private int flow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change status bar text color depending on Night mode when app is running
        String darkModeAppSetting = ((OdyseeApp) getApplication()).getDarkModeAppSetting();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_NIGHT) && AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                //noinspection deprecation
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else {
            int defaultNight = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_NOTNIGHT) || (darkModeAppSetting.equals(MainActivity.APP_SETTING_DARK_MODE_SYSTEM) && defaultNight == Configuration.UI_MODE_NIGHT_NO)) {
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


        verificationSkipQueue = new VerificationSkipQueue(this, new VerificationSkipQueue.ShowInProgressListener() {
            @Override
            public void maybeShowRequestInProgress() {
                showLoading();
            }
        }, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }

                if (!rewardVerified.isRewardApproved()) {
                    // show pending purchase message (possible slow card tx)
                    Snackbar.make(findViewById(R.id.verification_pager), R.string.purchase_request_pending, Snackbar.LENGTH_LONG).show();
                } else  {
                    Snackbar.make(findViewById(R.id.verification_pager), R.string.reward_verification_successful, Snackbar.LENGTH_LONG).show();
                }

                setResult(RESULT_OK);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 3000);
            }

            @Override
            public void onError(Exception error) {
                showFetchUserError(getString(R.string.purchase_request_failed_error));
                hideLoading();
            }
        });
        verificationSkipQueue.createBillingClientAndEstablishConnection();

        signedIn = Lbryio.isSignedIn();
        Intent intent = getIntent();
        if (intent != null) {
            flow = intent.getIntExtra("flow", -1);
            if (flow == -1 || (flow == VERIFICATION_FLOW_SIGN_IN && signedIn)) {
                // no flow specified (or user is already signed in), just exit
                setResult(signedIn ? RESULT_OK : RESULT_CANCELED);
                finish();
                return;
            }
        }

        if (!Arrays.asList(VERIFICATION_FLOW_SIGN_IN, VERIFICATION_FLOW_REWARDS, VERIFICATION_FLOW_WALLET).contains(flow)) {
            // invalid flow specified
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        IntentFilter filter = new IntentFilter();

        setContentView(R.layout.activity_verification);
        ViewPager2 viewPager = findViewById(R.id.verification_pager);
        viewPager.setUserInputEnabled(false);
        viewPager.setSaveEnabled(false);
        viewPager.setAdapter(new VerificationPagerAdapter(this));

        findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
        findViewById(R.id.verification_close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    public void onResume() {
        super.onResume();
        LbryAnalytics.setCurrentScreen(this, "Verification", "Verification");
        checkFlow();
    }

    public void checkFlow() {
        ViewPager2 viewPager = findViewById(R.id.verification_pager);
        if (Lbryio.isSignedIn()) {
            boolean flowHandled = false;
            if (flow == VERIFICATION_FLOW_WALLET) {
                viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_WALLET, false);
                flowHandled = true;
            } else if (flow == VERIFICATION_FLOW_REWARDS) {
                User user = Lbryio.currentUser;
                // disable phone verification for now
                if (!user.isIdentityVerified()) {
                    // phone number verification required
                    viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_PHONE, false);
                    flowHandled = true;
                } else {
                    if (!user.isRewardApproved()) {
                        // manual verification required
                        viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_MANUAL, false);
                        flowHandled = true;
                    }
                }
            }

            if (!flowHandled) {
                // user has already been verified and or reward approved
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
    }

    public void showPhoneVerification() {
        ViewPager2 viewPager = findViewById(R.id.verification_pager);
        viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_PHONE, false);
    }

    public void showLoading() {
        findViewById(R.id.verification_loading_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.verification_pager).setVisibility(View.INVISIBLE);
        findViewById(R.id.verification_close_button).setVisibility(View.GONE);
    }

    public void hideLoading() {
        findViewById(R.id.verification_loading_progress).setVisibility(View.GONE);
        findViewById(R.id.verification_pager).setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        ViewPager2 viewPager = findViewById(R.id.verification_pager);

        if (viewPager.getCurrentItem() != VerificationPagerAdapter.PAGE_VERIFICATION_MANUAL)
            viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_MANUAL);
        else
            super.onBackPressed();
    }

    public void onEmailAdded(String email) {
        this.email = email;
        findViewById(R.id.verification_close_button).setVisibility(View.GONE);

        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_EMAIL_ADDED, bundle);
    }
    public void onEmailEdit() {
        findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
    }
    public void onEmailVerified() {
        Snackbar.make(findViewById(R.id.verification_pager), R.string.sign_in_successful, Snackbar.LENGTH_LONG).show();
        sendBroadcast(new Intent(MainActivity.ACTION_USER_SIGN_IN_SUCCESS));

        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        LbryAnalytics.logEvent(LbryAnalytics.EVENT_EMAIL_VERIFIED, bundle);
        finish();

        if (flow == VERIFICATION_FLOW_SIGN_IN) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra("flow", VERIFICATION_FLOW_SIGN_IN);
            resultIntent.putExtra("email", email);

            // only sign in required, don't do anything else
            showLoading();
            FetchCurrentUserTask task = new FetchCurrentUserTask(this, new FetchCurrentUserTask.FetchUserTaskHandler() {
                @Override
                public void onSuccess(User user) {
                    Lbryio.currentUser = user;
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    showFetchUserError(error != null ? error.getMessage() : getString(R.string.fetch_current_user_error));
                    hideLoading();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            // change pager view depending on flow
            showLoading();
            FetchCurrentUserTask task = new FetchCurrentUserTask(this, new FetchCurrentUserTask.FetchUserTaskHandler() {
                @Override
                public void onSuccess(User user) {
                    hideLoading();
                    findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);

                    Lbryio.currentUser = user;
                    ViewPager2 viewPager = findViewById(R.id.verification_pager);
                    // for rewards, (show phone verification if not done, or manual verification if required)
                    if (flow == VERIFICATION_FLOW_REWARDS) {
                        if (!user.isIdentityVerified()) {
                            // phone number verification required
                            viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_PHONE, false);
                        } else {
                            if (!user.isRewardApproved()) {
                                // manual verification required
                                viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_MANUAL, false);
                            } else {
                                // fully verified
                                setResult(RESULT_OK);
                                finish();
                            }
                        }
                    }                }
                @Override
                public void onError(Exception error) {
                    showFetchUserError(error != null ? error.getMessage() : getString(R.string.fetch_current_user_error));
                    hideLoading();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onPhoneAdded(String countryCode, String phoneNumber) {

    }

    @Override
    public void onPhoneVerified() {
        showLoading();
        FetchCurrentUserTask task = new FetchCurrentUserTask(this, new FetchCurrentUserTask.FetchUserTaskHandler() {
            @Override
            public void onSuccess(User user) {
                Lbryio.currentUser = user;
                if (user.isIdentityVerified() && user.isRewardApproved()) {
                    // verified for rewards
                    LbryAnalytics.logEvent(LbryAnalytics.EVENT_REWARD_ELIGIBILITY_COMPLETED);

                    setResult(RESULT_OK);
                    finish();
                    return;
                }

                findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
                // show manual verification page if the user is still not reward approved
                ViewPager2 viewPager = findViewById(R.id.verification_pager);
                viewPager.setCurrentItem(VerificationPagerAdapter.PAGE_VERIFICATION_MANUAL, false);
                hideLoading();
            }

            @Override
            public void onError(Exception error) {
                showFetchUserError(error != null ? error.getMessage() : getString(R.string.fetch_current_user_error));
                hideLoading();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showFetchUserError(String message) {
        Snackbar.make(findViewById(R.id.verification_pager), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
    }

    @Override
    public void onManualVerifyContinue() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onWalletSyncProcessing() {
        findViewById(R.id.verification_close_button).setVisibility(View.GONE);
    }
    @Override
    public void onWalletSyncWaitingForInput() {
        findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onWalletSyncEnabled() {
        findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onWalletSyncFailed(Exception error) {
        findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onSkipQueueAction() {
        verificationSkipQueue.onSkipQueueAction(VerificationActivity.this);
    }

    @Override
    public void onTwitterVerified() {
        Snackbar.make(findViewById(R.id.verification_pager), R.string.reward_verification_successful, Snackbar.LENGTH_LONG).show();

        setResult(RESULT_OK);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3000);
    }

    @Override
    public void onManualProgress(boolean progress) {
        if (progress) {
            findViewById(R.id.verification_close_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.verification_close_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        Helper.unregisterReceiver(sdkReceiver, this);
        super.onDestroy();
    }
}
