package com.odysee.app.supplier;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Supplier;

public class ChannelGetLiveStatus implements Supplier<JSONObject> {
    private final String channelClaimId;

    public ChannelGetLiveStatus(String channelClaimId) {
        this.channelClaimId = channelClaimId;
    }

    @Override
    public JSONObject get() {
        Request.Builder builder = new Request.Builder();
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = "https://api.odysee.live/livestream/is_live?channel_claim_id=".concat(channelClaimId);
        Log.i("OdyseeChannelLiveStatus", "get: getting channel live status from: " + url);
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
                        if (jsonData.has("Live") && jsonData.getBoolean("Live") && jsonData.has("ChannelClaimID") && jsonData.getString("ChannelClaimID").equals(channelClaimId)) {
                            return jsonData;
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
