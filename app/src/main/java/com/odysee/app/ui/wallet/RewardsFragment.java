package com.odysee.app.ui.wallet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.adapter.RewardListAdapter;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.supplier.ClaimRewardSupplier;
import com.odysee.app.supplier.FetchRewardsSupplier;
import com.odysee.app.tasks.lbryinc.ClaimRewardTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RewardsFragment extends BaseFragment implements RewardListAdapter.RewardClickListener {

    private boolean rewardClaimInProgress;

    private ProgressBar rewardsLoading;
    private RewardListAdapter adapter;
    private RecyclerView rewardList;
    private TextView linkFilterUnclaimed;
    private TextView linkFilterAll;
    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_rewards, container, false);

        linkFilterUnclaimed = root.findViewById(R.id.rewards_filter_link_unclaimed);
        linkFilterAll = root.findViewById(R.id.rewards_filter_link_all);
        rewardList = root.findViewById(R.id.rewards_list);
        rewardsLoading = root.findViewById(R.id.rewards_list_loading);

        Context context = getContext();
        LinearLayoutManager llm = new LinearLayoutManager(context);
        rewardList.setLayoutManager(llm);
        adapter = new RewardListAdapter(Lbryio.allRewards, context);
        adapter.setClickListener(this);
        adapter.setDisplayMode(RewardListAdapter.DISPLAY_MODE_UNCLAIMED);
        rewardList.setAdapter(adapter);

        initUi();

        return root;
    }


    public void onResume() {
        super.onResume();
        fetchRewards();

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Rewards", "Rewards");
        }
    }

    public void onStart() {
        super.onStart();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.setWunderbarValue(null);
        }
    }

    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
        }
        super.onStop();
    }

    private void fetchRewards() {
        Helper.setViewVisibility(rewardList, View.INVISIBLE);
        rewardsLoading.setVisibility(View.VISIBLE);

        Activity activity = getActivity();

        String authToken;
        AccountManager am = AccountManager.get(getContext());
        Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());
        if (odyseeAccount != null) {
            authToken = am.peekAuthToken(odyseeAccount, "auth_token_type");
        } else {
            authToken = "";
        }

        Map<String, String> options = new HashMap<>();
        options.put("multiple_rewards_per_type", "true");
        if (odyseeAccount != null && authToken != null) {
            options.put("auth_token", authToken);
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<List<Reward>> supplier = new FetchRewardsSupplier(options);
            CompletableFuture<List<Reward>> cf = CompletableFuture.supplyAsync(supplier, executorService);
            cf.exceptionally(e -> {
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) activity).showError(e.getMessage());
                        }
                    });
                }
                return null;
            }).thenAccept(rewards -> {
                Lbryio.updateRewardsLists(rewards);

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            rewardsLoading.setVisibility(View.GONE);
                            if (adapter == null) {
                                adapter = new RewardListAdapter(rewards, getContext());
                                adapter.setClickListener(RewardsFragment.this);
                                adapter.setDisplayMode(RewardListAdapter.DISPLAY_MODE_UNCLAIMED);
                                rewardList.setAdapter(adapter);
                            } else {
                                adapter.setRewards(rewards);
                            }
                            Helper.setViewVisibility(rewardList, View.VISIBLE);
                        }
                    });
                }
            });
        } else {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Callable<List<Reward>> callable = new Callable<List<Reward>>() {
                        @Override
                        public List<Reward> call() {
                            List<Reward> rewards = null;
                            try {
                                JSONArray results = (JSONArray) Lbryio.parseResponse(Lbryio.call("reward", "list", options, null));
                                rewards = new ArrayList<>();
                                if (results != null) {
                                    for (int i = 0; i < results.length(); i++) {
                                        rewards.add(Reward.fromJSONObject(results.getJSONObject(i)));
                                    }
                                }
                            } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException ex) {
                                if (activity != null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((MainActivity) activity).showError(ex.getMessage());
                                        }
                                    });
                                }
                            }

                            return rewards;
                        }
                    };

                    Future<List<Reward>> future = executorService.submit(callable);

                    try {
                        List<Reward> rewards = future.get();

                        if (rewards != null) {
                            Lbryio.updateRewardsLists(rewards);

                            if (activity != null) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        rewardsLoading.setVisibility(View.GONE);
                                        if (adapter == null) {
                                            adapter = new RewardListAdapter(rewards, getContext());
                                            adapter.setClickListener(RewardsFragment.this);
                                            adapter.setDisplayMode(RewardListAdapter.DISPLAY_MODE_UNCLAIMED);
                                            rewardList.setAdapter(adapter);
                                        } else {
                                            adapter.setRewards(rewards);
                                        }
                                        Helper.setViewVisibility(rewardList, View.VISIBLE);
                                    }
                                });
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    private void initUi() {
        linkFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkFilterUnclaimed.setTypeface(null, Typeface.NORMAL);
                linkFilterAll.setTypeface(null, Typeface.BOLD);
                adapter.setDisplayMode(RewardListAdapter.DISPLAY_MODE_ALL);
                if (adapter.getItemCount() == 1) {
                    fetchRewards();
                }
            }
        });
        linkFilterUnclaimed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkFilterUnclaimed.setTypeface(null, Typeface.BOLD);
                linkFilterAll.setTypeface(null, Typeface.NORMAL);
                adapter.setDisplayMode(RewardListAdapter.DISPLAY_MODE_UNCLAIMED);
                if (adapter.getItemCount() == 1) {
                    fetchRewards();
                }
            }
        });
    }

    @Override
    public void onRewardClicked(Reward reward, View loadingView) {
        if (rewardClaimInProgress || reward.isCustom()) {
            return;
        }
        claimReward(reward.getRewardType(), null, null, null, loadingView);
    }

    @Override
    public void onCustomClaimButtonClicked(String code, EditText inputCustomCode, MaterialButton buttonClaim, View loadingView) {
        if (rewardClaimInProgress) {
            return;
        }
        claimReward(Reward.TYPE_REWARD_CODE, code, inputCustomCode, buttonClaim, loadingView);
    }

    private void claimReward(String type, String code, EditText inputClaimCode, MaterialButton buttonClaim, View loadingView) {
        rewardClaimInProgress = true;
        Helper.setViewEnabled(buttonClaim, false);
        Helper.setViewEnabled(inputClaimCode, false);
        Activity activity = getActivity();

        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingView.setVisibility(View.VISIBLE);
                }
            });
        }

        final AccountManager am = AccountManager.get(getContext());
        final String authToken = am.peekAuthToken(Helper.getOdyseeAccount(am.getAccounts()), "auth_token_type");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<JSONObject> s = new ClaimRewardSupplier(type, code, authToken);
            CompletableFuture<JSONObject> cf = CompletableFuture.supplyAsync(s);
            cf.whenComplete((result, e) -> {
                afterClaimingReward(inputClaimCode, buttonClaim, loadingView, activity, result, e);
            });
        } else {
            ClaimRewardTask task = new ClaimRewardTask(type, code, authToken, new ClaimRewardTask.ClaimRewardHandler() {
                @Override
                public void onSuccess(double amountClaimed, String message) {
                    loadingView.setVisibility(View.INVISIBLE);
                    if (Helper.isNullOrEmpty(message)) {
                        message = getResources().getQuantityString(
                                R.plurals.claim_reward_message,
                                amountClaimed == 1 ? 1 : 2,
                                new DecimalFormat(Helper.LBC_CURRENCY_FORMAT_PATTERN).format(amountClaimed));
                    }
                    View view = getView();
                    if (view != null) {
                        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                    }
                    Helper.setViewEnabled(buttonClaim, true);
                    Helper.setViewEnabled(inputClaimCode, true);
                    rewardClaimInProgress = false;

                    fetchRewards();
                }

                @Override
                public void onError(Exception error) {
                    loadingView.setVisibility(View.INVISIBLE);
                    View view = getView();
                    if (view != null && error != null && !Helper.isNullOrEmpty(error.getMessage())) {
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG).setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                    }
                    Helper.setViewEnabled(buttonClaim, true);
                    Helper.setViewEnabled(inputClaimCode, true);
                    rewardClaimInProgress = false;
                }
            });
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void afterClaimingReward(EditText inputClaimCode, MaterialButton buttonClaim, View loadingView, Activity activity, JSONObject result, Throwable e) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rewardClaimInProgress = false;
                    loadingView.setVisibility(View.INVISIBLE);
                    if (e != null) {
                        String message;
                        // Exception is wrapped into an CompletionException
                        Throwable throwable = e.getCause();
                        if (throwable != null) {
                            message = throwable.getMessage();
                        } else {
                            message = e.getMessage();
                        }
                        if (message != null) {
                            Snackbar.make(root, message,
                                    Snackbar.LENGTH_LONG).setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
                        }
                    }
                    if (result != null) {
                        double amountClaimed = Helper.getJSONDouble("reward_amount", 0, result);

                        String defaultMessage = getContext() != null ?
                                getContext().getResources().getQuantityString(
                                        R.plurals.claim_reward_message,
                                        amountClaimed == 1 ? 1 : 2,
                                        new DecimalFormat(Helper.LBC_CURRENCY_FORMAT_PATTERN).format(amountClaimed)) : "";
                        String message = Helper.getJSONString("reward_notification", defaultMessage, result);

                        View view = getView();
                        if (view != null) {
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                        }
                        Helper.setViewEnabled(buttonClaim, true);
                        Helper.setViewEnabled(inputClaimCode, true);

                        fetchRewards();
                    }
                }
            });
        }
    }
}
