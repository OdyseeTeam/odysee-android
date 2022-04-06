package com.odysee.app.callable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This class is used to perform a request to get the url for a channel's live stream
 * @deprecated Extract the url from the returned data for channel live status
 * @see ChannelLiveStatus
 */
@Deprecated
public class GetLivestreamUrl implements Callable<String> {
    private final String claimId;

    public GetLivestreamUrl(String claimId) {
        this.claimId = claimId;
    }

    @Override
    public String call() throws Exception {
        String urlLivestream = String.format("https://api.live.odysee.com/v1/odysee/live/%s", claimId);

        Request.Builder builder = new Request.Builder().url(urlLivestream);
        Request request = builder.build();

        OkHttpClient client = new OkHttpClient.Builder().build();

        try (Response resp = client.newCall(request).execute()) {
            ResponseBody body = resp.body();
            int responseCode = resp.code();
            if (body != null) {
                String responseString = body.string();
                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(responseString);
                    if (!json.isNull("data") && (json.has("success") && json.getBoolean("success"))) {
                        JSONObject jsonData = (JSONObject) json.get("data");
                        if (jsonData.has("live") && jsonData.getBoolean("live")) {
                            return jsonData.getString("url");
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
