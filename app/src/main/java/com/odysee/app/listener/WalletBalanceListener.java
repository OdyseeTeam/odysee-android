package com.odysee.app.listener;

import com.odysee.app.model.WalletBalance;

public interface WalletBalanceListener {
    void onWalletBalanceUpdated(WalletBalance walletBalance);
}
