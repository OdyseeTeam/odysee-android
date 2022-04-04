package com.odysee.app.runnable;

import android.util.Log;

import com.odysee.app.utils.Comments;

import org.json.JSONObject;

import okhttp3.ResponseBody;

public class ReactToComment implements Runnable {
    private final JSONObject options;

    public ReactToComment(JSONObject options) {
        this.options = options;
    }

    @Override
    public void run() {
        try (okhttp3.Response response = Comments.performRequest(options, "reaction.React")) {
            ResponseBody responseBody = response.body();

            if (responseBody != null) {
                JSONObject jsonResponse = new JSONObject(responseBody.string());

                if (!jsonResponse.has("result")) {
                    Log.e("ReactingToComment", jsonResponse.getJSONObject("error").getString("message"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
