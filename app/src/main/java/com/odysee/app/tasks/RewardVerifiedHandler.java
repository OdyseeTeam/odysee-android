package com.odysee.app.tasks;

import com.odysee.app.model.lbryinc.RewardVerified;

public interface RewardVerifiedHandler {
    void onSuccess(RewardVerified rewardVerified);
    void onError(Exception error);
}
