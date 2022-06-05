package com.odysee.app.ui.wallet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.TransactionListAdapter;
import com.odysee.app.adapter.WalletDetailAdapter;
import com.odysee.app.callable.TransactionList;
import com.odysee.app.callable.WalletGetUnusedAddress;
import com.odysee.app.supplier.WalletGetUnusedAddressSupplier;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Transaction;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.model.WalletDetailItem;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;
import com.odysee.app.views.CreditsBalanceView;

public class WalletFragment extends BaseFragment implements WalletBalanceListener {

    private static final String MOONPAY_KEY = "c2tfbGl2ZV9ueVJqVXNDbE5pcnVSdnlCMkJLWW5JcFA5VnA3dWU=";
    private static final String MOONPAY_URL_FORMAT =
            "https://buy.moonpay.io?apiKey=pk_live_xNFffrN5NWKy6fu0ggbV8VQIwRieRzy&colorCode=%%23E50054&currencyCode=LBC&showWalletAddressForm=true&walletAddress=%s&externalCustomerId=%s";

    private CreditsBalanceView walletTotalBalanceView;
    private CreditsBalanceView walletSpendableBalanceView;
    private CreditsBalanceView walletSupportingBalanceView;
    private TextView textWalletBalanceUSD;
    private TextView textWalletBalanceDesc;
    private TextView buttonViewMore;
    private ListView detailListView;
    List<WalletDetailItem> detailRows;
    private WalletDetailAdapter detailAdapter;

    private ProgressBar walletSendProgress;

    private View loadingRecentContainer;
    private View inlineBalanceContainer;
    private TextView textWalletInlineBalance;
    private RecyclerView recentTransactionsList;
    private View linkViewAll;
    private TextView textConvertCredits;
    private TextView textWhatSyncMeans;
    private TextView textWalletReceiveAddress;
    private TextView textWalletHintSyncStatus;
    private ImageButton buttonCopyReceiveAddress;
    private TextInputEditText inputSendAddress;
    private TextInputEditText inputSendAmount;
    private MaterialButton buttonSend;
    private TextView textConnectedEmail;
    private SwitchMaterial switchSyncStatus;
    private TextView linkManualBackup;
    private TextView linkSyncFAQ;
    private TextView textNoRecentTransactions;

