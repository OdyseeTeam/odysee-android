package com.odysee.app.callable;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

public class ChannelCreateUpdate implements Callable<Claim> {
    private final Claim claim;
    private final BigDecimal deposit;
    private final boolean update;
    private final String authToken;

    public ChannelCreateUpdate(Claim claim, BigDecimal deposit, boolean update, String authToken) {
        this.claim = claim;
        this.deposit = deposit;
        this.update = update;
        this.authToken = authToken;
    }

    @Override
    public Claim call() throws ApiCallException {
        Map<String, Object> options = new HashMap<>();
        if (!update) {
            options.put("name", claim.getName());
        } else {
            options.put("claim_id", claim.getClaimId());
        }
        options.put("bid", new DecimalFormat(Helper.SDK_AMOUNT_FORMAT, new DecimalFormatSymbols(Locale.US)).format(deposit.doubleValue()));
        if (!Helper.isNullOrEmpty(claim.getTitle())) {
            options.put("title", claim.getTitle());
        }
        if (!Helper.isNullOrEmpty(claim.getCoverUrl())) {
            options.put("cover_url", claim.getCoverUrl());
        }
        if (!Helper.isNullOrEmpty(claim.getThumbnailUrl())) {
            options.put("thumbnail_url", claim.getThumbnailUrl());
        }
        if (!Helper.isNullOrEmpty(claim.getDescription())) {
            options.put("description", claim.getDescription());
        }
        if (!Helper.isNullOrEmpty(claim.getWebsiteUrl())) {
            options.put("website_url", claim.getWebsiteUrl());
        }
        if (!Helper.isNullOrEmpty(claim.getEmail())) {
            options.put("email", claim.getEmail());
        }
        if (claim.getTags() != null && claim.getTags().size() > 0) {
            options.put("tags", claim.getTags());
        }
        options.put("blocking", true);

        Claim claimResult = null;
        String method = !update ? Lbry.METHOD_CHANNEL_CREATE : Lbry.METHOD_CHANNEL_UPDATE;
        try {
            JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(method, options, authToken);
            if (result.has("outputs")) {
                JSONArray outputs = result.getJSONArray("outputs");
                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject output = outputs.getJSONObject(i);
                    if (output.has("claim_id") && output.has("claim_op")) {
                        claimResult = Claim.claimFromOutput(output);
                        break;
                    }
                }
            }
        } catch (ClassCastException | JSONException ex) {
            ex.printStackTrace();
        }

        return claimResult;
    }
}

