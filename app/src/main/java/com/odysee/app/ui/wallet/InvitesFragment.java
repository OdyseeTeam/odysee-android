package com.odysee.app.ui.wallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.InlineChannelSpinnerAdapter;
import com.odysee.app.adapter.InviteeListAdapter;
import com.odysee.app.listener.WalletBalanceListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.model.lbryinc.Invitee;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.tasks.lbryinc.FetchInviteStatusTask;
import com.odysee.app.tasks.lbryinc.FetchReferralCodeTask;
import com.odysee.app.tasks.lbryinc.InviteByEmailTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.ui.channel.ChannelCreateDialogFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

public class InvitesFragment extends BaseFragment implements WalletBalanceListener, ChannelCreateDialogFragment.ChannelCreateListener {

    private static final String INVITE_LINK_FORMAT = "https://odysee.com/$/invite/%s:%s";

    private boolean fetchingChannels;
    private View layoutAccountDriver;
    private TextView textLearnMoreLink;
    private MaterialButton buttonGetStarted;

    private View buttonCopyInviteLink;
    private TextView textInviteLink;
    private TextInputLayout layoutInputEmail;
    private TextInputEditText inputEmail;
    private MaterialButton buttonInviteByEmail;

    private RecyclerView inviteHistoryList;
    private InviteeListAdapter inviteHistoryAdapter;
    private InlineChannelSpinnerAdapter channelSpinnerAdapter;
    private AppCompatSpinner channelSpinner;
    private View progressLoadingChannels;
    private View progressLoadingInviteByEmail;
    private View progressLoadingStatus;

