package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class PublishClaimTask extends AsyncTask<Void, Void, Claim> {
    private final Claim claim;
    private final View progressView;
    private final String authToken;
    private final ClaimResultHandler handler;
    private Exception error;

    public PublishClaimTask(Claim claim, View progressView, String authToken, ClaimResultHandler handler) {
        this.claim = claim;
        this.progressView = progressView;
        this.authToken = authToken;
        this.handler = handler;
    }

    @Override
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
        if (handler != null) {
            handler.beforeStart();
        }
    }

    @Override
    protected Claim doInBackground(Void... params) {
        Map<String, Object> options = Helper.buildPublishOptions(claim);
        Claim claimResult = null;

        try {
            JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_PUBLISH, options, authToken);
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
        } catch (ApiCallException | JSONException ex) {
            error = ex;
        }

        return claimResult;
    }

    @Override
    protected void onPostExecute(Claim result) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (result != null) {
                handler.onSuccess(result);
            } else {
                handler.onError(error);
            }
        }
    }
}
