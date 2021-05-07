package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.tasks.verification.CheckUserEmailVerifiedTask;
import com.odysee.app.tasks.verification.EmailNewTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends Activity {
    public final static String ARG_ACCOUNT_TYPE = "com.odysee";
    public final static String ARG_AUTH_TYPE = "auth_token_type";
    private MaterialButton buttonContinue;
    private TextInputEditText inputEmail;
    private ProgressBar emailAddProgress;
    private View layoutCollect;
    private View layoutVerify;
    private TextView textAddedEmail;

    private String currentEmail;
    private ScheduledExecutorService emailVerifyCheckScheduler;
    private String auth_token_sent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        layoutCollect = findViewById(R.id.verification_email_collect_container);
        layoutVerify = findViewById(R.id.verification_email_verify_container);
        textAddedEmail = findViewById(R.id.verification_email_added_address);
        inputEmail = findViewById(R.id.verification_email_input);
        emailAddProgress = findViewById(R.id.verification_email_add_progress);
        buttonContinue = findViewById(R.id.verification_email_continue_button);

        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEmail();
            }
        });
    }

    private void addEmail() {
        currentEmail = Helper.getValue(inputEmail.getText());
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



        EmailNewTask task = new EmailNewTask(currentEmail, emailAddProgress, new EmailNewTask.EmailNewHandler() {
            @Override
            public void beforeStart() {
                Helper.setViewVisibility(buttonContinue, View.INVISIBLE);
            }

            @Override
            public void onSuccess() {
                layoutCollect.setVisibility(View.GONE);
                layoutVerify.setVisibility(View.VISIBLE);
                Helper.setViewText(textAddedEmail, currentEmail);
                scheduleEmailVerify();

                Helper.setViewVisibility(buttonContinue, View.VISIBLE);
            }

            @Override
            public void onEmailExists() {
                // TODO: Update wording based on email already existing
            }

            @Override
            public void onError(Exception error) {
                View view = findViewById(R.id.verification_email_collect_container);
                if (view != null && error != null) {
                    Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).
                            setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                }
                Helper.setViewVisibility(buttonContinue, View.VISIBLE);
            }
        });

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                finish();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}