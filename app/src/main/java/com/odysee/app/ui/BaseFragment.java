package com.odysee.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import lombok.Getter;
import lombok.Setter;

public class BaseFragment extends Fragment {
    @Getter
    @Setter
    private Map<String, Object> params;
    private boolean rewardDriverClickListenerSet;

    public boolean shouldHideGlobalPlayer() {
        return false;
    }

    public boolean shouldSuspendGlobalPlayer() {
        return false;
    }

    public void onStart() {
        super.onStart();
        if (shouldSuspendGlobalPlayer()) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                MainActivity.suspendGlobalPlayer(context);
            }
        }
    }

    public void onStop() {
        if (shouldSuspendGlobalPlayer()) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                MainActivity.resumeGlobalPlayer(context);
            }
        }

        if (params != null && params.containsKey("source") && "notification".equalsIgnoreCase(params.get("source").toString())) {
            Context context = getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).navigateBackToNotifications();
            }
        }

        rewardDriverClickListenerSet = false;
        super.onStop();
    }

    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;

            if (shouldHideGlobalPlayer()) {
                activity.hideGlobalNowPlaying();
            } else {
                activity.checkNowPlaying();
            }
        }
    }

    public void checkRewardsDriverCard(String rewardDriverText, double minCost) {
        View root = getView();
        if (root != null) {
            View rewardDriverCard = root.findViewById(R.id.reward_driver_card);
            if (rewardDriverCard != null) {
                if (!rewardDriverClickListenerSet) {
                    rewardDriverCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Context context = getContext();
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).openRewards(null);
                            }
                        }
                    });
                    rewardDriverClickListenerSet = true;
                }

                // only apply to fragments that have the card present
                ((TextView) rewardDriverCard.findViewById(R.id.reward_driver_text)).setText(rewardDriverText);
                boolean showRewardsDriver = Lbry.walletBalance == null ||
                        minCost == 0 && Lbry.walletBalance.getAvailable().doubleValue() == 0 |
                        Lbry.walletBalance.getAvailable().doubleValue() < Math.max(minCost, Helper.MIN_DEPOSIT);
                rewardDriverCard.setVisibility(showRewardsDriver ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void showError(String message) {
        Context context = getContext();
        View v = getView();
        if (context != null && v != null) {
            Snackbar.make(v, message, Snackbar.LENGTH_LONG).setBackgroundTint(Color.RED).setTextColor(Color.WHITE).show();
        }
    }

    public void showMessage(int stringResourceId) {
        Context c = getContext();

        if (c != null) {
            showMessage(c.getResources().getString(stringResourceId));
        }
    }

    public void showMessage(String message) {
        Context context = getContext();
        View rootView = getView();
        if (context != null && rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
