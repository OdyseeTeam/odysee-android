package com.odysee.app.callable;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.utils.Lbry;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class WalletBalanceFetch implements Callable<WalletBalance> {
    private String authToken;

    public WalletBalanceFetch(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public WalletBalance call() throws Exception {
        WalletBalance balance;
        try {
            JSONObject json = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_WALLET_BALANCE, null, authToken);
            balance = WalletBalance.fromJSONObject(json);
        } catch (ApiCallException | ClassCastException ex) {
            ex.printStackTrace();
            return null;
        }

        return balance;

    }
}
