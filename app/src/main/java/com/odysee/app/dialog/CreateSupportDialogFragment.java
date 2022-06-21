package com.odysee.app.dialog;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.text.HtmlCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.supplier.SupportCreateSupplier;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class CreateSupportDialogFragment extends BottomSheetDialogFragment implements WalletBalanceListener {
    public static final String TAG = "CreateSupportDialog";

    private MaterialButton sendButton;
    private View cancelLink;
    private TextInputEditText inputAmount;
    private View inlineBalanceContainer;
    private TextView inlineBalanceValue;
    private ProgressBar sendProgress;

    private InlineChannelSpinnerAdapter channelSpinnerAdapter;
    private AppCompatSpinner channelSpinner;
    private SwitchMaterial switchTip;

    private boolean fetchingChannels;
    private ProgressBar progressLoadingChannels;


    private final CreateSupportListener listener;
    private final Claim claim;

    private CreateSupportDialogFragment(Claim claim, CreateSupportListener listener) {
        super();
        this.claim = claim;
        this.listener = listener;
    }

    @NonNull
    public static CreateSupportDialogFragment newInstance(Claim claim, CreateSupportListener listener) {
        return new CreateSupportDialogFragment(claim, listener);
    }

    private void disableControls() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
        }
        channelSpinner.setEnabled(false);
        switchTip.setEnabled(false);
        sendButton.setEnabled(false);
        cancelLink.setEnabled(false);
    }
    private void enableControls() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }
        channelSpinner.setEnabled(true);
        switchTip.setEnabled(true);
        sendButton.setEnabled(true);
        cancelLink.setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_support, container, false);

        inputAmount = view.findViewById(R.id.create_support_input_amount);
        inlineBalanceContainer = view.findViewById(R.id.create_support_inline_balance_container);
        inlineBalanceValue = view.findViewById(R.id.create_support_inline_balance_value);
        sendProgress = view.findViewById(R.id.create_support_progress);
        cancelLink = view.findViewById(R.id.create_support_cancel_link);
        sendButton = view.findViewById(R.id.create_support_send);

        channelSpinner = view.findViewById(R.id.create_support_channel_spinner);
        switchTip = view.findViewById(R.id.create_support_make_tip_switch);
        progressLoadingChannels = view.findViewById(R.id.create_support_channel_progress);

        inputAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                inputAmount.setHint(hasFocus ? getString(R.string.zero) : "");
                inlineBalanceContainer.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });
        inputAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateSendButtonText();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        updateInfoText();
        updateSendButtonText();

        String channel = null;
        if (Claim.TYPE_CHANNEL.equalsIgnoreCase(claim.getValueType())) {
            channel = claim.getTitleOrName();
        } else if (claim.getSigningChannel() != null) {
            channel = claim.getPublisherTitle();
        }
        TextView titleView = view.findViewById(R.id.create_support_title);
        String tipTitleText = Helper.isNullOrEmpty(channel) ? getString(R.string.send_a_tip) : getString(R.string.send_a_tip_to, channel);
        titleView.setText(tipTitleText);

        switchTip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    // show tip info
                    titleView.setText(tipTitleText);
                    updateSendButtonText();
                } else {
                    // show support info
                    titleView.setText(R.string.support_this_content);
                    sendButton.setText(R.string.send_revocable_support);
                }
                updateInfoText();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String amountString = Helper.getValue(inputAmount.getText());
                if (Helper.isNullOrEmpty(amountString)) {
                    showError(getString(R.string.invalid_amount));
                    return;
                }

                BigDecimal amount = new BigDecimal(amountString);
                if (amount.doubleValue() > Lbry.getAvailableBalance()) {
                    showError(getString(R.string.insufficient_balance));
                    return;
                }
                if (amount.doubleValue() < Helper.MIN_SPEND) {
                    showError(getString(R.string.min_spend_required));
                    return;
                }

                Claim selectedChannel = (Claim) channelSpinner.getSelectedItem();
                String channelId = !fetchingChannels && selectedChannel != null ? selectedChannel.getClaimId() : null;
                boolean isTip = switchTip.isChecked();

                disableControls();
                AccountManager am = AccountManager.get(getContext());
                String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                Map<String, Object> options = new HashMap<>();
                options.put("blocking", true);
                options.put("claim_id", claim.getClaimId());
                options.put("amount", new DecimalFormat(Helper.SDK_AMOUNT_FORMAT, new DecimalFormatSymbols(Locale.US)).format(amount.doubleValue()));
                options.put("tip", isTip);
                if (!Helper.isNullOrEmpty(channelId)) {
                    options.put("channel_id", channelId);
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    Supplier<String> task = new SupportCreateSupplier(options, authToken);
                    CompletableFuture<String> cf = CompletableFuture.supplyAsync(task);
                    cf.thenAccept(result -> {
                        Activity activity = getActivity();
                        if (result == null) {
                            if (listener != null) {
                                listener.onSupportCreated(amount, isTip);
                            }
                            dismiss();
                        } else {
                            showError(result);
                        }

                        if (activity != null) {
                            enableControls();
                        }
                    });
                } else {
                    Thread supportingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Callable<Boolean> callable = () -> {
                                try {
                                    Lbry.authenticatedGenericApiCall(Lbry.METHOD_SUPPORT_CREATE, options, authToken);
                                } catch (ApiCallException ex) {
                                    ex.printStackTrace();
                                    showError(ex.getMessage());
                                    return false;
                                }
                                return true;
                            };
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            Future<Boolean> future = executorService.submit(callable);

                            try {
                                boolean result = future.get();

                                Activity activity = getActivity();

                                if (result) {
                                    if (listener != null) {
                                        listener.onSupportCreated(amount, isTip);
                                    }

                                    if (activity != null) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dismiss();
                                            }
                                        });
                                    }
                                }

                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            enableControls();
                                        }
                                    });
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    supportingThread.start();
                }
            }
        });

        cancelLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        onWalletBalanceUpdated(Lbry.walletBalance);
        updateInfoText();

        inputAmount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    Context context = getContext();
                    if (context != null) {
                        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                    }
                }
            }
        });
        inputAmount.requestFocus();

        return view;
    }

    private void updateSendButtonText() {
        boolean isTip = switchTip.isChecked();
        if (!isTip) {
            sendButton.setText(R.string.send_revocable_support);
        } else {
            String amountString = Helper.getValue(inputAmount.getText(), "0");
            double parsedAmount = Helper.parseDouble(amountString, 0);
            String text = getResources().getQuantityString(R.plurals.send_lbc_tip, parsedAmount == 1.0 ? 1 : 2, amountString);
            sendButton.setText(text);
        }
    }

    private void updateInfoText() {
        View view = getView();
        if (view != null && switchTip != null) {
            TextView infoText = view.findViewById(R.id.create_support_info);
            boolean isTip = switchTip.isChecked();

            infoText.setMovementMethod(LinkMovementMethod.getInstance());
            if (!isTip) {
                infoText.setText(HtmlCompat.fromHtml(getString(R.string.support_info), HtmlCompat.FROM_HTML_MODE_LEGACY));
            } else if (claim != null) {
                infoText.setText(HtmlCompat.fromHtml(
                        Claim.TYPE_CHANNEL.equalsIgnoreCase(claim.getValueType()) ?
                                getString(R.string.send_tip_info_channel, claim.getTitleOrName()) :
                                getString(R.string.send_tip_info_content, claim.getTitleOrName()),
                        HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        }
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && !Lbry.ownChannels.isEmpty()) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        fetchingChannels = true;
        disableChannelSpinner();
        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        ClaimListTask task = new ClaimListTask(options, progressLoadingChannels, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownChannels = new ArrayList<>(claims);
                updateChannelList(Lbry.ownChannels);
                enableChannelSpinner();
                fetchingChannels = false;
            }

            @Override
            public void onError(Exception error) {
                enableChannelSpinner();
                fetchingChannels = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private void disableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, false);
    }
    private void enableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, true);
    }

    private void updateChannelList(List<Claim> channels) {
        Context context = getContext();
        if (channelSpinnerAdapter == null) {
            if (context != null) {
                channelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, new ArrayList<>(channels));
                channelSpinnerAdapter.addAnonymousPlaceholder();
                channelSpinnerAdapter.notifyDataSetChanged();
            }
        } else {
            channelSpinnerAdapter.clear();
            channelSpinnerAdapter.addAll(channels);
            channelSpinnerAdapter.addAnonymousPlaceholder();
            channelSpinnerAdapter.notifyDataSetChanged();
        }

        if (channelSpinner != null) {
            channelSpinner.setAdapter(channelSpinnerAdapter);
        }

        if (channelSpinnerAdapter != null && channelSpinner != null) {
            if (channelSpinnerAdapter.getCount() > 1) {
                String defaultChannelName = Helper.getDefaultChannelName(context);
                List<Claim> defaultChannel = channels.stream().filter(c -> c != null && c.getName().equalsIgnoreCase(defaultChannelName)).collect(Collectors.toList());

                if (defaultChannel.size() > 0) {
                    channelSpinner.setSelection(channelSpinnerAdapter.getItemPosition(defaultChannel.get(0)));
                } else {
                    channelSpinner.setSelection(1);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).addWalletBalanceListener(this);
        }
        updateInfoText();
        fetchChannels();
    }

    @Override
    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).removeWalletBalanceListener(this);
        }
        super.onPause();
    }

    @Override
    public void onWalletBalanceUpdated(WalletBalance walletBalance) {
        if (walletBalance != null && inlineBalanceValue != null) {
            inlineBalanceValue.setText(Helper.shortCurrencyFormat(walletBalance.getAvailable().doubleValue()));
        }
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).
                setBackgroundTint(Color.RED).
                setTextColor(Color.WHITE).
                show();
        }
    }

    public interface CreateSupportListener {
        void onSupportCreated(BigDecimal amount, boolean isTip);
    }
}
