package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.callable.UserExistsWithPassword;
import com.odysee.app.callable.UserSignin;
import com.odysee.app.tasks.verification.CheckUserEmailVerifiedTask;
import com.odysee.app.tasks.verification.EmailNewTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

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
    public static final String ACTION_USER_SIGNED_IN_SUCCESSFULLY = "com.odysee.app.USER_SIGNED_IN_SUCCESSFULLY";
    public final static String ARG_ACCOUNT_TYPE = "com.odysee";
    public final static String ARG_AUTH_TYPE = "auth_token_type";
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private ProgressBar emailAddProgress;
    private View layoutCollect;
    private View layoutVerify;
    private TextView textAddedEmail;

    private String currentEmail;
    private ScheduledExecutorService emailVerifyCheckScheduler;
    private ExecutorService executor;
    private int sourceTabId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        executor = Executors.newSingleThreadExecutor();

        layoutCollect = findViewById(R.id.signin_form);
        layoutVerify = findViewById(R.id.verification_email_verify_container);
        textAddedEmail = findViewById(R.id.verification_email_added_address);
        inputEmail = findViewById(R.id.verification_email_input);
        inputPassword = findViewById(R.id.signin_password);
        emailAddProgress = findViewById(R.id.verification_email_add_progress);
        Button buttonSignIn = findViewById(R.id.signin_button);

        TextView agreeToTerms = findViewById(R.id.agree_to_terms_note);
        agreeToTerms.setMovementMethod(LinkMovementMethod.getInstance());

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEmail();
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

    private void performSignIn(String email, String password) {
        View progressView = findViewById(R.id.password_signin_progress);
        TransitionManager.beginDelayedTransition(findViewById(R.id.signin_buttons));
        findViewById(R.id.signin_buttons).setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);

        Map<String, String> options = new HashMap<>();
        options.put("email", email);

        if (checkUserExistsWithPassword(email)) {
            options.put("password", password);

            Future<Boolean> future = executor.submit(new UserSignin(getApplicationContext(), options));
            try {
                if (future.get())
                    scheduleEmailVerify();
                else {
                    Snackbar.make(layoutCollect, "Retry request", Snackbar.LENGTH_LONG).
                            setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            EmailNewTask task = new EmailNewTask(currentEmail, emailAddProgress, new EmailNewTask.EmailNewHandler() {
                @Override
                public void beforeStart() {
                }

                @Override
                public void onSuccess() {
                    TransitionManager.beginDelayedTransition(findViewById(R.id.verification_activity));
                    findViewById(R.id.signin_buttons).setVisibility(View.VISIBLE);
                    progressView.setVisibility(View.GONE);
                    layoutCollect.setVisibility(View.GONE);
                    layoutVerify.setVisibility(View.VISIBLE);
                    Helper.setViewText(textAddedEmail, currentEmail);
                    scheduleEmailVerify();
                }

                @Override
                public void onEmailExists() {
                    // TODO: Update wording based on email already existing
                }

                @Override
                public void onError(Exception error) {
                    View view = findViewById(R.id.verification_email_collect_container);
                    if (view != null && error != null) {
                        Snackbar.make(view, error.toString(), Snackbar.LENGTH_LONG).
                                setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                    }
                }
            });

            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void addEmail() {
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

        performSignIn(currentEmail, password);
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

                // send broadcat to indicate the user finished sign in
                Intent intent = new Intent(ACTION_USER_SIGNED_IN_SUCCESSFULLY);
                intent.putExtra("sourceTabId", sourceTabId);
                LocalBroadcastManager.getInstance(SignInActivity.this).sendBroadcast(intent);

                // perform wallet sync first before finish the activity
                handleWalletSync();
                //finishSignInActivity();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleWalletSync() {

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

        TransitionManager.beginDelayedTransition(findViewById(R.id.verification_activity));
        layoutVerify.setVisibility(View.GONE);
        layoutCollect.setVisibility(View.VISIBLE);
    }
}