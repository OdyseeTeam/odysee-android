package com.odysee.app.ui.rewards;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.odysee.app.R;
import com.odysee.app.listener.VerificationListener;
import com.odysee.app.model.TwitterOauth;
import com.odysee.app.model.lbryinc.RewardVerified;
import com.odysee.app.tasks.RewardVerifiedHandler;
import com.odysee.app.tasks.TwitterOauthHandler;
import com.odysee.app.tasks.lbryinc.TwitterVerifyTask;
import com.odysee.app.tasks.verification.TwitterAccessTokenTask;
import com.odysee.app.tasks.verification.TwitterRequestTokenTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import lombok.Setter;

public class RewardVerificationTwitterFragment extends Fragment {
    @Setter
    private VerificationListener listener;
    private PopupWindow popup;
    private boolean twitterOauthInProgress;
    private MaterialButton buttonVerifyWithTwitter;
    private TwitterOauth currentOauth;
    private View mainView;
    private View loadingView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reward_verification_twitter, container, false);

        mainView = root.findViewById(R.id.reward_verification_twitter_main);
        loadingView = root.findViewById(R.id.reward_verification_twitter_loading);
        buttonVerifyWithTwitter = root.findViewById(R.id.reward_verification_twitter_button);
        buttonVerifyWithTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentOauth != null) {
                    // Twitter three-legged oauth already completed, verify directly
                    twitterVerify(currentOauth);
                } else {
                    // show twitter sign-in flow
                    twitterVerificationFlow();
                }
            }
        });

        return root;
    }

    private void twitterVerificationFlow() {
        twitterOauthInProgress = true;
        if (listener != null) {
            listener.onManualProgress(twitterOauthInProgress);
        }
        showLoading();
        String consumerKey = getResources().getString(R.string.TWITTER_CONSUMER_KEY);
        String consumerSecret = getResources().getString(R.string.TWITTER_CONSUMER_SECRET);
        TwitterRequestTokenTask task = new TwitterRequestTokenTask(consumerKey, consumerSecret, new TwitterOauthHandler() {
            @Override
            public void onSuccess(TwitterOauth twitterOauth) {
                twitterOauthInProgress = false;
                if (listener != null) {
                    listener.onManualProgress(twitterOauthInProgress);
                }
                showTwitterAuthenticateWithToken(twitterOauth.getOauthToken());
            }

            @Override
            public void onError(Exception error) {
                handleFlowError(null);
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public void showLoading() {
        Helper.setViewVisibility(mainView, View.GONE);
        Helper.setViewVisibility(loadingView, View.VISIBLE);
    }

    public void hideLoading() {
        Helper.setViewVisibility(mainView, View.VISIBLE);
        Helper.setViewVisibility(loadingView, View.GONE);
    }

    private void showTwitterAuthenticateWithToken(String oauthToken) {
        Context context = getContext();
        if (context != null) {
            WebView webView = new WebView(context);
            webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            webView.loadUrl(String.format("https://api.twitter.com/oauth/authorize?oauth_token=%s", oauthToken));
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("https://lbry.tv") || url.equalsIgnoreCase("https://twitter.com/home") /* Return to Twitter */) {
                        if (url.startsWith("https://lbry.tv") && url.contains("oauth_token") && url.contains("oauth_verifier")) {
                            // finish 3-legged oauth
                            twitterOauthInProgress = true;
                            listener.onManualProgress(twitterOauthInProgress);
                            finishTwitterOauth(url);
                        }

                        if (popup != null) {
                            popup.dismiss();
                        }
                        return false;
                    }

                    view.loadUrl(url);
                    return true;
                }
            });

            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_webview, null);
            ((LinearLayout) popupView.findViewById(R.id.popup_webivew_container)).addView(webView);
            popupView.findViewById(R.id.popup_cancel_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!twitterOauthInProgress && popup != null) {
                        popup.dismiss();
                        hideLoading();
                    }
                }
            });

            float scale = getResources().getDisplayMetrics().density;
            popup = new PopupWindow(context);
            popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (!twitterOauthInProgress) {
                        hideLoading();
                    }
                    popup = null;
                }
            });
            popup.setWidth(Helper.getScaledValue(340, scale));
            popup.setHeight(Helper.getScaledValue(480, scale));
            popup.setContentView(popupView);

            View parent = getView();
            popup.setFocusable(true);
            popup.showAtLocation(parent, Gravity.CENTER, 0, 0);
            popup.update();
        }
    }

    private void finishTwitterOauth(String callbackUrl) {
        String params = callbackUrl.substring(callbackUrl.indexOf('?') + 1);
        TwitterAccessTokenTask task = new TwitterAccessTokenTask(params, new TwitterOauthHandler() {
            @Override
            public void onSuccess(TwitterOauth twitterOauth) {
                // send request to finish verifying
                currentOauth = twitterOauth;
                twitterVerify(twitterOauth);
            }

            @Override
            public void onError(Exception error) {
                handleFlowError(null);
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void twitterVerify(TwitterOauth twitterOauth) {
        Context context = getContext();
        if (context != null) {
            showLoading();
            twitterOauthInProgress = true;
            if (listener != null) {
                listener.onManualProgress(twitterOauthInProgress);
            }

            TwitterVerifyTask task = new TwitterVerifyTask(twitterOauth, null, context, new RewardVerifiedHandler() {
                @Override
                public void onSuccess(RewardVerified rewardVerified) {
                    twitterOauthInProgress = false;
                    if (listener != null) {
                        listener.onManualProgress(twitterOauthInProgress);
                    }

                    if (Lbryio.currentUser != null) {
                        Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                    }
                    if (rewardVerified.isRewardApproved()) {
                        if (listener != null) {
                            listener.onTwitterVerified();
                        }
                    } else {
                        View root = getView();
                        if (root != null) {
                            // reward approved wasn't set to true
                            Snackbar.make(root, getString(R.string.twitter_verification_not_approved), Snackbar.LENGTH_LONG).
                                    setTextColor(Color.WHITE).
                                    setBackgroundTint(Color.RED).show();
                        }
                        hideLoading();
                    }
                }

                @Override
                public void onError(Exception error) {
                    handleFlowError(error != null ? error.getMessage() : null);
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            twitterOauthInProgress = false;
            if (listener != null) {
                listener.onManualProgress(twitterOauthInProgress);
            }
            hideLoading();
        }
    }

    private void handleFlowError(String extra) {
        hideLoading();
        twitterOauthInProgress = false;
        if (listener != null) {
            listener.onManualProgress(twitterOauthInProgress);
        }
        showFlowError(extra);
    }

    private void showError(String message) {
        Context context = getContext();
        View root = getView();
        if (context != null && root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).
                    setTextColor(Color.WHITE).
                    setBackgroundTint(Color.RED).show();
        }
    }

    private void showFlowError(String extra) {
        Context context = getContext();
        View root = getView();
        if (context != null && root != null) {
            String message = !Helper.isNullOrEmpty(extra) ?
                    getString(R.string.twitter_account_ineligible, extra) :
                    getString(R.string.twitter_verification_failed);

            Snackbar.make(root, message, Snackbar.LENGTH_LONG).
                    setTextColor(Color.WHITE).
                    setBackgroundTint(Color.RED).show();
        }
    }
}
