package com.odysee.app.ui.publish;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
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

public class PublishesFragment extends BaseFragment implements ActionMode.Callback, SelectionModeListener {

    private Button buttonNewPublish;
    private FloatingActionButton fabNewPublish;
    private ActionMode actionMode;
    private View emptyView;
    private View layoutSdkInitializing;
    private ProgressBar loading;
    private RecyclerView contentList;
    private ClaimListAdapter adapter;
    private boolean contentLoading;
    private boolean contentHasReachedEnd;
    private int contentCurrentPage = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_publishes, container, false);

        buttonNewPublish = root.findViewById(R.id.publishes_create_button);
        fabNewPublish = root.findViewById(R.id.publishes_fab_new_publish);
        buttonNewPublish.setOnClickListener(newPublishClickListener);
        fabNewPublish.setOnClickListener(newPublishClickListener);
        emptyView = root.findViewById(R.id.publishes_empty_container);
        loading = root.findViewById(R.id.publishes_list_loading);

        contentList = root.findViewById(R.id.publishes_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        contentList.setLayoutManager(llm);
        contentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (contentLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount && !contentHasReachedEnd) {
                        // load more
                        contentCurrentPage++;
                        fetchPublishes();
                    }
                }
            }
        });

        return root;
    }

    private final View.OnClickListener newPublishClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).openFragment(PublishFragment.class, true, null);
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
            activity.setWunderbarValue(null);
            activity.clearActionBarTitle();
            LbryAnalytics.setCurrentScreen(activity, "Publishes", "Publishes");
        }
        if (adapter != null && contentList != null) {
            contentList.setAdapter(adapter);
        }
        fetchPublishes();
    }

    private void checkNoPublishes() {
        Helper.setViewVisibility(emptyView, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void fetchPublishes() {
        contentLoading = true;
        Helper.setViewVisibility(emptyView, View.GONE);
        Map<String, Object> options = Lbry.buildClaimListOptions(
                Arrays.asList(Claim.TYPE_STREAM, Claim.TYPE_REPOST), contentCurrentPage, 5, true);
        ClaimListTask task = new ClaimListTask(options, Lbryio.AUTH_TOKEN, loading, new ClaimListResultHandler() {
                    @Override
                    public void onSuccess(List<Claim> claims, boolean hasReachedEnd) {
                        Lbry.ownClaims = Helper.filterDeletedClaims(new ArrayList<>(claims));
                        if (adapter == null) {
                            Context context = getContext();
                            if (context != null) {
                                adapter = new ClaimListAdapter(claims, context);
                                adapter.setCanEnterSelectionMode(true);
                                adapter.setSelectionModeListener(PublishesFragment.this);
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
                        } else {
                            adapter.addItems(claims);
                        }

                        contentHasReachedEnd = hasReachedEnd;
                        contentLoading = false;
                        checkNoPublishes();
                    }

                    @Override
                    public void onError(Exception error) {
                        checkNoPublishes();
                    }
                });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        menu.findItem(R.id.action_edit).setVisible(selectionCount == 1 &&
                Claim.TYPE_STREAM.equalsIgnoreCase(adapter.getSelectedItems().get(0).getValueType()));
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
                    ((MainActivity) context).openPublishForm(claim);
                }

                actionMode.finish();
                return true;
            }
        }
        if (R.id.action_delete == menuItem.getItemId()) {
            if (adapter != null && adapter.getSelectedCount() > 0) {
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

    private void handleDeleteSelectedClaims(List<Claim> selectedClaims) {
        List<String> claimIds = new ArrayList<>();

        for (Claim claim : selectedClaims) {
            claimIds.add(claim.getClaimId());
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        Helper.setViewVisibility(contentList, View.INVISIBLE);
        Helper.setViewVisibility(fabNewPublish, View.INVISIBLE);
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
                Helper.setViewVisibility(fabNewPublish, View.VISIBLE);
                checkNoPublishes();
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
