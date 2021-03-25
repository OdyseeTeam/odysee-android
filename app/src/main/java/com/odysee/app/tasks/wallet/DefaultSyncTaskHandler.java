package com.odysee.app.tasks.wallet;

import com.odysee.app.model.WalletSync;

public abstract class DefaultSyncTaskHandler implements SyncTaskHandler {
    public void onSyncGetSuccess(WalletSync walletSync) {
        throw new UnsupportedOperationException();
    }
    public void onSyncGetWalletNotFound() {
        throw new UnsupportedOperationException();
    }
    public void onSyncGetError(Exception error) {
        throw new UnsupportedOperationException();
    }
    public void onSyncSetSuccess(String hash) {
        throw new UnsupportedOperationException();
    }
    public void onSyncSetError(Exception error) {
        throw new UnsupportedOperationException();
    }
    public void onSyncApplySuccess(String hash, String data) {
        throw new UnsupportedOperationException();
    }
    public void onSyncApplyError(Exception error) {
        throw new UnsupportedOperationException();
    }
}
