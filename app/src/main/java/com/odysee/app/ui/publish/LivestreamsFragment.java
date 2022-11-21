package com.odysee.app.ui.publish;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.text.Html;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.odysee.app.GoLiveActivity;
import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.claim.AbandonHandler;
import com.odysee.app.tasks.claim.AbandonStreamTask;
import com.odysee.app.tasks.claim.ClaimListResultHandler;
import com.odysee.app.tasks.claim.ClaimListTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LivestreamsFragment extends BaseFragment implements ActionMode.Callback, SelectionModeListener {
    private TextInputEditText textServer;
    private TextInputEditText textStreamingKey;
    private TextView linkShowStreamingKey;
    private MaterialButton buttonStartStreaming;
    private ProgressBar loading;
    private RecyclerView contentList;
    private View emptyView;
    private FloatingActionButton fabCreate;

    private ClaimListAdapter adapter;

    private ActionMode actionMode;

    private boolean waitingForConfirmation = true;
    private boolean waitForConfirmationScheduled;
    private List<Claim> pendingClaims;

    private String streamKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_livestreams, container, false);

        contentList = root.findViewById(R.id.livestreams_list);
        contentList.setLayoutManager(new LinearLayoutManager(getContext()));

        textServer = root.findViewById(R.id.livestreams_server_edit_text);
        textStreamingKey = root.findViewById(R.id.livestreams_key_edit_text);
        linkShowStreamingKey = root.findViewById(R.id.livestreams_key_show_link);
        buttonStartStreaming = root.findViewById(R.id.livestreams_start_streaming);
        loading = root.findViewById(R.id.livestreams_list_loading);
        emptyView = root.findViewById(R.id.livestreams_empty_container);
        fabCreate = root.findViewById(R.id.livestreams_create_fab);

        root.findViewById(R.id.livestreams_server_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyEditText(textServer, false, R.string.stream_server_copied);
            }
        });

        root.findViewById(R.id.livestreams_key_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyEditText(textStreamingKey, true, R.string.stream_key_copied);
            }
        });

        linkShowStreamingKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (linkShowStreamingKey.getText() == getString(R.string.show)) {
                    linkShowStreamingKey.setText(R.string.hide);
                    textStreamingKey.setTransformationMethod(null);
                } else {
                    linkShowStreamingKey.setText(R.string.show);
                    textStreamingKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        buttonStartStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getContext();
                if (activity != null) {
                    activity.onBackPressed();
                }

                Intent intent = new Intent(getContext(), GoLiveActivity.class);
                intent.putExtra("streamKey", streamKey);
                startActivity(intent);
            }
        });

        View.OnClickListener createClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openFragment(GoLiveFormFragment.class, true, null);
                }
            }
        };
        root.findViewById(R.id.livestreams_create_button).setOnClickListener(createClickListener);
        fabCreate.setOnClickListener(createClickListener);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getContext();
        if (activity != null) {
            LbryAnalytics.setCurrentScreen(activity, "Livestreams", "Livestreams");
        }
        if (adapter != null && contentList != null) {
            contentList.setAdapter(adapter);
        }
        fetchChannels();
    }

    // region: Waiting for pending livestream claims
    private void waitForPendingClaims() {
        Activity a = getActivity();
        if (a != null && !waitForConfirmationScheduled) {
            ((OdyseeApp) a.getApplication()).getScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, Object> options = new HashMap<>(2);
                        options.put("type", Claim.TYPE_STREAM);
                        options.put("txid", pendingClaims.stream().map(Claim::getTxid).collect(Collectors.toList()));

                        JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(
                                Lbry.METHOD_TXO_LIST, options, Lbryio.AUTH_TOKEN);
                        JSONArray items = result.getJSONArray("items");

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);

                            int confirmations = item.getInt("confirmations");
                            String claimId = item.getString("claim_id");

                            if (confirmations > 0) {
                                adapter.getItems().stream()
                                        .filter(c -> c.getClaimId().equals(claimId))
                                        .findFirst()
                                        .ifPresent(claim -> new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                int position = adapter.getItems().indexOf(claim);
                                                claim.setConfirmations(confirmations);
                                                adapter.notifyItemChanged(position);

                                                pendingClaims.remove(claim);
                                                if (pendingClaims.size() == 0) {
                                                    buttonStartStreaming.setEnabled(true);
                                                }
                                            }
                                        }));
                            }
                        }
                    } catch (ApiCallException | JSONException ex) {
                        // Do nothing, will retry in 30s
                        showError(ex.getMessage());
                    }
                }
            }, 0, 30, TimeUnit.SECONDS);
            waitForConfirmationScheduled = true;
        }
    }
    // endregion

    private void fetchChannels() {
        if (Lbry.ownChannels != null && Lbry.ownChannels.size() > 0) {
            selectChannel(Lbry.ownChannels);
            return;
        }

        Map<String, Object> options = Lbry.buildClaimListOptions(Claim.TYPE_CHANNEL, 1, 999, true);
        loading.setVisibility(View.VISIBLE);
        ClaimListTask task = new ClaimListTask(options, null, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                Lbry.ownChannels = claims;
                selectChannel(Lbry.ownChannels);
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                checkNoLivestreams();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void selectChannel(List<Claim> channels) {
        String defaultChannelName = Helper.getDefaultChannelName(getContext());

        List<Claim> defaultChannel = channels.stream().filter(c -> c != null
                && c.getName().equalsIgnoreCase(defaultChannelName)).collect(Collectors.toList());

        Claim channel;
        if (defaultChannel.size() > 0) {
            channel = defaultChannel.get(0);
        } else {
            channel = channels.get(0);
        }

        fetchLivestreams(channel);
        generateStreamKey(channel);
    }

    private void checkNoLivestreams() {
        Helper.setViewVisibility(emptyView, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void fetchLivestreams(Claim channel) {
        Map<String, Object> options = Lbry.buildClaimListOptions(
                Claim.TYPE_STREAM, 1, 20, true);
        options.put("channel_id", channel.getClaimId());
        options.put("has_no_source", true);

        ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, loading, new ClaimListResultHandler() {
            @Override
            public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                List<Claim> livestreams = Helper.filterDeletedClaims(claims);
                Collections.sort(livestreams, new Comparator<Claim>() {
                    @Override
                    public int compare(Claim a, Claim b) {
                        if (a.getConfirmations() == 0) {
                            return -1;
                        } else if (b.getConfirmations() == 0) {
                            return 1;
                        } else {
                            return Long.compare(b.getTimestamp(), a.getTimestamp());
                        }
                    }
                });

                pendingClaims = livestreams
                        .stream()
                        .filter(claim -> claim.getConfirmations() == 0)
                        .collect(Collectors.toList());
                if (pendingClaims.size() > 0) {
                    waitForPendingClaims();
                } else {
                    waitingForConfirmation = false;
                    buttonStartStreaming.setEnabled(true);
                }

                Context context = getContext();
                if (context != null) {
                    adapter = new ClaimListAdapter(livestreams, context);
                    adapter.setCanEnterSelectionMode(true);
                    adapter.setSelectionModeListener(LivestreamsFragment.this);
                    adapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                        @Override
                        public void onClaimClicked(Claim claim, int position) {
                            if (context instanceof MainActivity) {
                                MainActivity activity = (MainActivity) context;
                                if (claim.getName().startsWith("@")) {
                                    activity.openChannelClaim(claim);
                                } else {
                                    activity.openFileClaim(claim);
                                }
                            }
                        }
                    });
                    if (contentList != null) {
                        contentList.setAdapter(adapter);
                    }
                }
                loading.setVisibility(View.GONE);
                checkNoLivestreams();
            }

            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                checkNoLivestreams();
                loading.setVisibility(View.GONE);
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void generateStreamKey(Claim channel) {
        Activity a = getActivity();
        if (a != null) {
            ((OdyseeApp) a.getApplication()).getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String hexData = Helper.toHexString(channel.getName());
                        Map<String, Object> options = new HashMap<>(2);
                        options.put("channel_id", channel.getClaimId());
                        options.put("hexdata", hexData);

                        JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(
                                "channel_sign", options, Lbryio.AUTH_TOKEN);
                        String signature = result.getString("signature");
                        String signingTs = result.getString("signing_ts");

                        if (!Helper.isNullOrEmpty(signature) && !Helper.isNullOrEmpty(signingTs)) {
                            streamKey = channel.getClaimId()
                                    + "?d=" + hexData
                                    + "&s=" + signature
                                    + "&t=" + signingTs;

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    textStreamingKey.setText(streamKey);
                                    if (!waitingForConfirmation) {
                                        buttonStartStreaming.setEnabled(true);
                                    }
                                }
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    showError(getString(R.string.stream_key_not_generated)));
                        }
                    } catch (ApiCallException | JSONException ex) {
                        ex.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() ->
                                showError(getString(R.string.stream_key_not_generated)));
                    }
                }
            });
        }
    }

    private void copyEditText(EditText text, boolean sensitive, @StringRes int copiedMessageResourceId) {
        Context context = getContext();
        if (context != null) {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(text.getHint(), text.getText());
            if (sensitive) {
                PersistableBundle bundle = new PersistableBundle();
                bundle.putBoolean("android.content.extra.IS_SENSITIVE", true);
                clip.getDescription().setExtras(bundle);
            }
            clipboardManager.setPrimaryClip(clip);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            showMessage(copiedMessageResourceId);
        }
    }

    private void handleDeleteSelectedClaims(List<Claim> selectedClaims) {
        List<String> claimIds = new ArrayList<>();

        for (Claim claim : selectedClaims) {
            claimIds.add(claim.getClaimId());
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        Helper.setViewVisibility(contentList, View.INVISIBLE);
        Helper.setViewVisibility(fabCreate, View.INVISIBLE);
        AbandonStreamTask task = new AbandonStreamTask(claimIds, loading, Lbryio.AUTH_TOKEN, new AbandonHandler() {
            @Override
            public void onComplete(List<String> successfulClaimIds, List<String> failedClaimIds, List<Exception> errors) {
                View root = getView();
                if (root != null) {
                    if (failedClaimIds.size() > 0) {
                        showError(getString(R.string.one_or_more_publishes_failed_abandon));
                    } else if (successfulClaimIds.size() == claimIds.size()) {
                        try {
                            showMessage(getResources().getQuantityString(R.plurals.publishes_deleted, successfulClaimIds.size()));
                        } catch (IllegalStateException ex) {
                            // pass
                        }
                    }
                }

                Lbry.abandonedClaimIds.addAll(successfulClaimIds);
                if (adapter != null) {
                    adapter.setItems(Helper.filterDeletedClaims(adapter.getItems()));
                }

                Helper.setViewVisibility(contentList, View.VISIBLE);
                Helper.setViewVisibility(fabCreate, View.VISIBLE);
                checkNoLivestreams();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onEnterSelectionMode() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.startSupportActionMode(this);
        }
    }

    @Override
    public void onExitSelectionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onItemSelectionToggled() {
        if (actionMode != null) {
            actionMode.setTitle(String.valueOf(adapter.getSelectedCount()));
            actionMode.invalidate();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        this.actionMode = actionMode;
        actionMode.getMenuInflater().inflate(R.menu.menu_claim_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        int selectionCount = adapter != null ? adapter.getSelectedCount() : 0;
        menu.findItem(R.id.action_edit).setVisible(selectionCount == 1 &&
                adapter.getSelectedItems().get(0).getConfirmations() > 0 &&
                Claim.TYPE_STREAM.equalsIgnoreCase(adapter.getSelectedItems().get(0).getValueType()));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        if (R.id.action_edit == menuItem.getItemId()) {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                Claim claim = adapter.getSelectedItems().get(0);
                // start go live form with the claim
                Context context = getContext();
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    activity.openGoLiveForm(claim);
                }

                actionMode.finish();
                return true;
            }
        }
        if (R.id.action_delete == menuItem.getItemId()) {
            Context context = getContext();
            if (context != null && adapter != null && adapter.getSelectedCount() > 0) {
                final List<Claim> selectedClaims = new ArrayList<>(adapter.getSelectedItems());
                String message = getResources().getQuantityString(R.plurals.confirm_delete_publishes, selectedClaims.size());
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).
                        setTitle(R.string.delete_selection).
                        setMessage(Html.fromHtml(message))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                handleDeleteSelectedClaims(selectedClaims);
                            }
                        }).setNegativeButton(R.string.no, null);
                builder.show();
                return true;
            }
        }

        return false;
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
}