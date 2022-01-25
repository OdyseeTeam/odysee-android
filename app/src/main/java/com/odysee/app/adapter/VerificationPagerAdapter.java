package com.odysee.app.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.odysee.app.listener.VerificationListener;
import com.odysee.app.listener.WalletSyncListener;
import com.odysee.app.ui.verification.EmailVerificationFragment;
import com.odysee.app.ui.verification.ManualVerificationFragment;
import com.odysee.app.ui.rewards.RewardVerificationPhoneFragment;
import com.odysee.app.ui.verification.WalletVerificationFragment;
import lombok.SneakyThrows;

/**
 * 4 fragments
 * - Email collect / verify (sign in)
 * - Phone number collect / verify (rewards)
 * - Wallet password
 * - Manual verification page
 */
public class VerificationPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_VERIFICATION_EMAIL = 0;
    public static final int PAGE_VERIFICATION_PHONE = 1;
    public static final int PAGE_VERIFICATION_WALLET = 2;
    public static final int PAGE_VERIFICATION_MANUAL = 3;

    private final FragmentActivity activity;

    public VerificationPagerAdapter(FragmentActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @SneakyThrows
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
            default:
                EmailVerificationFragment evFragment = EmailVerificationFragment.class.newInstance();
                if (activity instanceof VerificationListener) {
                    evFragment.setListener((VerificationListener) activity);
                }
                return evFragment;
            case 1:
                RewardVerificationPhoneFragment pvFragment = RewardVerificationPhoneFragment.class.newInstance();
                if (activity instanceof VerificationListener) {
                    pvFragment.setListener((VerificationListener) activity);
                }
                return pvFragment;
            case 2:
                WalletVerificationFragment wvFragment = WalletVerificationFragment.class.newInstance();
                if (activity instanceof WalletSyncListener) {
                    wvFragment.setListener((WalletSyncListener) activity);
                }
                return wvFragment;
            case 3:
                ManualVerificationFragment mvFragment = ManualVerificationFragment.class.newInstance();
                if (activity instanceof VerificationListener) {
                    mvFragment.setListener((VerificationListener) activity);
                }
                return mvFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
