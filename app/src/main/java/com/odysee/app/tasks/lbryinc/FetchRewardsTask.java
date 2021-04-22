package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.Reward;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

public class FetchRewardsTask extends AsyncTask<Void, Void, List<Reward>> {
    private final FetchRewardsHandler handler;
    private final View progressView;
    private Exception error;

    public FetchRewardsTask(View progressView, FetchRewardsHandler handler) {
        this.progressView = progressView;
        this.handler = handler;
    }

    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    protected List<Reward> doInBackground(Void... params) {
        List<Reward> rewards = null;
        try {
            Map<String, String> options = new HashMap<>();
            options.put("multiple_rewards_per_type", "true");
            JSONArray results = (JSONArray) Lbryio.parseResponse(Lbryio.call("reward", "list", options, null));
            rewards = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                rewards.add(Reward.fromJSONObject(results.getJSONObject(i)));
            }
        } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException ex) {
            error = ex;
        }

        return rewards;
    }

    protected void onPostExecute(List<Reward> rewards) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (rewards != null) {
                handler.onSuccess(rewards);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface FetchRewardsHandler {
        void onSuccess(List<Reward> rewards);
        void onError(Exception error);
    }
}
