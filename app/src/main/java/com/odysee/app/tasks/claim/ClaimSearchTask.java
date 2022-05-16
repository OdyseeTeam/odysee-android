package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.Page;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class ClaimSearchTask extends AsyncTask<Void, Void, Page> {
    private final Map<String, Object> options;
    private final String connectionString;
    private final ClaimSearchResultHandler handler;
    private final View progressView;
    private ApiCallException error;

    public ClaimSearchTask(Map<String, Object> options, String connectionString, View progressView, ClaimSearchResultHandler handler) {
        this.options = options;
        this.connectionString = connectionString;
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
            return Lbry.claimSearch(options, connectionString);
        } catch (ApiCallException ex) {
            error = ex;
            return null;
        }
    }
    @Override
    protected void onPostExecute(Page claimsPage) {
        Helper.setViewVisibility(progressView, View.INVISIBLE);
        if (handler != null) {
            if (claimsPage != null) {
                handler.onSuccess(Helper.filterInvalidReposts(claimsPage.getClaims()), claimsPage.isLastPage());
            } else {
                handler.onError(error);
            }
        }
    }
}
