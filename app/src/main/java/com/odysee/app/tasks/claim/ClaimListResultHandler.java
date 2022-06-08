package com.odysee.app.tasks.claim;

import java.util.List;

import com.odysee.app.model.Claim;

public interface ClaimListResultHandler {
    void onSuccess(List<Claim> claims, boolean hasReachedEnd);
    void onError(Exception error);
}
