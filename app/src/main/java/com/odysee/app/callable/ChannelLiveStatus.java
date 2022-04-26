package com.odysee.app.callable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A callable which makes a request to Odysee Live API and, if response includes channel claim ID,
 * it will return the "data" key.
 *
 * If alwaysReturnData is set to true, then this will return the data field, no matter if channel is not currently livestreaming.
 */
public class ChannelLiveStatus implements Callable<Map<String, JSONObject>> {
    private static final String ODYSEE_LIVESTREAM_CHANNEL_LIVE_STATUS_API = "https://api.odysee.live/livestream/is_live?channel_claim_id=";
    private final List<String> channelIds;
    private boolean alwaysReturnData = false;

    public ChannelLiveStatus(List<String> channelIds) {
        this.channelIds = channelIds;
    }

    /**
     *
     * @param channelIds - list of channels to be queried
     * @param alwaysReturnData - set to true if you are interested on the data, even if channel is not live right now
     */
    public ChannelLiveStatus(List<String> channelIds, boolean alwaysReturnData) {
        this(channelIds);
        this.alwaysReturnData = alwaysReturnData;
    }

    /**
     * Returns the "data" key. Calling code should check for the live status if the ChannelLiveStatus(List<String>, boolean) constructor
     * was used to create the object.
     *
     * @return A Java Map object with the "data" key for each channel claim ID stored as a value for its key
     */
    @Override
    public Map<String, JSONObject> call() throws Exception {
        Map<String, JSONObject> streamingChannels = new HashMap<>();
        Request.Builder builder = new Request.Builder();
        OkHttpClient client = new OkHttpClient.Builder().build();
        for (String channelId: channelIds) {
            String url = ODYSEE_LIVESTREAM_CHANNEL_LIVE_STATUS_API.concat(channelId);
//            Log.i("OdyseeLiveStatus", "get: getting channel live status from: " + url);
            builder.url(url);
            Request request = builder.build();

            try (Response resp = client.newCall(request).execute()) {
                ResponseBody body = resp.body();
                int responseCode = resp.code();
                if (body != null) {
                    String responseString = body.string();
                    if (responseCode >= 200 && responseCode < 300) {
                        JSONObject json = new JSONObject(responseString);
                        if (!json.isNull("data") && (json.has("success") && json.getBoolean("success"))) {
                            JSONObject jsonData = (JSONObject) json.get("data");
                            if (jsonData.has("ChannelClaimID") && jsonData.getString("ChannelClaimID").equals(channelId)) {
                                if (alwaysReturnData || (jsonData.has("Live") && jsonData.getBoolean("Live"))) {
                                    streamingChannels.put(channelId, jsonData);
                                }
                            }
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

        }
        return streamingChannels;
    }
}
