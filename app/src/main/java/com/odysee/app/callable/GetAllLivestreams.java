package com.odysee.app.callable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class GetAllLivestreams implements Callable<Map<String, JSONObject>> {
    @Override
    public Map<String, JSONObject> call() throws Exception {
        final String ODYSEE_LIVESTREAM_ALL_API = "https://api.odysee.live/livestream/all";
        Map<String, JSONObject> streamingChannels = new HashMap<>();
        Request.Builder builder = new Request.Builder();
        OkHttpClient client = new OkHttpClient.Builder().build();
        builder.url(ODYSEE_LIVESTREAM_ALL_API);
        Request request = builder.build();

        JSONArray jsonData;

        try (Response resp = client.newCall(request).execute()) {
            ResponseBody body = resp.body();
            int responseCode = resp.code();

            if (body != null && responseCode >= 200 && responseCode < 300) {
                String responseString = body.string();

                resp.close();

                JSONObject json = new JSONObject(responseString);
                if (!json.isNull("data") && (json.has("success") && json.getBoolean("success"))) {
                    jsonData = json.getJSONArray("data");

                    int s = jsonData.length();

                    for (int i = 0; i < s; i++) {
                        JSONObject channelData = jsonData.getJSONObject(i);

                        // On the livestreams/all API call, all returned channels are expected to be live,
                        // but let's check it, just in case.
                        if (channelData.has("ChannelClaimID") && channelData.has("Live") && channelData.getBoolean("Live")) {
                            streamingChannels.put(channelData.getString("ChannelClaimID"), channelData);
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }

        return streamingChannels;
    }
}
