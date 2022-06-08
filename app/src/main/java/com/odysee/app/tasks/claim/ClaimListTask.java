package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Page;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.Lbryio;

public class ClaimListTask extends AsyncTask<Void, Void, Page> {
    private final Map<String, Object> options;
    private final View progressView;
    private final ClaimListResultHandler handler;
    private Exception error;
    private String authToken;

    public ClaimListTask(Map<String, Object> options, String token, View progressView, ClaimListResultHandler handler) {
        this(options, progressView, handler);
        this.authToken = token;
    }
    public ClaimListTask(Map<String, Object> options, View progressView, ClaimListResultHandler handler) {
        this.options = options;
        this.progressView = progressView;
        this.handler = handler;
    }

    @Override
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    @Override
    protected Page doInBackground(Void... params) {
        try {
            return Lbry.claimList(options, authToken);
        } catch (ApiCallException ex) {
            error = ex;
            return null;
        }
    }

    @Override
    protected void onPostExecute(Page claimsPage) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (claimsPage != null) {
                // TODO: Add fix for handling invalid reposts in ClaimListAdapter
                handler.onSuccess(Helper.filterInvalidReposts(claimsPage.getClaims()), claimsPage.isLastPage());
            } else {
                handler.onError(error);
            }
        }
    }
}
