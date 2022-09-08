package com.odysee.app.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatSpinner;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Setter;

public class PublishPlaylistDialogFragment extends BottomSheetDialogFragment implements WalletBalanceListener {
    public static final String TAG = "PublishPlaylistDialog";

    private MaterialButton buttonPublish;
    private View linkCancel;
    private TextInputEditText inputDeposit;
    private View inlineBalanceContainer;
    private TextView inlineBalanceValue;
    private ProgressBar publishProgress;
    private boolean requestInProgress;
    private TextView textTitle;

    private AppCompatSpinner channelSpinner;
    private InlineChannelSpinnerAdapter channelSpinnerAdapter;
    private TextView textNamePrefix;
    private EditText inputName;
    private TextInputEditText inputTitle;
    private TextView linkToggleAdvanced;
    private View advancedContainer;

    private final PublishPlaylistListener listener;
    //private final Claim claim;

    @Setter
    private OdyseeCollection collection;

    private PublishPlaylistDialogFragment(OdyseeCollection collection, PublishPlaylistListener listener) {
        super();
        this.listener = listener;
        this.collection = collection;

    }

    public static PublishPlaylistDialogFragment newInstance(OdyseeCollection collection, PublishPlaylistListener listener) {
        return new PublishPlaylistDialogFragment(collection, listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_publish_playlist, container, false);

        buttonPublish = view.findViewById(R.id.publish_playlist_button);
        linkCancel = view.findViewById(R.id.publish_playlist_cancel_link);
        inputDeposit = view.findViewById(R.id.publish_playlist_input_deposit);
        inlineBalanceContainer = view.findViewById(R.id.publish_playlist_inline_balance_container);
        inlineBalanceValue = view.findViewById(R.id.publish_playlist_inline_balance_value);
        publishProgress = view.findViewById(R.id.publish_playlist_progress);
        textTitle = view.findViewById(R.id.publish_playlist_title);

        channelSpinner = view.findViewById(R.id.publish_playlist_channel_spinner);
        textNamePrefix = view.findViewById(R.id.publish_playlist_name_prefix);
        inputName = view.findViewById(R.id.publish_playlist_name_input);
        inputTitle = view.findViewById(R.id.publish_playlist_title_input);
        linkToggleAdvanced = view.findViewById(R.id.publish_playlist_toggle_advanced);
        advancedContainer = view.findViewById(R.id.publish_playlist_advanced_container);

        //textTitle.setText(getString(R.string.publish_playlist_title, claim.getTitle()));
        inputName.setText(collection.getActualClaim() != null ? collection.getActualClaim().getName() : collection.getName());
        inputName.setEnabled(Helper.isNullOrEmpty(collection.getClaimId())); // check edit mode
        inputTitle.setText(collection.getName());
        inputDeposit.setText(R.string.min_deposit);

        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Object item = adapterView.getItemAtPosition(position);
                if (item instanceof Claim) {
                    Claim claim = (Claim) item;
                    textNamePrefix.setText(String.format("%s%s/", LbryUri.ODYSEE_COM_BASE_URL, claim.getName()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        inputDeposit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                inputDeposit.setHint(hasFocus ? getString(R.string.zero) : "");
                inlineBalanceContainer.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });

        linkCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        linkToggleAdvanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (advancedContainer.getVisibility() != View.VISIBLE) {
                    advancedContainer.setVisibility(View.VISIBLE);
                    linkToggleAdvanced.setText(R.string.hide_advanced);
                } else {
                    advancedContainer.setVisibility(View.GONE);
                    linkToggleAdvanced.setText(R.string.show_advanced);
                }
            }
        });

        buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestInProgress) {
                    return;
                }
                validateAndPublishPlaylist();
            }
        });

        onWalletBalanceUpdated(Lbry.walletBalance);

        return view;
    }

    public boolean isEditMode() {
        return !Helper.isNullOrEmpty(collection.getClaimId());
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).addWalletBalanceListener(this);
        }
        fetchChannels();
    }

    @Override
    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            ((MainActivity) context).removeWalletBalanceListener(this);
        }
        inputDeposit.clearFocus();
        super.onPause();
    }


    private void fetchChannels() {
        if (Lbry.ownChannels == null || Lbry.ownChannels.size() == 0) {
            preRequest();
            Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
            ClaimListTask task = new ClaimListTask(options, publishProgress, new ClaimListResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                    Lbry.ownChannels = new ArrayList<>(claims);
                    loadChannels(claims);
                    postRequest();
                }

                @Override
                public void onError(Exception error) {
                    // could not fetch channels
                    showError(error.getMessage());
                    dismiss();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            loadChannels(Lbry.ownChannels);
        }
    }

    private void loadChannels(List<Claim> channels) {
        if (channelSpinnerAdapter == null) {
            Context context = getContext();
            if (context != null) {
                channelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, channels);
                channelSpinnerAdapter.notifyDataSetChanged();
            }
        } else {
            channelSpinnerAdapter.clear();
            channelSpinnerAdapter.addAll(channels);
            channelSpinnerAdapter.notifyDataSetChanged();
        }
        if (channelSpinner != null && channelSpinnerAdapter != null) {
            channelSpinner.setAdapter(channelSpinnerAdapter);
        }
    }

    @Override
    public void onWalletBalanceUpdated(WalletBalance walletBalance) {
        if (walletBalance != null && inlineBalanceValue != null) {
            inlineBalanceValue.setText(Helper.shortCurrencyFormat(walletBalance.getAvailable().doubleValue()));
        }
    }

    private void validateAndPublishPlaylist() {
        String name = Helper.getValue(inputName.getText());
        if (Helper.isNullOrEmpty(name) || !LbryUri.isNameValid(name)) {
            showError(getString(R.string.playlist_name_invalid_characters));
            return;
        }

        String depositString = Helper.getValue(inputDeposit.getText());
        if (Helper.isNullOrEmpty(depositString)) {
            showError(getString(R.string.invalid_amount));
            return;
        }

        final String title = Helper.getValue(inputTitle.getText());
        if (Helper.isNullOrEmpty(title)) {
            showError(getString(R.string.publish_playlist_title_required));
            return;
        }

        try {
            BigDecimal bid = new BigDecimal(depositString);
            if (bid.doubleValue() > Lbry.getAvailableBalance()) {
                showError(getString(R.string.insufficient_balance));
                return;
            }
            if (bid.doubleValue() < Helper.MIN_DEPOSIT) {
                String message = getResources().getQuantityString(R.plurals.min_deposit_required, 2, String.valueOf(Helper.MIN_DEPOSIT));
                showError(message);
                return;
            }
        } catch (NumberFormatException ex) {
            showError(getString(R.string.invalid_amount));
            return;
        }

        Claim channel = (Claim) channelSpinner.getSelectedItem();
        if (channel == null) {
            showError(getString(R.string.please_select_publish_channel));
            return;
        }

        if (collection.getItems() == null || collection.getItems().size() == 0) {
            showError(getString(R.string.playlist_at_least_one_item_required));
            return;
        }

        if (!isEditMode() && Helper.claimNameExists(name)) {
            showError(getString(R.string.name_already_used));
            return;
        }

        // Perform the publish operation
        preRequest();
        Claim claimToPublish = new Claim();
        claimToPublish.setName(name);
        claimToPublish.setAmount(depositString);

        List<String> collectionItems = collection.getItems();
        List<String> claimIds = new ArrayList<>(collectionItems.size());
        for (String item : collectionItems) {
            LbryUri url = LbryUri.tryParse(item);
            if (url != null) {
                claimIds.add(url.getClaimId());
            }
        }
        claimToPublish.setClaimIds(claimIds);

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            Helper.setViewVisibility(publishProgress, View.VISIBLE);
            ExecutorService executor = ((OdyseeApp) activity.getApplication()).getExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // Try to obtain a thumbnail by resolving the first item in the playlist (if no thumbnail was already set)
                    if (!isEditMode() || collection.getActualClaim() == null ||
                        collection.getActualClaim() != null && Helper.isNullOrEmpty(collection.getActualClaim().getThumbnailUrl())) {
                        
                    }

                    DecimalFormat amountFormat = new DecimalFormat(Helper.SDK_AMOUNT_FORMAT, new DecimalFormatSymbols(Locale.US));

                    Map<String, Object> options = new HashMap<>();
                    options.put("blocking", true);
                    options.put("bid", amountFormat.format(new BigDecimal(claimToPublish.getAmount()).doubleValue()));
                    options.put("title", title);
                    options.put("claims", claimToPublish.getClaimIds());
                    if (!isEditMode()) {
                        options.put("name", name);
                    } else {
                        options.put("claim_id", collection.getClaimId());
                    }

                    Claim claimResult = null;
                    try {
                        JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(
                                isEditMode() ? Lbry.METHOD_COLLECTION_UPDATE : Lbry.METHOD_COLLECTION_CREATE, options, Lbryio.AUTH_TOKEN);
                        if (result.has("outputs")) {
                            JSONArray outputs = result.getJSONArray("outputs");
                            for (int i = 0; i < outputs.length(); i++) {
                                JSONObject output = outputs.getJSONObject(i);
                                if (output.has("claim_id") && output.has("claim_op")) {
                                    claimResult = Claim.claimFromOutput(output);
                                    break;
                                }
                            }
                        }

                        final Claim fClaimResult = claimResult;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onPlaylistClaimPublished(fClaimResult);
                            }
                        });
                    } catch (ApiCallException | ClassCastException | JSONException ex) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onPlaylistClaimPublishError(ex);
                            }
                        });
                    }
                }
            });
        }
    }

    private void preRequest() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
        }

        requestInProgress = true;
        Helper.setViewVisibility(publishProgress, View.VISIBLE);

        Helper.setViewEnabled(inputName, false);
        Helper.setViewEnabled(inputTitle, false);
        Helper.setViewEnabled(inputDeposit, false);
        Helper.setViewEnabled(channelSpinner, false);
        Helper.setViewEnabled(buttonPublish, false);
        Helper.setViewEnabled(linkCancel, false);
        Helper.setViewVisibility(linkToggleAdvanced, View.INVISIBLE);
    }

    private void postRequest() {
        requestInProgress = false;
        Helper.setViewVisibility(publishProgress, View.GONE);

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
        }
        Helper.setViewEnabled(inputName, !isEditMode());
        Helper.setViewEnabled(inputTitle, true);
        Helper.setViewEnabled(inputDeposit, true);
        Helper.setViewEnabled(channelSpinner, true);
        Helper.setViewEnabled(buttonPublish, true);
        Helper.setViewEnabled(linkCancel, true);
        Helper.setViewVisibility(linkToggleAdvanced, View.VISIBLE);
    }

    private void onPlaylistClaimPublished(Claim claimResult) {
        if (listener != null) {
            listener.onPlaylistPublished(claimResult);
        }
        postRequest();
        dismiss();
    }

    private void onPlaylistClaimPublishError(Exception ex) {
        postRequest();
        showError(getString(R.string.publish_playlist_failed, ex.getMessage()));
    }

    private void showError(String message) {
        View view = getView();
        if (view != null && !Helper.isNullOrEmpty(message)) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).
                    setBackgroundTint(Color.RED).
                    setTextColor(Color.WHITE).
                    show();
        }
    }

    public interface PublishPlaylistListener {
        void onPlaylistPublished(Claim claim);
    }
}
