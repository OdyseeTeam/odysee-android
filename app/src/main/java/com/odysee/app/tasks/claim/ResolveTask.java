package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class ResolveTask extends AsyncTask<Void, Void, List<Claim>> {
    private final List<String> urls;
    private final String connectionString;
    private final ResolveResultHandler handler;
    private final View progressView;
    private ApiCallException error;

    public ResolveTask(String url, String connectionString, View progressView, ResolveResultHandler handler) {
        this(Arrays.asList(url), connectionString, progressView, handler);
    }

    public ResolveTask(List<String> urls, String connectionString, View progressView, ResolveResultHandler handler) {
        this.urls = urls;
        this.connectionString = connectionString;
        this.progressView = progressView;
        this.handler = handler;
    }
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }
    protected List<Claim> doInBackground(Void... params) {
        try {
            return Helper.filterInvalidReposts(Helper.filterInvalidClaims(Lbry.resolve(urls, connectionString)));
        } catch (ApiCallException ex) {
            error = ex;
            return null;
        }
    }
    protected void onPostExecute(List<Claim> claims) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (claims != null) {
                handler.onSuccess(claims);
            } else {
                handler.onError(error);
            }
        }
    }

}
