package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FetchStatCountTask extends AsyncTask<Void, Void, List<Integer>> {
    public static final int STAT_VIEW_COUNT = 1;
    public static final int STAT_SUB_COUNT = 2;

    private final List<String> claimIds;
    private final int stat;
    private final FetchStatCountHandler handler;
    private final View progressView;
    private Exception error;

    public FetchStatCountTask(int stat, String claimId, View progressView, FetchStatCountHandler handler) {
        this(stat, Collections.singletonList(claimId), progressView, handler);
    }
    public FetchStatCountTask(int stat, List<String> claimIds, View progressView, FetchStatCountHandler handler) {
        this.stat = stat;
        this.claimIds = claimIds;
        this.progressView = progressView;
        this.handler = handler;
    }

    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    protected List<Integer> doInBackground(Void... params) {
        List<Integer> counts = new ArrayList<>();
        try {
            if (stat != STAT_VIEW_COUNT && stat != STAT_SUB_COUNT) {
                throw new LbryioRequestException("Invalid stat count specified.");
            }

            JSONArray results = (JSONArray)
                    Lbryio.parseResponse(Lbryio.call(
                            stat == STAT_VIEW_COUNT ? "file" : "subscription",
                            stat == STAT_VIEW_COUNT ? "view_count" : "sub_count",
                            Lbryio.buildSingleListParam("claim_id", claimIds),
                            Helper.METHOD_GET, null));
            for (int i = 0; i < results.length(); i++) {
                counts.add(results.getInt(i));
            }
        } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException ex) {
            error = ex;
        }

        return counts;
    }

    protected void onPostExecute(List<Integer> counts) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (counts.size() > 0) {
                handler.onSuccess(counts);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface FetchStatCountHandler {
        void onSuccess(List<Integer> counts);
        void onError(Exception error);
    }
}
