package com.odysee.app.callable;

import android.util.Log;

import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Lbry;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class CommentEnabled implements Callable<Boolean> {
    private static final String TAG = CommentEnabled.class.getSimpleName();
    private static final String METHOD_COMMENT_LIST = "comment.List";

    private final String channelId;
    private final String channelName;

    public CommentEnabled(String channelId, String channelName) {
        this.channelId = channelId;
        this.channelName = channelName;
    }

    @Override
    public Boolean call() {
        Map<String, Object> params = new HashMap<>(3);
        params.put("claim_id", channelId);
        params.put("channel_id", channelId);
        params.put("channel_name", channelName);

        try {
            JSONObject result = (JSONObject) Lbry.parseResponse(Comments.performRequest(Lbry.buildJsonParams(params), METHOD_COMMENT_LIST));
            if (result == null || result.has("error")) {
                return false;
            }
        } catch (LbryResponseException | IOException e) {
            Log.e(TAG, "Error while fetching comments", e);
            return false;
        }
        return true;
    }
}