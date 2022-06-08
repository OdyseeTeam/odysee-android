package com.odysee.app.ui.channel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.NavMenuItem;
import com.odysee.app.supplier.ClaimListSupplier;
import com.odysee.app.tasks.claim.AbandonChannelTask;
import com.odysee.app.tasks.claim.AbandonHandler;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

public class ChannelManagerFragment extends BaseFragment implements ActionMode.Callback, SelectionModeListener {

    private Button buttonNewChannel;
    private FloatingActionButton fabNewChannel;
    private ActionMode actionMode;
    private View emptyView;
    private ProgressBar loading;
    private ProgressBar bigLoading;
    private RecyclerView channelList;
    private ClaimListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_channel_manager, container, false);

        buttonNewChannel = root.findViewById(R.id.channel_manager_create_button);
        fabNewChannel = root.findViewById(R.id.channel_manager_fab_new_channel);
        buttonNewChannel.setOnClickListener(newChannelClickListener);
        fabNewChannel.setOnClickListener(newChannelClickListener);

        emptyView = root.findViewById(R.id.channel_manager_empty_container);
        channelList = root.findViewById(R.id.channel_manager_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        channelList.setLayoutManager(llm);
        loading = root.findViewById(R.id.channel_manager_list_loading);
        bigLoading = root.findViewById(R.id.channel_manager_list_big_loading);

        return root;
    }

    private final View.OnClickListener newChannelClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).openChannelForm(null);
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Channel Manager", "ChannelManager");
            MainActivity.suspendGlobalPlayer(context);
        }
        if (adapter != null && channelList != null) {
            channelList.setAdapter(adapter);
        }
        fetchChannels();
    }

    public void onPause() {
        MainActivity.resumeGlobalPlayer(getContext());
        super.onPause();
    }

    public View getLoading() {
        return (adapter == null || adapter.getItemCount() == 0) ? bigLoading : loading;
    }

    private void checkNoChannels() {
        Helper.setViewVisibility(emptyView, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void fetchChannels() {
        Helper.setViewVisibility(emptyView, View.GONE);
        AccountManager am = AccountManager.get(getContext());
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        String authToken = am.peekAuthToken(odyseeAccount, "auth_token_type");

        MainActivity activity = (MainActivity) getActivity();

        final View progressView = getLoading();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            progressView.setVisibility(View.VISIBLE);
            Supplier<List<Claim>> s = new ClaimListSupplier(Collections.singletonList(Claim.TYPE_CHANNEL), authToken);
            CompletableFuture<List<Claim>> cf = CompletableFuture.supplyAsync(s);
            cf.whenComplete((result, e) -> {
                if (e != null && activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Throwable t = e.getCause();
                            if (t != null) {
                                showError(t.getMessage());
                            }
                        }
                    });
                }

                if (result != null && activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Lbry.ownChannels = Helper.filterDeletedClaims(new ArrayList<>(result));
                            if (adapter == null) {
                                Context context = getContext();
                                if (context != null) {
                                    adapter = new ClaimListAdapter(result, context);
                                    adapter.setCanEnterSelectionMode(true);
                                    adapter.setSelectionModeListener(ChannelManagerFragment.this);
                                    adapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                                        @Override
                                        public void onClaimClicked(Claim claim, int position) {
                                            if (context instanceof MainActivity) {
                                                ((MainActivity) context).openChannelClaim(claim);
                                            }
                                        }
                                    });
                                    if (channelList != null) {
                                        channelList.setAdapter(adapter);
                                    }
                                }
                            } else {
                                adapter.setItems(result);
                            }

                            checkNoChannels();
                        }
                    });
                }

                if (activity != null)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressView.setVisibility(View.GONE);
                        }
                    });
            });
        } else {
            Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
            ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, getLoading(), new ClaimListResultHandler() {
                @Override
                public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                    Lbry.ownChannels = Helper.filterDeletedClaims(new ArrayList<>(claims));
                    if (adapter == null) {
                        Context context = getContext();
                        if (context != null) {
                            adapter = new ClaimListAdapter(claims, context);
                            adapter.setCanEnterSelectionMode(true);
                            adapter.setSelectionModeListener(ChannelManagerFragment.this);
                            adapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                                @Override
                                public void onClaimClicked(Claim claim, int position) {
                                    if (context instanceof MainActivity) {
                                        ((MainActivity) context).openChannelClaim(claim);
                                    }
                                }
                            });
                            if (channelList != null) {
                                channelList.setAdapter(adapter);
                            }
                        }
                    } else {
                        adapter.setItems(claims);
                    }

                    checkNoChannels();
                }

                @Override
                public void onError(Exception error) {
                    checkNoChannels();
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void onEnterSelectionMode() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.startSupportActionMode(this);
        }
    }
    public void onItemSelectionToggled() {
        if (actionMode != null) {
            actionMode.setTitle(String.valueOf(adapter.getSelectedCount()));
            actionMode.invalidate();
        }
    }
    public void onExitSelectionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        this.actionMode = actionMode;
        actionMode.getMenuInflater().inflate(R.menu.menu_claim_list, menu);
        return true;
    }
    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (adapter != null) {
            adapter.clearSelectedItems();
            adapter.setInSelectionMode(false);
            adapter.notifyDataSetChanged();
        }
        this.actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode actionMode, Menu menu) {
        int selectionCount = adapter != null ? adapter.getSelectedCount() : 0;
        menu.findItem(R.id.action_edit).setVisible(selectionCount == 1);
        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode actionMode, MenuItem menuItem) {
        if (R.id.action_edit == menuItem.getItemId()) {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                Claim claim = adapter.getSelectedItems().get(0);
                // start channel editor with the claim
                Context context = getContext();
                if (context instanceof MainActivity) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("claim", claim);
                    ((MainActivity) context).openFragment(ChannelFormFragment.class, true, params);
                }

                actionMode.finish();
                return true;
            }
        }
        if (R.id.action_delete == menuItem.getItemId()) {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                final List<Claim> selectedClaims = new ArrayList<>(adapter.getSelectedItems());
                String message = getResources().getQuantityString(R.plurals.confirm_delete_channels, selectedClaims.size());
                Context c = getContext();
                if (c != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(c).
                            setTitle(R.string.delete_selection).
                            setMessage(message)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    handleDeleteSelectedClaims(selectedClaims);
                                }
                            }).setNegativeButton(R.string.no, null);
                    builder.show();
                }
                return true;
            }
        }

        return false;
    }

    private void handleDeleteSelectedClaims(List<Claim> selectedClaims) {
        List<String> claimIds = new ArrayList<>();

        for (Claim claim : selectedClaims) {
            claimIds.add(claim.getClaimId());
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        Helper.setViewVisibility(channelList, View.INVISIBLE);
        Helper.setViewVisibility(fabNewChannel, View.INVISIBLE);
        AbandonChannelTask task = new AbandonChannelTask(claimIds, bigLoading, Lbryio.AUTH_TOKEN, new AbandonHandler() {
            @Override
            public void onComplete(List<String> successfulClaimIds, List<String> failedClaimIds, List<Exception> errors) {
                View root = getView();
                if (root != null) {
                    if (failedClaimIds.size() > 0) {
                        showError(getString(R.string.one_or_more_channels_failed_abandon));
                    } else if (successfulClaimIds.size() == claimIds.size()) {
                        try {
                            showMessage(getResources().getQuantityString(R.plurals.channels_deleted, successfulClaimIds.size()));
                        } catch (IllegalStateException ex) {
                            // pass
                        }
                    }
                }

                Lbry.abandonedClaimIds.addAll(successfulClaimIds);
                if (adapter != null) {
                    adapter.setItems(Helper.filterDeletedClaims(adapter.getItems()));
                }

                Helper.setViewVisibility(channelList, View.VISIBLE);
                Helper.setViewVisibility(fabNewChannel, View.VISIBLE);
                checkNoChannels();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
