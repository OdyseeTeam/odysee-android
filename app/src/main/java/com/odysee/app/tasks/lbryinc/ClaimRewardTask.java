package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

public class ClaimRewardTask extends AsyncTask<Void, Void, String> {

    private final String type;
    private final String rewardCode;
    private double amountClaimed;
    private final String authToken;
    private final ClaimRewardHandler handler;
    private Exception error;

    public ClaimRewardTask(String type, String rewardCode, String authToken, ClaimRewardHandler handler) {
        this.type = type;
        this.rewardCode = rewardCode;
        this.authToken = authToken;
        this.handler = handler;
    }

    protected void onPreExecute() {
    }

    public String doInBackground(Void... params) {
        String message = null;
        try {
            String txid = null;
            if (Reward.TYPE_FIRST_CHANNEL.equalsIgnoreCase(type)) {
                // fetch a channel
                txid = fetchSingleClaimTxid(Claim.TYPE_CHANNEL);
            } else if (Reward.TYPE_FIRST_PUBLISH.equalsIgnoreCase(type)) {
                // fetch a publish
                txid = fetchSingleClaimTxid(Claim.TYPE_STREAM);
            }

            // Get a new wallet address for the reward
            String address = (String) Lbry.genericApiCall(Lbry.METHOD_ADDRESS_UNUSED, authToken);
            Map<String, String> options = new HashMap<>();
            options.put("reward_type", type);
            options.put("wallet_address", address);
            if (!Helper.isNullOrEmpty(rewardCode)) {
                options.put("code", rewardCode);
            }
            if (!Helper.isNullOrEmpty(txid)) {
                options.put("transaction_id", txid);
            }
            if (authToken != null) {
                options.put("auth_token", authToken);
            }

            JSONObject reward = (JSONObject) Lbryio.parseResponse(
                    Lbryio.call("reward", "claim", options, Helper.METHOD_POST, null));
            if (reward != null) {
                amountClaimed = Helper.getJSONDouble("reward_amount", 0, reward);
                message = Helper.getJSONString("reward_notification", "", reward);
            }
        } catch (ApiCallException | JSONException | LbryioRequestException | LbryioResponseException ex) {
            error = ex;
        }

        return message;
    }

    protected void onPostExecute(String message) {
        if (handler != null) {
            if (message != null) {
                handler.onSuccess(amountClaimed, message);
            } else {
                handler.onError(error);
            }
        }
    }

    private String fetchSingleClaimTxid(String claimType) throws ApiCallException, JSONException {
        Map<String, Object> options = new HashMap<>();
        options.put("claim_type", claimType);
        options.put("page", 1);
        options.put("page_size", 1);
        options.put("resolve", true);

        JSONObject result = (JSONObject) Lbry.genericApiCall(Lbry.METHOD_CLAIM_LIST, options);
        JSONArray items = result.getJSONArray("items");
        if (items.length() > 0) {
            Claim claim = Claim.fromJSONObject(items.getJSONObject(0));
            return claim.getTxid();
        }

        return null;
    }

    public interface ClaimRewardHandler {
        void onSuccess(double amountClaimed, String message);
        void onError(Exception error);
    }
}
