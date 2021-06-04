package com.odysee.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.tasks.verification.CheckUserEmailVerifiedTask;
import com.odysee.app.tasks.verification.EmailNewTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;

import static com.odysee.app.utils.Lbryio.TAG;

public class SignInActivity extends Activity {
    public final static String ARG_ACCOUNT_TYPE = "com.odysee";
    public final static String ARG_AUTH_TYPE = "auth_token_type";
    private MaterialButton buttonContinue;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private ProgressBar emailAddProgress;
    private View layoutCollect;
    private View layoutVerify;
    private TextView textAddedEmail;
    private View buttonEdit;

    private String currentEmail;
    private ScheduledExecutorService emailVerifyCheckScheduler;
    private String auth_token_sent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

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

        buttonEdit = findViewById(R.id.verification_email_edit_button);
        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editEmail();
            }
        });
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> callable = () -> {
            Map<String, String> options = new HashMap<String, String>();
            options.put("email", email);

            try {
                Response response = Lbryio.call("user", "exists", options, Helper.METHOD_POST, getApplicationContext());

                if (response.isSuccessful()) {
                    String responseString = response.body().string();
                    response.close();
                    JSONObject jsonData = new JSONObject(responseString);

                    if (jsonData.has("data"))
                        return (jsonData.getJSONObject("data").getBoolean("has_password"));
                }
                return false;
            } catch (LbryioRequestException | LbryioResponseException e) {
                Log.e(TAG, e.getLocalizedMessage());
                return false;
            }
        };

        Future<Boolean> future = executor.submit(callable);

        try {
            boolean hasPassword = future.get();
            return hasPassword;
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "checkUserExistsWithPassword: ".concat(e.getLocalizedMessage()));
        }
        return false;
    }

    private void performSignIn(String email, String password, String authToken) {
        View progressView = findViewById(R.id.password_signin_progress);
        TransitionManager.beginDelayedTransition(findViewById(R.id.signin_buttons));
        findViewById(R.id.signin_buttons).setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> callable = () -> {
            Map<String, String> options = new HashMap<String, String>();
            options.put("email", email);

            if (checkUserExistsWithPassword(email)) {
                options.put("password", password);
                try {
                    Response responseSignIn = Lbryio.call("user", "signin", options, Helper.METHOD_POST, getApplicationContext());
                    if (responseSignIn.isSuccessful()) {
                        String responseString = responseSignIn.body().string();
                        responseSignIn.close();
                        JSONObject responseJson = new JSONObject(responseString);
                        if (responseJson.getBoolean("success") == true) {
                            JSONObject jsondata = responseJson.getJSONObject("data");
                            if (jsondata.has("primary_email") && jsondata.getString("primary_email").equals(email)) {
                                scheduleEmailVerify();
                            }
                        }
                        return true;
                    } else {
                        Log.e(TAG, "performSignIn: ".concat(responseSignIn.body().string()));
                        return false;
                    }
                } catch (LbryioRequestException | LbryioResponseException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    return false;
                }
            } else {
                EmailNewTask task = new EmailNewTask(currentEmail, emailAddProgress, new EmailNewTask.EmailNewHandler() {
                    @Override
                    public void beforeStart() {
                        Helper.setViewVisibility(buttonContinue, View.INVISIBLE);
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
                return false;
            }
        };

        Future<Boolean> future = executor.submit(callable);
        try {
            boolean resultado = future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
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

        performSignIn(currentEmail, password, null);
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

                finishSignInActivity();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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