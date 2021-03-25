package com.odysee.app.tasks.claim;

import java.util.List;

public interface AbandonHandler {
    void onComplete(List<String> successfulClaimIds, List<String> failedClaimIds, List<Exception> errors);
}
