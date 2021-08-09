package com.odysee.app.tasks.wallet;

import android.os.AsyncTask;

import org.json.JSONObject;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.WalletBalance;
import com.odysee.app.utils.Lbry;

public class WalletBalanceTask extends AsyncTask<Void, Void, WalletBalance> {
    private final String authToken;
    private final WalletBalanceHandler handler;
    private Exception error;

    public WalletBalanceTask(String authToken, WalletBalanceHandler handler) {
        this.authToken = authToken;
        this.handler = handler;
    }

    protected WalletBalance doInBackground(Void... params) {
        WalletBalance balance;
        try {
            JSONObject json = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_WALLET_BALANCE, null, authToken);
            balance = WalletBalance.fromJSONObject(json);
        } catch (ApiCallException | ClassCastException ex) {
            error = ex;
            return null;
        }

        return balance;
    }

    protected void onPostExecute(WalletBalance walletBalance) {
        if (handler != null) {
            if (walletBalance != null) {
                handler.onSuccess(walletBalance);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface WalletBalanceHandler {
        void onSuccess(WalletBalance walletBalance);
        void onError(Exception error);
    }
}
