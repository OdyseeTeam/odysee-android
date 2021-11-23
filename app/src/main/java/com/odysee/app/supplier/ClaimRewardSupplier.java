package com.odysee.app.supplier;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.SneakyThrows;

public class ClaimRewardSupplier implements Supplier<JSONObject> {
    private final String type;
    private final String rewardCode;
    private final String token;

    public ClaimRewardSupplier(String type, String rewardCode, String token) {
        this.type = type;
        this.rewardCode = rewardCode;
        this.token = token;
    }

    @SneakyThrows
    @Override
    public JSONObject get() {
        return claimReward();
    }

    private JSONObject claimReward() throws ApiCallException, LbryioResponseException {
        try {
            String txid = null;
            if (Reward.TYPE_FIRST_CHANNEL.equalsIgnoreCase(type)) {
                // fetch a channel
                txid = fetchSingleClaimTxid(Claim.TYPE_CHANNEL);
            } else if (Reward.TYPE_FIRST_PUBLISH.equalsIgnoreCase(type)) {
                // fetch a publish
                txid = fetchSingleClaimTxid(Claim.TYPE_STREAM);
            }

            Map<String, String> options = new HashMap<>();
            options.put("reward_type", type);
            options.put("wallet_address", (String) Lbry.genericApiCall(Lbry.METHOD_ADDRESS_UNUSED, token));

            if (!Helper.isNullOrEmpty(rewardCode)) {
                options.put("code", rewardCode);
            }
            if (!Helper.isNullOrEmpty(txid)) {
                options.put("transaction_id", txid);
            }
            if (token != null) {
                options.put("auth_token", token);
            }

            return (JSONObject) Lbryio.parseResponse(Lbryio.call("reward", "claim", options, Helper.METHOD_POST, null));
        } catch (JSONException | LbryioRequestException ex) {
            ex.printStackTrace();
            return null;
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
}
