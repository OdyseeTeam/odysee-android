package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;

import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Due to the TusExecutor callback, result is saved in claimResult
// instead of returning through AsyncTask methods.
public class TusPublishTask extends AsyncTask<Void, Void, Void> {
    private final Claim claim;
    private final String filePath;
    private final View progressView;
    private final String authToken;
    private final ClaimResultHandler handler;

    private Exception error;
    private Claim claimResult;

    public TusPublishTask(Claim claim, String filePath, View progressView,
                          String authToken, ClaimResultHandler handler) {
        this.claim = claim;
        this.filePath = filePath;
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
    protected Void doInBackground(Void... voids) {
        try {
            TusClient client = new TusClient();
            client.setUploadCreationURL(new URL("https://api.na-backend.odysee.com/api/v2/publish/"));
            // TODO: enableResuming
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Lbry-Auth-Token", authToken);
            client.setHeaders(headers);

            File file = new File(filePath);
            TusUpload upload = new TusUpload(file);

            TusExecutor executor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    TusUploader uploader = client.createUpload(upload);
                    uploader.setChunkSize(1024 * 1024);
                    while (uploader.uploadChunk() > -1);
                    uploader.finish();

                    URL notifyURL = new URL(uploader.getUploadURL().toString() + "/notify");
                    JSONObject requestBody = new JSONObject();
                    try {
                        Map<String, Object> options = Helper.buildPublishOptions(claim);

                        JSONObject params = Lbry.buildJsonParams(options);
                        long counter = Double.valueOf(System.currentTimeMillis() / 1000.0).longValue();
                        requestBody.put("jsonrpc", "2.0");
                        requestBody.put("method", Lbry.METHOD_PUBLISH);
                        requestBody.put("params", params);
                        requestBody.put("counter", counter);
                    } catch (JSONException ex) {
                        error = ex;
                        return;
                    }

                    RequestBody body = RequestBody.create(requestBody.toString(), Helper.JSON_MEDIA_TYPE);
                    Request.Builder requestBuilder = new Request.Builder().url(notifyURL).post(body);
                    requestBuilder.addHeader("X-Lbry-Auth-Token", authToken);
                    requestBuilder.addHeader("Tus-Resumable", "1.0.0");
                    Request request = requestBuilder.build();
                    OkHttpClient client  = new OkHttpClient.Builder()
                            .writeTimeout(300, TimeUnit.SECONDS)
                            .readTimeout(300, TimeUnit.SECONDS)
                            .build();

                    try {
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
                    } catch (IOException | LbryResponseException | ClassCastException
                            | JSONException ex) {
                        error = ex;
                    }
                }
            };
            executor.makeAttempts();
        } catch (ProtocolException | IOException ex) {
            error = ex;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (claimResult != null) {
                handler.onSuccess(claimResult);
            } else {
                handler.onError(error);
            }
        }
    }
}
