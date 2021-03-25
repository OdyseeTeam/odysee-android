package com.odysee.app.tasks.wallet;

import com.odysee.app.model.WalletSync;

public interface SyncTaskHandler {
    void onSyncGetSuccess(WalletSync walletSync);
    void onSyncGetWalletNotFound();
    void onSyncGetError(Exception error);
    void onSyncSetSuccess(String hash);
    void onSyncSetError(Exception error);
    void onSyncApplySuccess(String hash, String data);
    void onSyncApplyError(Exception error);
}
