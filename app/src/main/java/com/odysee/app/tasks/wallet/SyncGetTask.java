package com.odysee.app.tasks.wallet;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.exceptions.WalletException;
import com.odysee.app.model.WalletSync;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

public class SyncGetTask extends AsyncTask<Void, Void, WalletSync> {

    private final boolean applySyncChanges;
    private boolean applySyncSuccessful;
    private Exception error;
    private Exception syncApplyError;
    private String password;
    private final SyncTaskHandler handler;
    private final View progressView;

    private String syncHash;
    private String syncData;

    public SyncGetTask(String password, boolean applySyncChanges, View progressView, SyncTaskHandler handler) {
        this.password = password;
        this.progressView = progressView;
        this.applySyncChanges = applySyncChanges;
        this.handler = handler;
    }

    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }
    protected WalletSync doInBackground(Void... params) {
        try {
            password = Helper.isNullOrEmpty(password) ? "" : password;
            JSONObject result = (JSONObject) Lbry.genericApiCall(Lbry.METHOD_WALLET_STATUS, Lbryio.AUTH_TOKEN);
            boolean isLocked = Helper.getJSONBoolean("is_locked", false, result);
            boolean unlockSuccessful =
                    !isLocked || (boolean) Lbry.authenticatedGenericApiCall(
                            Lbry.METHOD_WALLET_UNLOCK, Lbry.buildSingleParam("password", password), Lbryio.AUTH_TOKEN);
            if (!unlockSuccessful) {
                throw new WalletException("The wallet could not be unlocked with the provided password.");
            }

            String hash = (String) Lbry.authenticatedGenericApiCall(Lbry.METHOD_SYNC_HASH, null, Lbryio.AUTH_TOKEN);
            try {
                JSONObject response = (JSONObject) Lbryio.parseResponse(
                        Lbryio.call("sync", "get", Lbryio.buildSingleParam("hash", hash), Helper.METHOD_POST, null));
                WalletSync walletSync = new WalletSync(
                        Helper.getJSONString("hash", null, response),
                        Helper.getJSONString("data", null, response),
                        Helper.getJSONBoolean("changed", false, response)
                );

                if (applySyncChanges && (!hash.equalsIgnoreCase(walletSync.getHash()) || walletSync.isChanged())) {
                    //Lbry.sync_apply({ password, data: response.data, blocking: true });
                    try {
                        Map<String, Object> options = new HashMap<>();
                        options.put("password", Helper.isNullOrEmpty(password) ? "" : password);
                        options.put("data", walletSync.getData());
                        options.put("blocking", true);

                        JSONObject syncApplyResponse = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_SYNC_APPLY, options, Lbryio.AUTH_TOKEN);
                        syncHash = Helper.getJSONString("hash", null, syncApplyResponse);
                        syncData = Helper.getJSONString("data", null, syncApplyResponse);
                        applySyncSuccessful = true;
                    } catch (ApiCallException | ClassCastException ex) {
                        // sync_apply failed
                        syncApplyError = ex;
                    }
                }

                if (Lbryio.isSignedIn() && !Lbryio.userHasSyncedWallet) {
                    // indicate that the user owns a synced wallet (only if the user is signed in)
                    Lbryio.userHasSyncedWallet = true;
                }

                return walletSync;
            } catch (LbryioResponseException ex) {
                // wallet sync data doesn't exist
                return null;
            }
        } catch (ApiCallException | WalletException | ClassCastException | LbryioRequestException ex) {
            error = ex;
            return null;
        }
    }
    protected void onPostExecute(WalletSync result) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (result != null) {
                handler.onSyncGetSuccess(result);
            } else if (error != null) {
                handler.onSyncGetError(error);
            } else {
                handler.onSyncGetWalletNotFound();
            }

            if (applySyncChanges) {
                if (applySyncSuccessful) {
                    handler.onSyncApplySuccess(syncHash, syncData);
                } else if (syncApplyError != null) {
                    handler.onSyncApplyError(syncApplyError);
                }
            }
        }
    }
}
