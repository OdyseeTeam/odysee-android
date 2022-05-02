package com.odysee.app.ui.channel;

import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.BuildConfig;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.callable.ChannelCreateUpdate;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.lbryinc.LogPublishTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChannelCreateDialogFragment extends BottomSheetDialogFragment {
    ChannelCreateListener listener;

    private ChannelCreateDialogFragment(ChannelCreateListener listener) {
        super();
        this.listener = listener;
    }

    public static ChannelCreateDialogFragment newInstance(ChannelCreateListener listener) {
        return new ChannelCreateDialogFragment(listener);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.channel_form_bottom_sheet, container, false);

        View balanceView = v.findViewById(R.id.channel_form_balanceview);

        TextInputEditText inputDeposit = v.findViewById(R.id.inline_channel_form_input_deposit);
        inputDeposit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Helper.setViewVisibility(balanceView, hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });

        TextInputEditText inputChannelName = v.findViewById(R.id.inline_channel_form_input_name);

        TextView linkCancel = v.findViewById(R.id.inline_channel_form_cancel_link);
        linkCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputChannelName.setText("");
                inputDeposit.setText(R.string.min_deposit);
                dismiss();
            }
        });

        View progressView = v.findViewById(R.id.inline_channel_form_create_progress);
        Button createButton = v.findViewById(R.id.inline_channel_form_create_button);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // validate deposit and channel name
                String channelNameString = Helper.normalizeChannelName(Helper.getValue(inputChannelName.getText()));
                Claim claimToSave = new Claim();
                claimToSave.setName(channelNameString);
                String channelName = claimToSave.getName().startsWith("@") ? claimToSave.getName().substring(1) : claimToSave.getName();
                String depositString = Helper.getValue(inputDeposit.getText());
                if ("@".equals(channelName) || Helper.isNullOrEmpty(channelName)) {
                    showError(getString(R.string.please_enter_channel_name));
                    return;
                }
                if (!LbryUri.isNameValid(channelName)) {
                    showError(getString(R.string.channel_name_invalid_characters));
                    return;
                }
                if (Helper.channelExists(channelName)) {
                    showError(getString(R.string.channel_name_already_created));
                    return;
                }

                double depositAmount;
                try {
                    depositAmount = Double.parseDouble(depositString);
                } catch (NumberFormatException ex) {
                    // pass
                    showError(getString(R.string.please_enter_valid_deposit));
                    return;
                }
                if (depositAmount == 0) {
                    String error = getResources().getQuantityString(R.plurals.min_deposit_required, depositAmount == 1 ? 1 : 2, String.valueOf(Helper.MIN_DEPOSIT));
                    showError(error);
                    return;
                }
                if (Lbry.walletBalance == null || Lbry.getAvailableBalance() < depositAmount) {
                    showError(getString(R.string.deposit_more_than_balance));
                    return;
                }

                AccountManager am = AccountManager.get(getContext());
                String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");
                MainActivity activity = (MainActivity) getActivity();

                enableViews(inputChannelName, inputDeposit, createButton, linkCancel, false);
                Helper.setViewVisibility(progressView, View.VISIBLE);

                Callable<Claim> c = new ChannelCreateUpdate(claimToSave, new BigDecimal(depositString), false, authToken);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Claim> future = executor.submit(c);

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Claim result = future.get();

                            if (result != null) {
                                if (!BuildConfig.DEBUG) {
                                    LogPublishTask logPublishTask = new LogPublishTask(result);
                                    logPublishTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }

                                // channel created
                                Bundle bundle = new Bundle();
                                bundle.putString("claim_id", result.getClaimId());
                                bundle.putString("claim_name", result.getName());
                                LbryAnalytics.logEvent(LbryAnalytics.EVENT_CHANNEL_CREATE, bundle);

                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (listener != null) {
                                                listener.onChannelCreated(result);
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (ExecutionException ex) {
                            Throwable cause = ex.getCause();

                            if (cause instanceof ApiCallException) {
                                showError(cause.getMessage());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Helper.setViewVisibility(progressView, View.GONE);
                                        enableViews(inputChannelName, inputDeposit, createButton, linkCancel, true);

                                        dismiss();
                                    }
                                });
                            }

                        }
                    }
                });
                t.start();
            }
        });

        Helper.setViewText((TextView) balanceView, Helper.shortCurrencyFormat(Lbry.getAvailableBalance()));
        return v;
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.destroyChannelCreator();
        }
    }

    private void enableViews(TextInputEditText inputChannelName, TextInputEditText inputDeposit, Button createButton, TextView linkCancel, boolean b) {
        Helper.setViewEnabled(inputChannelName, b);
        Helper.setViewEnabled(inputDeposit, b);
        Helper.setViewEnabled(createButton, b);
        Helper.setViewEnabled(linkCancel, b);
    }

    private void showError(String message) {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showError(message, activity.findViewById(R.id.main_activity_other_fragment));
                }
            });
        }
    }

    public interface ChannelCreateListener {
        void onChannelCreated(Claim claimResult);
    }
}
