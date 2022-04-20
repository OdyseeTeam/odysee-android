package com.odysee.app.utils;

import android.app.Activity;
import android.content.Context;

import com.odysee.app.tasks.RewardVerifiedHandler;

public class VerificationSkipQueue {
    public VerificationSkipQueue(Context context, ShowInProgressListener listener, RewardVerifiedHandler handler) { }

    public void createBillingClientAndEstablishConnection() { }

    public void onSkipQueueAction(Activity activity) { }

    public interface ShowInProgressListener {
        void maybeShowRequestInProgress();
    }
}
