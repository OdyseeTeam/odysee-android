package com.odysee.app.tasks;

import android.os.AsyncTask;
import android.view.View;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.LivestreamReplay;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LivestreamReplaysTask extends AsyncTask<Void, Void, List<LivestreamReplay>> {
    private static final String ODYSEE_LIVESTREAM_REPLAYS_LIST_API = "https://api.odysee.live/replays/list?channel_claim_id=%s&signature=%s&signature_ts=%s&channel_name=%s";

    private final Claim channel;
    private final View progressView;
    private final String authToken;
    private final LivestreamReplaysResultHandler handler;
    private Exception error;

    public LivestreamReplaysTask(Claim channel, View progressView, String token, LivestreamReplaysResultHandler handler) {
        this.channel = channel;
        this.progressView = progressView;
        this.authToken = token;
        this.handler = handler;
    }

    @Override
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    @Override
    protected List<LivestreamReplay> doInBackground(Void... params) {
        try {
            // Sign channel data
            Map<String, Object> options = new HashMap<>(2);
            options.put("channel_id", channel.getClaimId());
            options.put("hexdata", Helper.toHexString(channel.getName()));

            JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(
                    "channel_sign", options, authToken);
            String signature = result.getString("signature");
            String signingTs = result.getString("signing_ts");

            // Fetch replays
            OkHttpClient client = new OkHttpClient();
            String url = String.format(ODYSEE_LIVESTREAM_REPLAYS_LIST_API,
                    channel.getClaimId(), signature, signingTs, channel.getName());
            Request request = new Request.Builder().url(url).build();

            List<LivestreamReplay> replays = null;

            try (Response response = client.newCall(request).execute()) {
                ResponseBody body = response.body();
                int responseCode = response.code();
                if (body != null && responseCode >= 200 && responseCode < 300) {
                    String responseString = body.string();
                    JSONObject responseJson = new JSONObject(responseString);
                    JSONArray data = responseJson.getJSONArray("data");
                    replays = new ArrayList<>(data.length());
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject replayJson = data.getJSONObject(i);
                        LivestreamReplay replay = LivestreamReplay.fromJSONObject(replayJson);

                        if (replay.getStatus().equals("inprogress") || replay.getStatus().equals("ready")) {
                            replays.add(replay);
                        }
                    }
                }
            }

            return replays;
        } catch (ApiCallException | JSONException | IOException ex) {
            error = ex;
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<LivestreamReplay> replays) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (replays != null) {
                handler.onSuccess(replays);
            } else {
                handler.onError(error);
            }
        }
    }
}