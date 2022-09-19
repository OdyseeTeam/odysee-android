package com.odysee.app.callable;

import com.odysee.app.utils.Helper;
import org.json.JSONArray;
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
 */
public class ChannelLiveStatus implements Callable<Map<String, JSONObject>> {
    private static final String ODYSEE_LIVESTREAM_CHANNEL_LIVE_STATUS_API = "https://api.odysee.live/livestream/is_live?channel_claim_id=";
    private static final String ODYSEE_LIVESTREAM_SUBSCRIBED_LIVE_STATUS_API = "https://api.odysee.live/livestream/subscribed?channel_claim_ids=";
    private final List<String> channelIds;
    private boolean alwaysReturnData = false;
    private boolean subscribed = false;

    public ChannelLiveStatus(List<String> channelIds) {
        this.channelIds = channelIds;
    }

    /**
     * @param channelIds       list of channels to be queried
     * @param alwaysReturnData set to true if you are interested on the data, even if channel is not live right now
     */
    public ChannelLiveStatus(List<String> channelIds, boolean alwaysReturnData) {
        this(channelIds, alwaysReturnData, false);
    }

    /**
     *
     * @param channelIds list of channels to be queried
     * @param alwaysReturnData set to true if you are interested on the data, even if channel is not live right now
     * @param subscribed set to true if the request is for multiple channel ids, false for a single channel ID
     */
    public ChannelLiveStatus(List<String> channelIds, boolean alwaysReturnData, boolean subscribed) {
        this(channelIds);
        this.alwaysReturnData = alwaysReturnData;
        this.subscribed = subscribed;
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
        if (!subscribed) {
            for (String channelId: channelIds) {
                String url = ODYSEE_LIVESTREAM_CHANNEL_LIVE_STATUS_API.concat(channelId);
                JSONArray jsonDataArray = fetchLiveStatusData(url, builder, client);

                if (jsonDataArray != null && jsonDataArray.length() > 0) {
                    JSONObject json = jsonDataArray.getJSONObject(0);
                    if (json.has("ChannelClaimID") && json.getString("ChannelClaimID").equals(channelId)
                          && (alwaysReturnData || (json.has("Live") && json.getBoolean("Live")))) {
                        streamingChannels.put(channelId, json);
                    }
                }
            }
        } else {
            String url = ODYSEE_LIVESTREAM_SUBSCRIBED_LIVE_STATUS_API.concat(Helper.join(channelIds, ","));

            JSONArray jsonData = fetchLiveStatusData(url, builder, client);

            if (jsonData != null) {
                int s = jsonData.length();

                for (int i = 0; i < s; i++) {
                    JSONObject channelData = jsonData.getJSONObject(i);

                    if (channelData.has("ChannelClaimID") && channelIds.contains(channelData.getString("ChannelClaimID"))
                            && (alwaysReturnData || (channelData.has("Live") && channelData.getBoolean("Live")))) {
                        streamingChannels.put(channelData.getString("ChannelClaimID"), channelData);
                    }
                }
            }
        }
        return streamingChannels;
    }

    private JSONArray fetchLiveStatusData(String url, Request.Builder builder, OkHttpClient client) {
        builder.url(url);
        Request request = builder.build();

        JSONArray jsonData = null;
        try (Response resp = client.newCall(request).execute()) {
            ResponseBody body = resp.body();
            int responseCode = resp.code();

            if (body != null) {
                String responseString = body.string();
                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(responseString);
                    if (!json.isNull("data") && (json.has("success") && json.getBoolean("success"))) {
                        if (subscribed) {
                            jsonData = json.getJSONArray("data");
                        } else {
                            jsonData = new JSONArray();
                            jsonData.put(json.get("data"));
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonData;
    }
}
