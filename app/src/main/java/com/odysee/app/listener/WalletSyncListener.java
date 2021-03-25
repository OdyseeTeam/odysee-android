package com.odysee.app.listener;

public interface WalletSyncListener {
    void onWalletSyncProcessing();
    void onWalletSyncWaitingForInput();
    void onWalletSyncEnabled();
    void onWalletSyncFailed(Exception error);
}