    private CardView rewardDriverCard;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_invites, container, false);

        layoutAccountDriver = root.findViewById(R.id.invites_account_driver_container);
        textLearnMoreLink = root.findViewById(R.id.invites_account_driver_learn_more);
        buttonGetStarted = root.findViewById(R.id.invites_get_started_button);
        rewardDriverCard = root.findViewById(R.id.reward_driver_card);

        textInviteLink = root.findViewById(R.id.invites_invite_link);
        buttonCopyInviteLink = root.findViewById(R.id.invites_copy_invite_link);
        layoutInputEmail = root.findViewById(R.id.invites_email_input_layout);
        inputEmail = root.findViewById(R.id.invites_email_input);
        buttonInviteByEmail = root.findViewById(R.id.invites_email_button);

        progressLoadingChannels = root.findViewById(R.id.invites_loading_channels_progress);
        progressLoadingInviteByEmail = root.findViewById(R.id.invites_loading_invite_by_email_progress);
        progressLoadingStatus = root.findViewById(R.id.invites_loading_status_progress);

        inviteHistoryList = root.findViewById(R.id.invite_history_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        inviteHistoryList.setLayoutManager(llm);

        channelSpinner = root.findViewById(R.id.invites_channel_spinner);

        initUi();

        return root;
    }

    private void initUi() {
        layoutAccountDriver.setVisibility(Lbryio.isSignedIn() ? View.GONE : View.VISIBLE);
        Helper.applyHtmlForTextView(textLearnMoreLink);

        rewardDriverCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openRewards();
                }
            }
        });

        inputEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                layoutInputEmail.setHint(hasFocus ? getString(R.string.email) :
                        Helper.getValue(inputEmail.getText()).length() > 0 ?
                                getString(R.string.email) : getString(R.string.invite_email_placeholder));
            }
        });
        inputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Helper.setViewEnabled(buttonInviteByEmail, charSequence.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        buttonInviteByEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = Helper.getValue(inputEmail.getText());
                if (!email.contains("@")) {
                    showError(getString(R.string.provide_valid_email));
                    return;
                }

                InviteByEmailTask task = new InviteByEmailTask(email, progressLoadingInviteByEmail, new GenericTaskHandler() {
                    @Override
                    public void beforeStart() {
                        Helper.setViewEnabled(buttonInviteByEmail, false);
                    }

                    @Override
                    public void onSuccess() {
                        Snackbar.make(getView(), getString(R.string.invite_sent_to, email), Snackbar.LENGTH_LONG).show();
                        Helper.setViewText(inputEmail, null);
                        Helper.setViewEnabled(buttonInviteByEmail, true);
                        fetchInviteStatus();
                    }

                    @Override
                    public void onError(Exception error) {
                        showError(error.getMessage());
                        Helper.setViewEnabled(buttonInviteByEmail, true);
                    }
                });
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        buttonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).simpleSignIn(R.id.action_home_menu);
                }
            }
        });

        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Object item = adapterView.getItemAtPosition(position);
                if (item instanceof Claim) {
                    Claim claim = (Claim) item;
                    if (claim.isPlaceholder()) {
                        if (!fetchingChannels) {
                            showChannelCreator();
                        }
                    } else {
                        // build invite link
                        updateInviteLink(claim);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        textInviteLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyInviteLink();
            }
        });
        buttonCopyInviteLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyInviteLink();
            }
        });

        channelSpinnerAdapter = new InlineChannelSpinnerAdapter(getContext(), R.layout.spinner_item_channel, new ArrayList<>());
        channelSpinnerAdapter.addPlaceholder(false);
    }

    private void updateInviteLink(Claim claim) {
        LbryUri canonical = LbryUri.tryParse(claim.getCanonicalUrl());
        String link = String.format(INVITE_LINK_FORMAT,
                canonical != null ? String.format("@%s", canonical.getChannelName()) : claim.getName(),
                canonical != null ? canonical.getChannelClaimId() : claim.getClaimId());
        textInviteLink.setText(link);
    }
    private void copyInviteLink() {
        Context context = getContext();
        if (context != null && textInviteLink != null) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("inviteLink", textInviteLink.getText());
            clipboard.setPrimaryClip(data);
        }
        Snackbar.make(getView(), R.string.invite_link_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void updateChannelList(List<Claim> channels) {
        if (channelSpinnerAdapter == null) {
            Context context = getContext();
            channelSpinnerAdapter = new InlineChannelSpinnerAdapter(context, R.layout.spinner_item_channel, new ArrayList<>(channels));
        } else {
            channelSpinnerAdapter.clear();
            channelSpinnerAdapter.addAll(channels);
        }

        channelSpinnerAdapter.addPlaceholder(false);
        channelSpinnerAdapter.notifyDataSetChanged();

        if (channelSpinner != null) {
            channelSpinner.setAdapter(channelSpinnerAdapter);
        }

        if (channelSpinnerAdapter.getCount() > 1) {
            channelSpinner.setSelection(1);
        }
    }

    public void onResume() {
        super.onResume();
        layoutAccountDriver.setVisibility(Lbryio.isSignedIn() ? View.GONE : View.VISIBLE);
        checkRewardsDriver();

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Invites", "Invites");
        }

        fetchInviteStatus();
        fetchChannels();
    }

    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.setWunderbarValue(null);
        }
    }

    public void clearInputFocus() {
        inputEmail.clearFocus();
    }

    public void onStop() {
        clearInputFocus();
        super.onStop();
    }

    private void showChannelCreator() {
        MainActivity activity = (MainActivity) getActivity();

        if (activity != null) {
            activity.showChannelCreator(this);
        }
    }

    @Override
    public void onChannelCreated(Claim claimResult) {
        // add the claim to the channel list and set it as the selected item
        if (channelSpinnerAdapter != null) {
            channelSpinnerAdapter.add(claimResult);
        }
        if (channelSpinner != null && channelSpinnerAdapter != null) {
            channelSpinner.setSelection(channelSpinnerAdapter.getCount() - 1);
        }

        if (channelSpinner != null) {
            View formRoot = (View) channelSpinner.getParent().getParent();
            formRoot.setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.has_channels).setVisibility(View.VISIBLE);
            formRoot.findViewById(R.id.no_channels).setVisibility(View.GONE);
        }
    }

    private void fetchDefaultInviteLink() {
        FetchReferralCodeTask task = new FetchReferralCodeTask(null, new FetchReferralCodeTask.FetchReferralCodeHandler() {
            @Override
            public void onSuccess(String referralCode) {
                String previousLink = Helper.getValue(textInviteLink.getText());
                if (Helper.isNullOrEmpty(previousLink)) {
                    Helper.setViewText(textInviteLink, String.format("https://lbry.tv/$/invite/%s", referralCode));
                }
            }

            @Override
            public void onError(Exception error) {
                // pass
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void disableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, false);
    }
    private void enableChannelSpinner() {
        Helper.setViewEnabled(channelSpinner, true);
        Claim selectedClaim = (Claim) channelSpinner.getSelectedItem();
        if (selectedClaim != null) {
            if (selectedClaim.isPlaceholder()) {
                showChannelCreator();
            }
        }
    }

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            updateChannelList(Lbry.ownChannels);
            return;
        }

        fetchingChannels = true;
        disableChannelSpinner();
        ClaimListTask task = new ClaimListTask(Claim.TYPE_CHANNEL, progressLoadingChannels, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims) {
                Lbry.ownChannels = new ArrayList<>(claims);
                updateChannelList(Lbry.ownChannels);
                if (Lbry.ownChannels == null || Lbry.ownChannels.size() == 0) {
                    fetchDefaultInviteLink();
                }
                enableChannelSpinner();
                fetchingChannels = false;
            }

            @Override
            public void onError(Exception error) {
                fetchDefaultInviteLink();
                enableChannelSpinner();
                fetchingChannels = false;
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void fetchInviteStatus() {
        FetchInviteStatusTask task = new FetchInviteStatusTask(progressLoadingStatus, new FetchInviteStatusTask.FetchInviteStatusHandler() {
            @Override
            public void onSuccess(List<Invitee> invitees) {
                if (inviteHistoryAdapter == null) {
                    inviteHistoryAdapter = new InviteeListAdapter(invitees, getContext());
                    inviteHistoryAdapter.addHeader();
                } else {
                    inviteHistoryAdapter.addInvitees(invitees);
                }
                if (inviteHistoryList != null) {
                    inviteHistoryList.setAdapter(inviteHistoryAdapter);
                }
                Helper.setViewVisibility(inviteHistoryList,
                        inviteHistoryAdapter == null || inviteHistoryAdapter.getItemCount() < 2 ? View.GONE : View.VISIBLE
                );
            }

            @Override
            public void onError(Exception error) {
                Helper.setViewVisibility(inviteHistoryList,
                        inviteHistoryAdapter == null || inviteHistoryAdapter.getItemCount() < 2 ? View.GONE : View.VISIBLE
                );
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onWalletBalanceUpdated(WalletBalance walletBalance) {
        checkRewardsDriver();
    }

    private void checkRewardsDriver() {
        Context ctx = getContext();
        View root = getView();
        if (ctx != null && root != null) {
            Helper.setViewText(root.findViewById(R.id.reward_driver_text), R.string.earn_credits_for_inviting);
        }
    }
}
