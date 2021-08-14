package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.odysee.app.callable.UserExistsWithPassword;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.User;
import com.odysee.app.tasks.verification.CheckUserEmailVerifiedTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.odysee.app.utils.Lbryio.TAG;

public class SignInActivity extends Activity {
    public static final String ACTION_USER_FINISHED_SIGN_IN = "com.odysee.app.USER_SIGNED_IN_SUCCESSFULLY";
    public final static String ARG_ACCOUNT_TYPE = "com.odysee";
    public final static String ARG_AUTH_TYPE = "auth_token_type";

    private TextInputLayout layoutPassword;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private ProgressBar activityProgress;
    private View layoutCollect;
    private View layoutVerify;
    private TextView textAddedEmail;

    private String currentEmail;
    private ScheduledExecutorService emailVerifyCheckScheduler;
    private ExecutorService executor;
    private int sourceTabId;

    private boolean requestInProgress;
    private ImageButton closeSignupSignIn;
    private MaterialButton buttonPrimary;
    private MaterialButton buttonSecondary;
    private boolean signInMode;
    private boolean emailSignInChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        executor = Executors.newSingleThreadExecutor();

        layoutCollect = findViewById(R.id.signin_form);
        layoutVerify = findViewById(R.id.verification_email_verify_container);
        textAddedEmail = findViewById(R.id.verification_email_added_address);
        layoutPassword = findViewById(R.id.layout_signin_password);
        inputEmail = findViewById(R.id.verification_email_input);
        inputPassword = findViewById(R.id.signin_password);
        activityProgress = findViewById(R.id.signin_activity_progress);
        closeSignupSignIn = findViewById(R.id.signin_close);

        buttonPrimary = findViewById(R.id.button_primary);
        buttonSecondary = findViewById(R.id.button_secondary);

        buttonSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInMode = !signInMode;
                buttonPrimary.setText(signInMode ? R.string.continue_text : R.string.sign_up);
                buttonSecondary.setText(signInMode ? R.string.sign_up : R.string.sign_in);
                layoutPassword.setVisibility(signInMode ? View.GONE : View.VISIBLE);
                inputPassword.setText("");
            }
        });

        TextView agreeToTerms = findViewById(R.id.agree_to_terms_note);
        agreeToTerms.setMovementMethod(LinkMovementMethod.getInstance());

        buttonPrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpOrSignIn();
            }
        });

        findViewById(R.id.verification_email_resend_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleEmailVerificationFlow(currentEmail);
            }
        });

        ImageButton closeButton = findViewById(R.id.signin_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishSignInActivity();
            }
        });

        View buttonEdit = findViewById(R.id.verification_email_edit_button);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editEmail();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            sourceTabId = intent.getIntExtra("sourceTabId", R.id.action_home_menu);
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutVerify.getVisibility() != View.VISIBLE) {
            finishSignInActivity();
            super.onBackPressed();
        } else {
            editEmail();
        }
    }

    private boolean checkUserExistsWithPassword(String email) {
        Callable<Boolean> callable = new UserExistsWithPassword(getApplicationContext(), email);
        Future<Boolean> future = executor.submit(callable);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "checkUserExistsWithPassword: ".concat(e.getLocalizedMessage()));
        }
        return false;
    }

    private void performSignIn(final String email, final String password) {
        if (requestInProgress) {
            return;
        }

        TransitionManager.beginDelayedTransition(findViewById(R.id.signin_buttons));
        findViewById(R.id.signin_buttons).setVisibility(View.GONE);

        activityProgress.setVisibility(View.VISIBLE);
        if (!emailSignInChecked) {
            requestInProgress = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Map<String, String> options = new HashMap<>();
                    options.put("email", email);
                    try {
                        Object response = Lbryio.parseResponse(Lbryio.call(
                                "user", "exists", options, Helper.METHOD_POST, SignInActivity.this));

                        requestInProgress = false;
                        if (response instanceof JSONObject) {
                            JSONObject json = (JSONObject) response;
                            boolean hasPassword = Helper.getJSONBoolean("has_password", false, json);
                            if (!hasPassword) {
                                setCurrentEmail(email);
                                handleUserSignInWithoutPassword(email);
                                return;
                            }

                            // email exists, we can use sign in flow. Show password field
                            setCurrentEmail(email);
                            emailSignInChecked = true;
                            displaySignInControls();
                        }
                    } catch (LbryioRequestException | LbryioResponseException ex) {
                        if (ex instanceof LbryioResponseException) {
                            LbryioResponseException rex = (LbryioResponseException) ex;
                            if (rex.getStatusCode() == 412) {
                                // old email verification flow
                                setCurrentEmail(email);
                                handleUserSignInWithoutPassword(email);

                                Bundle bundle = new Bundle();
                                bundle.putString("email", currentEmail);
                                LbryAnalytics.logEvent(LbryAnalytics.EVENT_EMAIL_ADDED, bundle);
                                LbryAnalytics.logEvent("");

                                requestInProgress = false;
                                return;
                            }
                        }

                        requestInProgress = false;
                        restoreControls(true);
                        showError(ex.getMessage());
                    }
                }
            });

            return;
        }

        if (Helper.isNullOrEmpty(password)) {
            showError(getString(R.string.please_enter_signin_password));
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> options = new HashMap<>();
                options.put("email", email);
                options.put("password", password);
                try {
                    Object response = Lbryio.parseResponse(Lbryio.call("user", "signin", options, Helper.METHOD_POST, SignInActivity.this));
                    requestInProgress = false;
                    restoreControls(false);
                    if (response instanceof JSONObject) {
                        Type type = new TypeToken<User>(){}.getType();
                        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
                        User user = gson.fromJson(response.toString(), type);
                        Lbryio.currentUser = user;

                        addOdyseeAccountExplicitly(user.getPrimaryEmail());

                        finishWithWalletSync();
                        return;
                    }
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    if (ex instanceof LbryioResponseException) {
                        if (((LbryioResponseException) ex).getStatusCode() == 409) {
                            handleEmailVerificationFlow(email);
                            return;
                        }
                    }
                }

                showError(getString(R.string.unknown_error_occurred));
            }
        });
    }

    private void disableVerificationControls() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.verification_email_resend_button).setEnabled(false);
                findViewById(R.id.verification_email_edit_button).setEnabled(false);
            }
        });
    }

    private void enableVerificationControls() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.verification_email_resend_button).setEnabled(true);
                findViewById(R.id.verification_email_edit_button).setEnabled(true);
            }
        });
    }

    private void handleEmailVerificationFlow(final String email) {
        requestInProgress = true;
        disableVerificationControls();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> options = new HashMap<>();
                options.put("email", email);
                options.put("only_if_expired", "true");
                try {
                    Object response = Lbryio.parseResponse(Lbryio.call("user_email", "resend_token", options, Helper.METHOD_POST, SignInActivity.this));
                    requestInProgress = false;
                    enableVerificationControls();
                    waitForVerification();
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    requestInProgress = false;
                    enableVerificationControls();
                    showError(ex.getMessage());
                }
            }
        });
    }

    private void displaySignInControls() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                inputPassword.setVisibility(View.VISIBLE);
                buttonPrimary.setText(R.string.sign_in);
            }
        });
    }

    private void handleUserSignInWithoutPassword(String email) {
        requestInProgress = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> options = new HashMap<>();
                options.put("email", email);
                options.put("send_verification_email", "true");
                try {
                    Object response = Lbryio.parseResponse(Lbryio.call("user_email", "new", options, Helper.METHOD_POST,SignInActivity.this));
                    requestInProgress = false;
                    waitForVerification();
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    requestInProgress = false;
                    if (ex instanceof LbryioResponseException) {
                        if (((LbryioResponseException) ex).getStatusCode() == 409) {
                            handleEmailVerificationFlow(email);
                            return;
                        }
                    }
                    restoreControls(true);
                    showError(ex.getMessage());
                }
            }
        });
    }

    private void setCurrentEmail(String email) {
        this.currentEmail = email;
        ((TextView) findViewById(R.id.verification_email_added_address)).setText(currentEmail);
    }

    private void signUpOrSignIn() {
        currentEmail = Helper.getValue(inputEmail.getText());
        String password = Helper.getValue(inputPassword.getText());
        if (Helper.isNullOrEmpty(currentEmail) || !currentEmail.contains("@")) {
            View view = findViewById(R.id.verification_email_collect_container);
            if (view != null) {
                Snackbar.make(view, R.string.provide_valid_email, Snackbar.LENGTH_LONG).
                        setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
            }
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputEmail.getWindowToken(), 0);

        if (!signInMode) {
            // sign up
            handleUserSignUp(currentEmail, password);
            return;
        }

        performSignIn(currentEmail, password);
    }

    private void updateControlsBeforeRequest() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                inputEmail.setEnabled(false);
                inputPassword.setEnabled(false);
                buttonPrimary.setEnabled(false);
                buttonSecondary.setEnabled(false);
                activityProgress.setVisibility(View.VISIBLE);
                closeSignupSignIn.setVisibility(View.GONE);

                findViewById(R.id.signin_buttons).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void restoreControls(final boolean showClose) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                inputEmail.setEnabled(true);
                inputPassword.setEnabled(true);
                buttonPrimary.setEnabled(true);
                buttonSecondary.setEnabled(true);
                activityProgress.setVisibility(View.INVISIBLE);

                closeSignupSignIn.setVisibility(showClose ? View.VISIBLE : View.GONE);
                findViewById(R.id.signin_buttons).setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleUserSignUp(final String email, final String password) {
        if (requestInProgress) {
            return;
        }
        if (Helper.isNullOrEmpty(email) || Helper.isNullOrEmpty(password)) {
            showError(getString(R.string.provide_valid_email_password));
            return;
        }

        updateControlsBeforeRequest();
        requestInProgress = true;

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> options = new HashMap<>();
                    options.put("email", email);
                    options.put("password", password);
                    Object response = Lbryio.parseResponse(Lbryio.call("user", "signup", options, Helper.METHOD_POST,SignInActivity.this));
                    if (response != null && "ok".equalsIgnoreCase(response.toString())) {
                        // log email added
                        Bundle bundle = new Bundle();
                        bundle.putString("email", currentEmail);
                        LbryAnalytics.logEvent(LbryAnalytics.EVENT_EMAIL_ADDED, bundle);

                        // wait for verification
                        waitForVerification();
                        restoreControls(false);
                        return;
                    }

                    requestInProgress = false;
                    restoreControls(false);
                    showError(getString(R.string.signup_error_occurred));
                } catch (LbryioRequestException | LbryioResponseException ex) {
                    requestInProgress = false;
                    restoreControls(true);
                    showError(getString(R.string.signup_failed, ex.getMessage()));
                }
            }
        });
    }

    private void waitForVerification() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                layoutCollect.setVisibility(View.GONE);
                layoutVerify.setVisibility(View.VISIBLE);

                scheduleEmailVerify();
            }
        });
    }

    private void showError(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(findViewById(R.id.verification_activity), message, Snackbar.LENGTH_LONG).
                        setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
            }
        });
    }

    private void scheduleEmailVerify() {
        emailVerifyCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        emailVerifyCheckScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                checkEmailVerified();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void checkEmailVerified() {
        CheckUserEmailVerifiedTask task = new CheckUserEmailVerifiedTask(this, new CheckUserEmailVerifiedTask.CheckUserEmailVerifiedHandler() {
            @Override
            public void onUserEmailVerified() {
                layoutCollect.setVisibility(View.GONE);
                layoutVerify.setVisibility(View.GONE);
                if (emailVerifyCheckScheduler != null) {
                    emailVerifyCheckScheduler.shutdownNow();
                    emailVerifyCheckScheduler = null;
                }

                addOdyseeAccountExplicitly(currentEmail);

                // send broadcast to indicate the user finished sign in
                Intent intent = new Intent(ACTION_USER_FINISHED_SIGN_IN);
                intent.putExtra("sourceTabId", sourceTabId);
                LocalBroadcastManager.getInstance(SignInActivity.this).sendBroadcast(intent);

                // perform wallet sync first before finish the activity
                finishWithWalletSync();
                //finishSignInActivity();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void finishWithWalletSync() {
        finishSignInActivity();
    }

    private void addOdyseeAccountExplicitly(String currentEmail) {
        // Add account explicitly
        Account account = new Account("odysee", ARG_ACCOUNT_TYPE);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        try {
            Bundle bundle = new Bundle();
            bundle.putString("email", currentEmail);
            accountManager.addAccountExplicitly(account, "", bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Account act = accountManager.getAccounts()[0];
        accountManager.setAuthToken(act, ARG_AUTH_TYPE, Lbryio.AUTH_TOKEN);
    }

    private void finishSignInActivity() {
        finish();
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    private void editEmail() {
        if (emailVerifyCheckScheduler != null) {
            emailVerifyCheckScheduler.shutdownNow();
            emailVerifyCheckScheduler = null;
        }

        inputPassword.setText("");
        restoreControls(true);

        TransitionManager.beginDelayedTransition(findViewById(R.id.verification_activity));
        layoutVerify.setVisibility(View.GONE);
        layoutCollect.setVisibility(View.VISIBLE);
    }
}