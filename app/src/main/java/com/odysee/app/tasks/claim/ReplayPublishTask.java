package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReplayPublishTask extends AsyncTask<Void, Void, Claim> {
    private static final String PUBLISH_ENDPOINT = "https://publish.na-backend.odysee.com/v1";

    private final Claim claim;
    private final String remoteUrl;
    private final View progressView;
    private final String authToken;
    private final ClaimResultHandler handler;
    private Exception error;

    public ReplayPublishTask(Claim claim, String remoteUrl, View progressView, String authToken, ClaimResultHandler handler) {
        this.claim = claim;
        this.remoteUrl = remoteUrl;
        this.progressView = progressView;
        this.authToken = authToken;
        this.handler = handler;
    }

    @Override
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
        if (handler != null) {
            handler.beforeStart();
        }
    }

    @Override
    protected Claim doInBackground(Void... params) {
        Map<String, Object> options = Helper.buildPublishOptions(claim);
        Claim claimResult = null;

        try {
            JSONObject lbryParams = Lbry.buildJsonParams(options);
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("jsonrpc", "2.0");
            jsonPayload.put("method", Lbry.METHOD_PUBLISH);
            jsonPayload.put("params", lbryParams);
            jsonPayload.put("id", System.currentTimeMillis());

            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(Helper.FORM_DATA_MEDIA_TYPE)
                    .addFormDataPart("json_payload", jsonPayload.toString());

            if (remoteUrl != null) {
                multipartBuilder.addFormDataPart("remote_url", remoteUrl);
            }

            RequestBody body = multipartBuilder.build();
            Request request = new Request.Builder()
                    .url(new URL(PUBLISH_ENDPOINT))
                    .post(body)
                    .addHeader("X-Lbry-Auth-Token", authToken)
                    .build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject result = (JSONObject) Lbry.parseResponse(response);
            if (result.has("outputs")) {
                JSONArray outputs = result.getJSONArray("outputs");
                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject output = outputs.getJSONObject(i);
                    if (output.has("claim_id") && output.has("claim_op")) {
                        claimResult = Claim.claimFromOutput(output);
                        break;
                    }
                }
            }
        } catch (IOException | LbryResponseException | JSONException ex) {
            error = ex;
            return null;
        }

        return claimResult;
    }

    @Override
    protected void onPostExecute(Claim result) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (result != null) {
                handler.onSuccess(result);
            } else {
                handler.onError(error);
            }
        }
    }
}