    private boolean hasFetchedRecentTransactions = false;
    private TransactionListAdapter recentTransactionsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wallet, container, false);

        loadingRecentContainer = root.findViewById(R.id.wallet_loading_recent_container);

        inlineBalanceContainer = root.findViewById(R.id.wallet_inline_balance_container);
        textWalletInlineBalance = root.findViewById(R.id.wallet_inline_balance_value);
        walletSendProgress = root.findViewById(R.id.wallet_send_progress);

        walletTotalBalanceView = root.findViewById(R.id.wallet_total_balance);
        walletSpendableBalanceView = root.findViewById(R.id.wallet_spendable_balance);
        walletSupportingBalanceView = root.findViewById(R.id.wallet_supporting_balance);
        textWalletBalanceUSD = root.findViewById(R.id.wallet_balance_usd_value);
        textWalletBalanceDesc = root.findViewById(R.id.total_balance_desc);
        textWalletHintSyncStatus = root.findViewById(R.id.wallet_hint_sync_status);
        buttonViewMore = root.findViewById(R.id.view_more_button);
        detailListView = root.findViewById(R.id.balance_detail_listview);

        recentTransactionsList = root.findViewById(R.id.wallet_recent_transactions_list);
        linkViewAll = root.findViewById(R.id.wallet_link_view_all);
        textNoRecentTransactions = root.findViewById(R.id.wallet_no_recent_transactions);
        textConvertCredits = root.findViewById(R.id.wallet_hint_convert_credits);
        textWhatSyncMeans = root.findViewById(R.id.wallet_hint_what_sync_means);
        textWalletReceiveAddress = root.findViewById(R.id.wallet_receive_address);
        buttonCopyReceiveAddress = root.findViewById(R.id.wallet_copy_receive_address);
        inputSendAddress = root.findViewById(R.id.wallet_input_send_address);
        inputSendAmount = root.findViewById(R.id.wallet_input_amount);
        buttonSend = root.findViewById(R.id.wallet_send);
        textConnectedEmail = root.findViewById(R.id.wallet_connected_email);
        switchSyncStatus = root.findViewById(R.id.wallet_switch_sync_status);
        linkManualBackup = root.findViewById(R.id.wallet_link_manual_backup);
        linkSyncFAQ = root.findViewById(R.id.wallet_link_sync_faq);

        initUi();

        return root;
    }

    private void copyReceiveAddress() {
        Context context = getContext();
        if (context != null && textWalletReceiveAddress != null) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("address", textWalletReceiveAddress.getText());
            clipboard.setPrimaryClip(data);
        }
        showMessage(R.string.address_copied);
    }

    private void fetchRecentTransactions() {
        fetchRecentTransactions(false);
    }

    private void fetchRecentTransactions(boolean forceFetch) {
        if (!Helper.isSignedIn(getContext())) {
            return;
        }

        if (hasFetchedRecentTransactions && !forceFetch) {
            return;
        }

        Helper.setViewVisibility(textNoRecentTransactions, View.GONE);

        AccountManager am = AccountManager.get(getContext());
        Account[] accounts = am.getAccounts();
        String authToken = am.peekAuthToken(Helper.getOdyseeAccount(accounts), "auth_token_type");

        loadingRecentContainer.setVisibility(View.VISIBLE);
        final Activity a = getActivity();

        if (a != null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Future<List<Transaction>> f = ((OdyseeApp) a.getApplication()).getExecutor().submit(new TransactionList(1, 5, authToken));

                    try {
                        List<Transaction> resultList = f.get();

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hasFetchedRecentTransactions = true;
                                recentTransactionsAdapter = new TransactionListAdapter(resultList, getContext());
                                recentTransactionsAdapter.setListener(new TransactionListAdapter.TransactionClickListener() {
                                    @Override
                                    public void onTransactionClicked(Transaction transaction) {

                                    }

                                    @Override
                                    public void onClaimUrlClicked(LbryUri uri) {
                                        Context context = getContext();
                                        if (uri != null && context instanceof MainActivity) {
                                            MainActivity activity = (MainActivity) context;
                                            if (uri.isChannel()) {
                                                activity.openChannelUrl(uri.toString());
                                            } else {
                                                activity.openFileUrl(uri.toString());
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recentTransactionsList.setAdapter(recentTransactionsAdapter);
                                displayNoRecentTransactions();
                                loadingRecentContainer.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
            t.start();
        }
    }

    private void displayNoRecentTransactions() {
        boolean showNoTransactionsView = hasFetchedRecentTransactions &&
                (recentTransactionsAdapter == null || recentTransactionsAdapter.getItemCount() == 0);
        Helper.setViewVisibility(textNoRecentTransactions, showNoTransactionsView ? View.VISIBLE : View.GONE);
    }

    private boolean validateSend() {
        String recipientAddress = Helper.getValue(inputSendAddress.getText());
        String amountString = Helper.getValue(inputSendAmount.getText());
        if (!recipientAddress.matches(LbryUri.REGEX_ADDRESS)) {
            showError(getString(R.string.invalid_recipient_address));
            return false;
        }

        if (!Helper.isNullOrEmpty(amountString)) {
            try {
                double amountValue = Double.parseDouble(amountString);
                if (Lbry.getAvailableBalance() < amountValue) {
                    showError(getString(R.string.insufficient_balance));
                    return false;
                }
            } catch (NumberFormatException ex) {
                // pass
                showError(getString(R.string.invalid_amount));
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void initUi() {
        onWalletBalanceUpdated(Lbry.walletBalance);

        Helper.applyHtmlForTextView(textConvertCredits);
        Helper.applyHtmlForTextView(textWhatSyncMeans);
        Helper.applyHtmlForTextView(linkManualBackup);
        Helper.applyHtmlForTextView(linkSyncFAQ);

        Context context = getContext();

        if (context != null) {
            LinearLayoutManager llm = new LinearLayoutManager(context);
            recentTransactionsList.setLayoutManager(llm);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            Drawable thinDivider = ContextCompat.getDrawable(context, R.drawable.thin_divider);
            if (thinDivider != null) {
                itemDecoration.setDrawable(thinDivider);
                recentTransactionsList.addItemDecoration(itemDecoration);
            }
        }

        buttonViewMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    View walletDetail = ((MainActivity) context).findViewById(R.id.balance_detail_listview);

                    if (walletDetail.getVisibility() == View.GONE) {
                        int listHeight = Math.round(getResources().getDisplayMetrics().density);

                        for (int i = 0; i < detailRows.size(); i++) {
                            View item = detailAdapter.getView(i, null, detailListView);
                            item.measure(0, 0);
                            listHeight += item.getMeasuredHeight();
                        }

                        // Avoid scroll bars being displayed
                        ViewGroup.LayoutParams params = detailListView.getLayoutParams();
                        params.height = listHeight + (detailListView.getCount() + 1) * detailListView.getDividerHeight();
                        detailListView.setLayoutParams(params);
                        detailListView.setVerticalScrollBarEnabled(false);
                        detailListView.requestLayout();

                        TransitionManager.beginDelayedTransition((ViewGroup) walletDetail.getParent());
                        walletDetail.setVisibility(View.VISIBLE);
                        buttonViewMore.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_dropup, 0);
                    } else {
                        walletDetail.setVisibility(View.GONE);
                        buttonViewMore.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_dropdown, 0);
                    }
                }
            }
        });

        textWalletReceiveAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyReceiveAddress();
            }
        });
        buttonCopyReceiveAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyReceiveAddress();
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateSend()) {
                    sendCredits();
                }
            }
        });

        inputSendAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                inputSendAddress.setHint(hasFocus ? getString(R.string.recipient_address_placeholder) : "");
            }
        });
        inputSendAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                inputSendAmount.setHint(hasFocus ? getString(R.string.zero) : "");
                inlineBalanceContainer.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });

        linkViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openFragment(TransactionHistoryFragment.class, true, null);
                }
            }
        });

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        final boolean walletSyncEnabled = sp.getBoolean(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_SYNC_ENABLED, false);
        switchSyncStatus.setChecked(walletSyncEnabled);
        switchSyncStatus.setText(walletSyncEnabled ? R.string.on : R.string.off);
        textWalletHintSyncStatus.setText(walletSyncEnabled ? R.string.backup_synced : R.string.backup_notsynced);
        textConnectedEmail.setText(walletSyncEnabled ? Lbryio.getSignedInEmail() : null);
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Context c = getContext();
                if (switchSyncStatus.isChecked() && c != null) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
                    sp.edit().putBoolean(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_SYNC_ENABLED, false).apply();
                    switchSyncStatus.setText(R.string.off);
                    switchSyncStatus.setChecked(false);
                } else {
                    // launch verification activity for wallet sync flow
                    if (c instanceof MainActivity) {
                        ((MainActivity) c).walletSyncSignIn();
                    }
                }
                return true;
            }
        };
        GestureDetector detector = new GestureDetector(getContext(), gestureListener);

        switchSyncStatus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                detector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    public void onWalletSyncEnabled() {
        switchSyncStatus.setText(R.string.on);
        switchSyncStatus.setChecked(true);
        textWalletHintSyncStatus.setText(R.string.backup_synced);
        textConnectedEmail.setText(Lbryio.getSignedInEmail());
        fetchRecentTransactions();
    }

    private void disableSendControls() {
        inputSendAddress.clearFocus();
        inputSendAmount.clearFocus();
        Helper.setViewEnabled(buttonSend, false);
        Helper.setViewEnabled(inputSendAddress, false);
        Helper.setViewEnabled(inputSendAmount, false);
    }

    private void enableSendControls() {
        Helper.setViewEnabled(buttonSend, true);
        Helper.setViewEnabled(inputSendAddress, true);
        Helper.setViewEnabled(inputSendAmount, true);
    }

    private void sendCredits() {
        // wallet_send task
        View view = getView();
        String recipientAddress = Helper.getValue(inputSendAddress.getText());
        String amountString = Helper.getValue(inputSendAmount.getText());
        final String amount;
        try {
            amount = new DecimalFormat(Helper.SDK_AMOUNT_FORMAT, new DecimalFormatSymbols(Locale.US)).
                    format(new BigDecimal(amountString).doubleValue());
        } catch (NumberFormatException ex) {
            if (view != null) {
                showError(getString(R.string.invalid_amount));
            }
            return;
        }


        double actualSendAmount = Double.parseDouble(amount);
        if (actualSendAmount < Helper.MIN_SPEND) {
            if (view != null) {
                showError(getString(R.string.min_spend_required));
            }
            return;
        }

        disableSendControls();
        Helper.setViewVisibility(walletSendProgress, View.VISIBLE);

        Runnable sendRunnable = new Runnable() {
            @Override
            public void run() {
                boolean result = false;
                Context ctx = getContext();
                if (ctx instanceof MainActivity) {
                    MainActivity activity = (MainActivity) ctx;
                    try {
                        Map<String, Object> options = new HashMap<>();
                        options.put("addresses", Collections.singletonList(recipientAddress));
                        options.put("amount", amount);
                        options.put("blocking", true);
                        Lbry.directApiCall(Lbry.METHOD_WALLET_SEND, options, activity.getAuthToken());

                        result = true;
                    } catch (ApiCallException ex) {
                        // pass
                    }
                }
                handleSendResult(result, actualSendAmount);
            }
        };

        Activity a = getActivity();

        if (a != null) {
            ((OdyseeApp) a.getApplication()).getExecutor().execute(sendRunnable);
        }
    }

    private void handleSendResult(final boolean result, final double actualSendAmount) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                enableSendControls();
                Helper.setViewVisibility(walletSendProgress, View.GONE);

                View view = getView();
                if (result) {
                    showMessage(getResources().getQuantityString(
                            R.plurals.you_sent_credits, actualSendAmount == 1.0 ? 1 : 2,
                            new DecimalFormat("#,###.####").format(actualSendAmount)));
                    Helper.setViewText(inputSendAddress, null);
                    Helper.setViewText(inputSendAmount, null);
                } else {
                    if (view != null) {
                        showError(getString(R.string.send_credit_error));
                    }
                }
            }
        });
    }

    private void checkReceiveAddress() {
        Context context = getContext();
        String receiveAddress = null;
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            receiveAddress = sp.getString(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_RECEIVE_ADDRESS, null);
        }
        if (Helper.isNullOrEmpty(receiveAddress)) {
            generateNewAddress();
        } else {
            Helper.setViewText(textWalletReceiveAddress, receiveAddress);
        }
    }

    public void launchMoonpayFlow() {
        Context context = getContext();
        String receiveAddress;
        if (context != null) {
            try {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                receiveAddress = sp.getString(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_RECEIVE_ADDRESS, null);
                if (Helper.isNullOrEmpty(receiveAddress)) {
                    showError(getString(R.string.receive_address_not_set));
                    return;
                }

                long userId = Lbryio.currentUser != null ? Lbryio.currentUser.getId() : 0;
                @SuppressLint("DefaultLocale")
                String url = String.format(MOONPAY_URL_FORMAT, receiveAddress,
                        URLEncoder.encode(String.format("OdyseeAndroid-%d", userId), StandardCharsets.UTF_8.name()));
                String email = Lbryio.getSignedInEmail();
                if (!Helper.isNullOrEmpty(email)) {
                    url = String.format("%s&email=%s", url, URLEncoder.encode(email, StandardCharsets.UTF_8.name()));
                }
                // Sign the URL
                String query = url.substring(url.indexOf("?"));
                Mac hmacSHA256 = Mac.getInstance("HmacSHA256");

                SecretKeySpec secretKey = new SecretKeySpec(
                        new String(Base64.decode(MOONPAY_KEY, Base64.NO_WRAP), StandardCharsets.UTF_8.name()).getBytes(), "HmacSHA256");
                hmacSHA256.init(secretKey);
                String signature = new String(
                        Base64.encode(hmacSHA256.doFinal(query.getBytes(StandardCharsets.UTF_8.name())), Base64.NO_WRAP),
                        StandardCharsets.UTF_8.name());
                url = String.format("%s&signature=%s", url, URLEncoder.encode(signature, StandardCharsets.UTF_8.name()));

                CustomTabColorSchemeParams.Builder ctcspb = new CustomTabColorSchemeParams.Builder();
                ctcspb.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                CustomTabColorSchemeParams ctcsp = ctcspb.build();

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder().setDefaultColorSchemeParams(ctcsp);
                CustomTabsIntent intent = builder.build();
                intent.launchUrl(context, Uri.parse(url));
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException ex) {
                showError(getString(R.string.hash_not_supported));
            }
        }
    }

    public void onResume() {
        super.onResume();

        Context context = getContext();
        //Helper.setWunderbarValue(null, context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (!activity.isSignedIn()) {
                return;
            }

            // When device is in Battery Saver mode, the scheduled wallet update will no perform the job.
            // In order to show user the update balance, let's request it explicitly when user opens the Wallet fragment
            if (activity.isBatterySaverMode()) {
                activity.updateWalletBalance();
            }

            activity.syncWalletAndLoadPreferences();
            LbryAnalytics.setCurrentScreen(activity, "Wallet", "Wallet");
        }
        checkReceiveAddress();
        checkRewardsDriver();
        fetchRecentTransactions();
    }

    public void onPause() {
        hasFetchedRecentTransactions = false;
        super.onPause();
    }
    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (!activity.isSignedIn()) {
                activity.simpleSignIn(R.id.action_wallet_menu);
            }
            activity.addWalletBalanceListener(this);
        }
    }

    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.removeWalletBalanceListener(this);
        }
        super.onStop();
    }

    public void generateNewAddress() {
        Activity a = getActivity();
        if (a != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                AccountManager am = AccountManager.get(getContext());
                Supplier<String> task = new WalletGetUnusedAddressSupplier(am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type"));
                CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(task, ((OdyseeApp) a.getApplication()).getExecutor());
                completableFuture.thenAccept(addr -> {
                    if (!Helper.isNullOrEmpty(addr)) {
                        Context context = getContext();
                        if (context != null) {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                            sp.edit().putString(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_RECEIVE_ADDRESS, addr).apply();
                        }
                        Helper.setViewText(textWalletReceiveAddress, addr);
                    }
                });
            } else {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AccountManager am = AccountManager.get(getContext());
                        String authToken = am.peekAuthToken(am.getAccounts()[0], "auth_token_type");

                        Callable<String> callable = new WalletGetUnusedAddress(authToken);
                        Future<String> future = ((OdyseeApp) a.getApplication()).getExecutor().submit(callable);
                        try {
                            String addr = future.get();

                            if (!Helper.isNullOrEmpty(addr)) {
                                Context context = getContext();
                                if (context != null) {
                                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                                    sp.edit().putString(MainActivity.PREFERENCE_KEY_INTERNAL_WALLET_RECEIVE_ADDRESS, addr).apply();
                                    ((MainActivity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Helper.setViewText(textWalletReceiveAddress, addr);
                                        }
                                    });
                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });

                thread.start();
            }
        }
    }

    public void onWalletBalanceUpdated(WalletBalance walletBalance) {
        if (walletBalance == null) {
            return;
        }
        double totalBalance = walletBalance.getTotal().doubleValue();
        double spendableBalance = walletBalance.getAvailable().doubleValue();
        double supportingBalance = walletBalance.getClaims().doubleValue() + walletBalance.getTips().doubleValue() + walletBalance.getSupports().doubleValue();
        double usdBalance = totalBalance * Lbryio.LBCUSDRate;
        double tipsBalance = walletBalance.getTips().doubleValue();

        if (detailRows == null)
            detailRows = new ArrayList<>(3);

        if (detailAdapter == null) {
            detailAdapter = new WalletDetailAdapter(getContext(), detailRows);
            detailListView.setAdapter(detailAdapter);
        }

        Activity activity = (Activity) getContext();
        boolean tipsBeenUnlocked;

        if (activity instanceof MainActivity)
            tipsBeenUnlocked = ((MainActivity) activity).isUnlockingTips();
        else
            tipsBeenUnlocked = false;

        WalletDetailItem earnedBalance = new WalletDetailItem(getResources().getString(R.string.earned_from_others), getResources().getString(R.string.unlock_to_spend), Helper.SIMPLE_CURRENCY_FORMAT.format(tipsBalance), tipsBalance != 0, tipsBeenUnlocked);
        WalletDetailItem initialPublishes = new WalletDetailItem(getResources().getString(R.string.on_initial_publishes), getResources().getString(R.string.delete_or_edit_past_content), Helper.SIMPLE_CURRENCY_FORMAT.format(walletBalance.getClaims().doubleValue()), false, false);
        WalletDetailItem supportingContent = new WalletDetailItem(getResources().getString(R.string.supporting_content), getResources().getString(R.string.delete_supports_to_spend), Helper.SIMPLE_CURRENCY_FORMAT.format(walletBalance.getSupports().doubleValue()), false, false);

        if (detailRows.size() == 0) {
            detailRows.add(0, earnedBalance);
            detailRows.add(1, initialPublishes);
            detailRows.add(2, supportingContent);
        } else {
            if (!detailRows.get(0).detailAmount.equals(earnedBalance.detailAmount)
                 || detailRows.get(0).isInProgress != earnedBalance.isInProgress
                 || detailRows.get(0).isUnlockable != earnedBalance.isUnlockable) {
                detailRows.set(0, earnedBalance);
            }
            if (!detailRows.get(1).detailAmount.equals(initialPublishes.detailAmount)) {
                detailRows.set(1, initialPublishes);
            }
            if (!detailRows.get(2).detailAmount.equals(supportingContent.detailAmount)) {
                detailRows.set(2, supportingContent);
            }
        }

        detailListView.setAdapter(detailAdapter);

        String formattedTotalBalance = Helper.REDUCED_LBC_CURRENCY_FORMAT.format(totalBalance);
        String formattedSpendableBalance = Helper.SIMPLE_CURRENCY_FORMAT.format(spendableBalance);
        String formattedSupportingBalance = Helper.SIMPLE_CURRENCY_FORMAT.format(supportingBalance);
        Helper.setViewText(walletTotalBalanceView, totalBalance > 0 && formattedTotalBalance.equals("0") ? Helper.FULL_LBC_CURRENCY_FORMAT.format(totalBalance) : formattedTotalBalance);
        Helper.setViewText(walletSpendableBalanceView, spendableBalance > 0 && formattedSpendableBalance.equals("0") ? Helper.FULL_LBC_CURRENCY_FORMAT.format(spendableBalance) : formattedSpendableBalance);
        Helper.setViewText(walletSupportingBalanceView, supportingBalance > 0 && formattedSupportingBalance.equals("0") ? Helper.FULL_LBC_CURRENCY_FORMAT.format(supportingBalance) : formattedSupportingBalance);
        Helper.setViewText(textWalletInlineBalance, Helper.shortCurrencyFormat(spendableBalance));
        if (Lbryio.LBCUSDRate > 0) {
            // only update display usd values if the rate is loaded
            Helper.setViewText(textWalletBalanceUSD, String.format("≈$%s", Helper.SIMPLE_CURRENCY_FORMAT.format(usdBalance)));
        }

        textWalletBalanceDesc.setText(spendableBalance == totalBalance ? getResources().getString(R.string.your_total_balance) : getResources().getString(R.string.all_of_this_is_yours));

        fetchRecentTransactions(true);
        checkRewardsDriver();
    }

    private void checkRewardsDriver() {
        // check rewards driver
        Context ctx = getContext();
        if (ctx != null) {
            String rewardsDriverText = getString(R.string.free_credits_available);
            if (Lbryio.totalUnclaimedRewardAmount > 0) {
                rewardsDriverText = getResources().getQuantityString(
                        Helper.isSignedIn(ctx) ? R.plurals.wallet_signed_in_free_credits : R.plurals.wallet_get_free_credits,
                        Lbryio.totalUnclaimedRewardAmount == 1 ? 1 : 2,
                        Helper.shortCurrencyFormat(Lbryio.totalUnclaimedRewardAmount));
            }
            checkRewardsDriverCard(rewardsDriverText, 0);
        }
    }
}
