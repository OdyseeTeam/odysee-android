package com.odysee.app.ui.rewards;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.odysee.app.FirstRunActivity;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.listener.VerificationListener;
import com.odysee.app.model.lbryinc.User;
import com.odysee.app.tasks.lbryinc.FetchCurrentUserTask;
import com.odysee.app.ui.rewards.RewardVerificationManualFragment;
import com.odysee.app.ui.rewards.RewardVerificationPaidFragment;
import com.odysee.app.ui.rewards.RewardVerificationPhoneFragment;
import com.odysee.app.ui.rewards.RewardVerificationTwitterFragment;
import com.odysee.app.utils.FirstRunStepHandler;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryAnalytics;
import com.odysee.app.utils.Lbryio;

import lombok.Setter;
import lombok.SneakyThrows;

public class RewardVerificationFragment extends Fragment implements VerificationListener {
    @Setter
    private FirstRunStepHandler firstRunStepHandler;

    private View brandContainer;
    private TextView textSummary;
    private ViewPager2 optionsPager;
    private TabLayout optionsTabs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reward_verification, container, false);

        brandContainer = root.findViewById(R.id.reward_verification_brand_container);
        textSummary = root.findViewById(R.id.first_run_reward_verification_desc);
        optionsPager = root.findViewById(R.id.reward_verification_options_view_pager);
        optionsPager.setSaveEnabled(false);
        Context context = getContext();
        if (context instanceof FirstRunActivity) {
            optionsPager.setAdapter(new RewardVerificationPagerAdapter((FirstRunActivity) context, this));
            brandContainer.setVisibility(View.VISIBLE);
        } else if (context instanceof MainActivity) {
            brandContainer.setVisibility(View.GONE);
            optionsPager.setAdapter(new RewardVerificationPagerAdapter((MainActivity) context, this));
        }

        optionsTabs = root.findViewById(R.id.reward_verification_options_tabs);
        new TabLayoutMediator(optionsTabs, optionsPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0: tab.setText(getString(R.string.phone)); break;
                    case 1: tab.setText(getString(R.string.twitter)); break;
                    case 2: tab.setText(getString(R.string.paid)); break;
                    case 3: tab.setText(getString(R.string.manual)); break;
                }
            }
        }).attach();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkRewardApproved(true);

        Context context = getContext();
        if (context instanceof MainActivity && firstRunStepHandler == null) {
            // only set this as the current display fragment if we are not in first run mode
            ((MainActivity) context).updateCurrentDisplayFragment(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        if (context instanceof MainActivity && firstRunStepHandler == null) {
            // only set this as the current display fragment if we are not in first run mode
            ((MainActivity) context).updateCurrentDisplayFragment(null);
        }
    }

    @Override
    public void onEmailAdded(String email) {

    }

    @Override
    public void onEmailEdit() {

    }

    @Override
    public void onEmailVerified() {

    }

    @Override
    public void onPhoneAdded(String countryCode, String phoneNumber) {

    }

    @Override
    public void onPhoneVerified() {
        // phone verified stuff
        checkRewardApproved();
    }

    @Override
    public void onManualVerifyContinue() {

    }

    @Override
    public void onSkipQueueAction() {
        Context context = getContext();
        if (context instanceof VerificationListener) {
            // call this on FirstRunActivity
            ((VerificationListener) context).onSkipQueueAction();
        }
    }

    @Override
    public void onTwitterVerified() {
        checkRewardApproved();
    }

    @Override
    public void onManualProgress(boolean progress) {

    }

    private void checkRewardApproved() {
        checkRewardApproved(false);
    }

    private void checkRewardApproved(final boolean firstCheck) {
        if (firstRunStepHandler != null) {
            firstRunStepHandler.onRequestInProgress(true);
        }

        Helper.setViewVisibility(textSummary, View.INVISIBLE);
        optionsPager.setVisibility(View.INVISIBLE);
        optionsTabs.setVisibility(View.INVISIBLE);

        FetchCurrentUserTask task = new FetchCurrentUserTask(getContext(), new FetchCurrentUserTask.FetchUserTaskHandler() {
            @Override
            public void onSuccess(User user) {
                if (firstRunStepHandler != null) {
                    firstRunStepHandler.onRequestCompleted(FirstRunActivity.FIRST_RUN_STEP_REWARDS);
                }

                Lbryio.currentUser = user;
                if (user.isIdentityVerified() && user.isRewardApproved()) {
                    // verified for rewards
                    LbryAnalytics.logEvent(LbryAnalytics.EVENT_REWARD_ELIGIBILITY_COMPLETED);
                    textSummary.setText(R.string.reward_eligible);
                    Helper.setViewVisibility(textSummary, View.VISIBLE);
                    return;
                }

                if (user.isIdentityVerified()) {
                    textSummary.setMovementMethod(new LinkMovementMethod());
                    textSummary.setText(Html.fromHtml(getString(R.string.identify_verified_not_reward_eligible), Html.FROM_HTML_MODE_LEGACY));
                    Helper.setViewVisibility(textSummary, View.VISIBLE);
                    return;
                }

                if (!firstCheck) {
                    // show manual verification if the user is not yet reward approved
                    // and this is not the check when the page loads
                    optionsPager.setCurrentItem(3);
                }
                Helper.setViewVisibility(textSummary, View.VISIBLE);
                optionsPager.setVisibility(View.VISIBLE);
                optionsTabs.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception error) {
                showFetchUserError(error != null ? error.getMessage() : getString(R.string.fetch_current_user_error));
                optionsPager.setVisibility(View.VISIBLE);
                optionsTabs.setVisibility(View.VISIBLE);
                if (firstRunStepHandler != null) {
                    firstRunStepHandler.onRequestCompleted(FirstRunActivity.FIRST_RUN_STEP_REWARDS);
                }
            }
        });
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void showFetchUserError(String message) {
        Context context = getContext();
        if (context instanceof FirstRunActivity) {
            ((FirstRunActivity) context).showFetchUserError(message);
        }
    }

    private static class RewardVerificationPagerAdapter extends FragmentStateAdapter {
        private FragmentActivity activity;
        private Fragment parent;

        public RewardVerificationPagerAdapter(FragmentActivity activity, Fragment parent) {
            super(activity);
            this.activity = activity;
            this.parent = parent;
        }

        @Override
        public int getItemCount() {
            return 4;
        }

        @NonNull
        @Override
        @SneakyThrows
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                default:
                    RewardVerificationPhoneFragment phoneFragment = RewardVerificationPhoneFragment.class.newInstance();
                    if (parent instanceof VerificationListener) {
                        phoneFragment.setListener((VerificationListener) parent);
                    }
                    return phoneFragment;
                case 1:
                    RewardVerificationTwitterFragment twitterFragment = RewardVerificationTwitterFragment.class.newInstance();
                    if (parent instanceof VerificationListener) {
                        twitterFragment.setListener((VerificationListener) parent);
                    }
                    return twitterFragment;
                case 2:
                    RewardVerificationPaidFragment paidFragment = RewardVerificationPaidFragment.class.newInstance();
                    if (parent instanceof VerificationListener) {
                        paidFragment.setListener((VerificationListener) parent);
                    }
                    return paidFragment;
                case 3:
                    return RewardVerificationManualFragment.class.newInstance();
            }
        }
    }
}
