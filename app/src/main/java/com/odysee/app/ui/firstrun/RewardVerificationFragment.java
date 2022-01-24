package com.odysee.app.ui.firstrun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.odysee.app.R;
import com.odysee.app.ui.rewards.RewardVerificationManualFragment;
import com.odysee.app.utils.FirstRunStepHandler;

import lombok.Setter;
import lombok.SneakyThrows;

public class RewardVerificationFragment extends Fragment {
    @Setter
    private FirstRunStepHandler firstRunStepHandler;

    private ViewPager2 optionsPager;
    private TabLayout optionsTabs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reward_verification, container, false);

        optionsPager = root.findViewById(R.id.reward_verification_options_view_pager);
        optionsPager.setSaveEnabled(false);
        optionsPager.setAdapter(new RewardVerificationPagerAdapter(this));

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

    private static class RewardVerificationPagerAdapter extends FragmentStateAdapter {
        public RewardVerificationPagerAdapter(Fragment fragment) {
            super(fragment);
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
                    return RewardVerificationManualFragment.class.newInstance();
                case 1:
                    return RewardVerificationManualFragment.class.newInstance();
                case 2:
                    return RewardVerificationManualFragment.class.newInstance();
                case 3:
                    return RewardVerificationManualFragment.class.newInstance();
            }
        }
    }
}
