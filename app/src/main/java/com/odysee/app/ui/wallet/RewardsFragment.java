package com.odysee.app.ui.wallet;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.odysee.app.MainActivity;
import com.odysee.app.OdyseeApp;
import com.odysee.app.R;
import com.odysee.app.adapter.RewardListAdapter;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.model.lbryinc.User;
import com.odysee.app.supplier.ClaimRewardSupplier;
import com.odysee.app.supplier.FetchRewardsSupplier;
import com.odysee.app.tasks.lbryinc.FetchCurrentUserTask;
import com.odysee.app.ui.BaseFragment;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

import org.json.JSONObject;

public class RewardsFragment extends BaseFragment implements RewardListAdapter.RewardClickListener {

    private boolean rewardClaimInProgress;

    private ProgressBar rewardsLoading;
    private RewardListAdapter adapter;
    private RecyclerView rewardList;
    private TextView linkFilterUnclaimed;
    private TextView linkFilterAll;
    private boolean isRewardsApproved;
    private boolean hasFinishedRewardsApprovedCheck;
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

    private void checkRewardsApproved() {
        View v = getView();
        Helper.setViewVisibility(v, View.INVISIBLE);

        FetchCurrentUserTask task = new FetchCurrentUserTask(getContext(), new FetchCurrentUserTask.FetchUserTaskHandler() {
            @Override
            public void onSuccess(User user) {
                Lbryio.currentUser = user;
                boolean isApproved = (user.isIdentityVerified() && user.isRewardApproved());
                isRewardsApproved = isApproved;
                onFinishRewardsApprovedCheck(isApproved);
            }

            @Override
            public void onError(Exception error) {
                // if an error occurred, show rewards approval process anyway
                onFinishRewardsApprovedCheck(false);
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onFinishRewardsApprovedCheck(boolean isApproved) {
        hasFinishedRewardsApprovedCheck = true;

        if (isApproved) {
            View v = getView();
            Helper.setViewVisibility(v, View.VISIBLE);
            return;
        }

        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.showRewardsVerification();
        }
    }

    public void onResume() {
        super.onResume();

        Context context = getContext();
        if (hasFinishedRewardsApprovedCheck && !isRewardsApproved) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).navigateBackToMain();
            }
            return;
        }

        if (!hasFinishedRewardsApprovedCheck) {
            checkRewardsApproved();
        } else {
            fetchRewards();
        }

        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            LbryAnalytics.setCurrentScreen(activity, "Rewards", "Rewards");
            activity.updateMiniPlayerMargins(false);
            activity.updateCurrentDisplayFragment(this);
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

    public void onPause() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.updateMiniPlayerMargins(true);
        }
        super.onPause();
    }

    public void onStop() {
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            activity.resetCurrentDisplayFragment();
        }
        super.onStop();
    }

    private void fetchRewards() {
        Helper.setViewVisibility(rewardList, View.INVISIBLE);
        rewardsLoading.setVisibility(View.VISIBLE);

        Activity activity = getActivity();

        if (activity != null) {
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
            ExecutorService executorService = ((OdyseeApp) activity.getApplication()).getExecutor();

            Supplier<List<Reward>> supplier = new FetchRewardsSupplier(options);
            CompletableFuture<List<Reward>> cf = CompletableFuture.supplyAsync(supplier, executorService);
            cf.exceptionally(e -> {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showError(e.getMessage());
                    }
                });
                return null;
            }).thenAccept(rewards -> {
                Lbryio.updateRewardsLists(rewards);

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
            });
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

        Supplier<JSONObject> s = new ClaimRewardSupplier(type, code, authToken);
        CompletableFuture<JSONObject> cf = CompletableFuture.supplyAsync(s);
        cf.whenComplete((result, e) -> afterClaimingReward(inputClaimCode, buttonClaim, loadingView, activity, result, e));
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
                            showError(message);
                        }
                    }
                    if (result != null) {
                        double amountClaimed = Helper.getJSONDouble("reward_amount", 0, result);

                        String defaultMessage = getContext() != null ?
                                getContext().getResources().getQuantityString(
                                        R.plurals.claim_reward_message,
                                        amountClaimed == 1 ? 1 : 2,
                                        new DecimalFormat(Helper.LBC_CURRENCY_FORMAT_PATTERN).format(amountClaimed)) : "";

                        showMessage(Helper.getJSONString("reward_notification", defaultMessage, result));
                        Helper.setViewEnabled(buttonClaim, true);
                        Helper.setViewEnabled(inputClaimCode, true);

                        fetchRewards();
                    }
                }
            });
        }
    }
}
