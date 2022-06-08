package com.odysee.app.tasks.claim;

import com.odysee.app.model.Claim;

import java.util.List;

public interface ResolveResultHandler {
    void onSuccess(List<Claim> claims);
    void onError(Exception error);
}
