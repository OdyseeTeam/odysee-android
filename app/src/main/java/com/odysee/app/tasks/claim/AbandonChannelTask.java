package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class AbandonChannelTask extends AsyncTask<Void, Void, Boolean> {
    private final List<String> claimIds;
    private List<String> successfulClaimIds;
    private List<String> failedClaimIds;
    private List<Exception> failedExceptions;
    private final View progressView;
    private final AbandonHandler handler;
    private String authToken;

    public AbandonChannelTask(List<String> claimIds, View progressView, String authToken, AbandonHandler handler) {
        this.claimIds = claimIds;
        this.progressView = progressView;
        this.authToken = authToken;
        this.handler = handler;
    }

    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    public Boolean doInBackground(Void... params) {
        successfulClaimIds = new ArrayList<>();
        failedClaimIds = new ArrayList<>();
        failedExceptions = new ArrayList<>();

        for (String claimId : claimIds) {
            try {
                Map<String, Object> options = new HashMap<>();
                options.put("claim_id", claimId);
                options.put("blocking", true);
                JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_CHANNEL_ABANDON, options, authToken);
                successfulClaimIds.add(claimId);
            } catch (ApiCallException ex) {
                failedClaimIds.add(claimId);
                failedExceptions.add(ex);
                ex.printStackTrace();
            }
        }

        return true;
    }

    protected void onPostExecute(Boolean result) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            handler.onComplete(successfulClaimIds, failedClaimIds, failedExceptions);
        }
    }
}
