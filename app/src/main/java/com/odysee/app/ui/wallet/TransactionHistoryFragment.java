package com.odysee.app.ui.wallet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.TransactionListAdapter;
import com.odysee.app.callable.TransactionList;
import com.odysee.app.model.Transaction;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

public class TransactionHistoryFragment extends BaseFragment implements TransactionListAdapter.TransactionClickListener {

    private static final int TRANSACTION_PAGE_LIMIT = 50;
    private boolean transactionsHaveReachedEnd;
    private boolean transactionsLoading;
    private ProgressBar loading;
    private RecyclerView transactionList;
    private TransactionListAdapter adapter;
    private View noTransactionsView;
    private int currentTransactionPage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        loading = root.findViewById(R.id.transaction_history_loading);
        transactionList = root.findViewById(R.id.transaction_history_list);
        noTransactionsView = root.findViewById(R.id.transaction_history_no_transactions);

        Context context = getContext();
        if (context != null) {
            LinearLayoutManager llm = new LinearLayoutManager(context);
            transactionList.setLayoutManager(llm);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            android.graphics.drawable.Drawable thinDivider = ContextCompat.getDrawable(context, R.drawable.thin_divider);
            if (thinDivider != null) {
                itemDecoration.setDrawable(thinDivider);
            }
            transactionList.addItemDecoration(itemDecoration);
        }

        transactionList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (transactionsLoading) {
                    return;
                }
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int pastVisibleItems = lm.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        if (!transactionsHaveReachedEnd) {
                            // load more
                            currentTransactionPage++;
                            loadTransactions();
                        }
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Transaction History", "TransactionHistory");
        }

        if (adapter != null && adapter.getItemCount() > 0 && transactionList != null) {
            transactionList.setAdapter(adapter);
        }
        loadTransactions();
    }

    private void checkNoTransactions() {
        Helper.setViewVisibility(noTransactionsView, adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void loadTransactions() {
        currentTransactionPage = currentTransactionPage == 0 ? 1 : currentTransactionPage;
        transactionsLoading = true;

        Activity a = getActivity();
        if (a != null) {
            loading.setVisibility(View.VISIBLE);

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Future<List<Transaction>> f = ((OdyseeApp) a.getApplication()).getExecutor().submit(new TransactionList(currentTransactionPage, TRANSACTION_PAGE_LIMIT, Lbryio.AUTH_TOKEN));

                    try {
                        List<Transaction> resultList = f.get();

                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Context context = getContext();

                                transactionsLoading = false;
                                transactionsHaveReachedEnd = resultList.size() < TRANSACTION_PAGE_LIMIT;
                                if (context != null) {
                                    if (adapter == null) {
                                        adapter = new TransactionListAdapter(resultList, context);
                                        adapter.setListener(TransactionHistoryFragment.this);
                                        if (transactionList != null) {
                                            transactionList.setAdapter(adapter);
                                        }
                                    } else {
                                        adapter.addTransactions(resultList);
                                    }
                                }
                            }
                        });
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loading.setVisibility(View.GONE);
                                checkNoTransactions();
                            }
                        });
                    }
                }
            });
            t.start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity activity = (MainActivity) getContext();
        if (activity != null) {
            activity.hideSearchBar();

            activity.setActionBarTitle(R.string.transaction_history);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onTransactionClicked(Transaction transaction) {
        // Don't do anything? Or open the transaction in a browser?
    }
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
}
