package com.odysee.app.tasks.claim;

import com.odysee.app.model.Claim;

public interface ClaimResultHandler {
    void beforeStart();
    void onSuccess(Claim claimResult);
    void onError(Exception error);
}
