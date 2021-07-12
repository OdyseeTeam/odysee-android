package com.odysee.app.callable;

import android.accounts.AccountManager;
import android.content.Context;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Lbry;

import java.util.concurrent.Callable;

public class WalletGetUnusedAddress implements Callable<String> {
    Context ctx;

    public WalletGetUnusedAddress(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public String call() throws Exception {
        String address = null;
        try {
            AccountManager am = AccountManager.get(ctx);
            address = (String) Lbry.directApiCall(Lbry.METHOD_ADDRESS_UNUSED, am.peekAuthToken(am.getAccounts()[0], "auth_token_type"));
        } catch (ApiCallException | ClassCastException ex) {
            ex.printStackTrace();
        }

        return address;
    }
}
