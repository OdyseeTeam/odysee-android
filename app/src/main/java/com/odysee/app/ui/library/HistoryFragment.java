package com.odysee.app.ui.library;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.ClaimListAdapter;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.listener.SelectionModeListener;
import com.odysee.app.model.Claim;
import com.odysee.app.model.ViewHistory;
import com.odysee.app.runnable.DeleteViewHistoryItem;
import com.odysee.app.tasks.localdata.FetchViewHistoryTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;

public class HistoryFragment extends BaseFragment implements
        ActionMode.Callback, SelectionModeListener {
    private static final int PAGE_SIZE = 50;

    private ActionMode actionMode;
    private RecyclerView contentList;
    private ClaimListAdapter contentListAdapter;
    private ProgressBar listLoading;
    private View layoutListEmpty;
    private TextView textListEmpty;
    private Date lastDate;
    private boolean listReachedEnd;
    private boolean contentListLoading;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        contentList = root.findViewById(R.id.history_list);
        contentList.setLayoutManager(llm);

        listLoading = root.findViewById(R.id.history_list_loading);

        layoutListEmpty = root.findViewById(R.id.history_empty_container);
        textListEmpty = root.findViewById(R.id.history_list_empty_text);

        contentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (contentListLoading) {
                    return;
                }

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!listReachedEnd) {
                            fetchHistory();
                        }
                    }
                }
            }
        });

        return root;
    }

    public void onResume() {
        super.onResume();

        Context context = getContext();
        Helper.setWunderbarValue(null, context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Library", "Library");
        }

        showHistory();
    }

    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
        }
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity a = (MainActivity) context;
            a.switchClearViewHistoryButton(false);
        }
    }

    private void showHistory() {
        if (actionMode != null) {
            actionMode.finish();
        }
        if (contentListAdapter != null) {
            contentListAdapter.setHideFee(false);
            contentListAdapter.clearItems();
            contentListAdapter.setCanEnterSelectionMode(true);
        }
        listReachedEnd = false;

        lastDate = null;
        fetchHistory();
    }

    private void initContentListAdapter(List<Claim> claims) {
        Context context = getContext();
        if (context != null) {
            contentListAdapter = new ClaimListAdapter(claims, context);
            contentListAdapter.setCanEnterSelectionMode(true);
            contentListAdapter.setSelectionModeListener(this);
            contentListAdapter.setListener(new ClaimListAdapter.ClaimListItemListener() {
                @Override
                public void onClaimClicked(Claim claim, int position) {
                    Context context = getContext();
                    if (context instanceof MainActivity) {
                        MainActivity activity = (MainActivity) context;
                        if (claim.getName().startsWith("@")) {
                            activity.openChannelUrl(claim.getPermanentUrl());
                        } else {
                            activity.openFileUrl(claim.getPermanentUrl());
                        }
                    }
                }
            });
        }
    }


    private void fetchHistory() {
        contentListLoading = true;
        Helper.setViewVisibility(layoutListEmpty, View.GONE);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance();
        if (dbHelper != null) {
            FetchViewHistoryTask task = new FetchViewHistoryTask(lastDate, PAGE_SIZE, dbHelper, new FetchViewHistoryTask.FetchViewHistoryHandler() {
                @Override
                public void onSuccess(List<ViewHistory> history, boolean hasReachedEnd) {
                    listReachedEnd = hasReachedEnd;
                    if (history.size() > 0) {
                        lastDate = history.get(history.size() - 1).getTimestamp();
                    }

                    List<Claim> claims = Helper.claimsFromViewHistory(history);
                    if (contentListAdapter == null) {
                        initContentListAdapter(claims);
                    } else {
                        contentListAdapter.addItems(claims);
                    }
                    if (contentListAdapter != null && contentList.getAdapter() == null) {
                        contentList.setAdapter(contentListAdapter);
                    }
                    checkListEmpty();
                    contentListLoading = false;
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            checkListEmpty();
            contentListLoading = false;
        }
    }

    private void checkListEmpty() {
        layoutListEmpty.setVisibility(contentListAdapter == null || contentListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);

        MainActivity a = (MainActivity) getActivity();
        if (a != null) {
            a.switchClearViewHistoryButton(contentListAdapter != null && contentListAdapter.getItemCount() > 0);
        }
        textListEmpty.setText(R.string.library_no_history);
    }

    @MainThread
    public void onViewHistoryCleared() {
        contentListAdapter.removeItems(contentListAdapter.getItems());
        checkListEmpty();
    }

    private void applyLayoutWeight(View view, int weight) {
        LinearLayout.LayoutParams params =  (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = weight;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        this.actionMode = actionMode;
        actionMode.getMenuInflater().inflate(R.menu.menu_claim_list, menu);
        return true;
    }
    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (contentListAdapter != null) {
            contentListAdapter.clearSelectedItems();
            contentListAdapter.setInSelectionMode(false);
            contentListAdapter.notifyDataSetChanged();
        }
        this.actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode actionMode, Menu menu) {
        menu.findItem(R.id.action_edit).setVisible(false);
        return true;
    }

    @Override
    public boolean onActionItemClicked(androidx.appcompat.view.ActionMode actionMode, MenuItem menuItem) {
        if (R.id.action_delete == menuItem.getItemId()) {
            if (contentListAdapter != null && contentListAdapter.getSelectedCount() > 0) {
                Context context = getContext();
                if (context != null) {
                    final List<Claim> selectedClaims = new ArrayList<>(contentListAdapter.getSelectedItems());
                    handleDeleteSelectedClaims(selectedClaims);
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

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Activity a = getActivity();
        String message = getResources().getQuantityString(R.plurals.confirm_delete_files, selectedClaims.size());
        AlertDialog.Builder builder;
        if (a != null) {
            builder = new AlertDialog.Builder(a);
            builder.setTitle(R.string.delete_selection).setMessage(message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (Claim c : selectedClaims) {
                                        try {
                                            Runnable r = new DeleteViewHistoryItem(c.getPermanentUrl());
                                            Future<?> f = executorService.submit(r);
                                            f.get();
                                            a.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    contentListAdapter.removeItem(c);
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    a.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (actionMode != null) {
                                                actionMode.finish();
                                            }

                                            if (executorService != null && !executorService.isShutdown()) {
                                                executorService.shutdown();
                                            }
                                        }
                                    });
                                }
                            });
                            t.start();
                        }
                    })
                    .setNegativeButton(R.string.no, null);
            builder.show();
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
            actionMode.setTitle(String.valueOf(contentListAdapter.getSelectedCount()));
            actionMode.invalidate();
        }
    }
    public void onExitSelectionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }
}
