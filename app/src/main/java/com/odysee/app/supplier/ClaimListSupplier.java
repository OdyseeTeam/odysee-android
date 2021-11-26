package com.odysee.app.supplier;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Lbry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClaimListSupplier implements Supplier<List<Claim>> {
    private final List<String> types;
    private final String authToken;

    public ClaimListSupplier(List<String> types, String authToken) {
        this.types = types;
        this.authToken = authToken;
    }

    @Override
    public List<Claim> get() {
        List<Claim> claims = null;

        try {
            Map<String, Object> options = new HashMap<>();
            if (types != null && types.size() > 0) {
                options.put("claim_type", types);
            }
            options.put("page", 1);
            options.put("page_size", 999);
            options.put("resolve", true);

            JSONObject result;
            if (authToken != null && !authToken.equals(""))
                result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_CLAIM_LIST, options, authToken);
            else
                result = (JSONObject) Lbry.genericApiCall(Lbry.METHOD_CLAIM_LIST, options);
            JSONArray items = result.getJSONArray("items");
            claims = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                claims.add(Claim.fromJSONObject(items.getJSONObject(i)));
            }
        } catch (ApiCallException | JSONException ex) {
            ex.printStackTrace();
        }
        return claims;
    }
}
