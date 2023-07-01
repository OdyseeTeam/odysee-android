package com.odysee.app.tasks.claim;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Due to the TusExecutor callback, result is saved in claimResult
// instead of returning through AsyncTask methods.
public class TusPublishTask extends AsyncTask<Void, Integer, Void> {
    private static final int NOTIFY_RETRY_INTERVAL = 5000;
    private static final int STATUS_RETRY_COUNT = 12;
    private static final int STATUS_RETRY_INTERVAL = 10000;

    private final Claim claim;
    private final String filePath;
    private final String uploadUrl;
    private final ProgressBar progressView;
    private final String authToken;
    private final ClaimResultHandler handler;

    private Exception error;
    private Claim claimResult;

    public TusPublishTask(Claim claim, String filePath, String uploadUrl,
                          ProgressBar progressView, String authToken, ClaimResultHandler handler) {
        this.claim = claim;
        this.filePath = filePath;
        this.uploadUrl = uploadUrl;
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
        if (!Helper.isNullOrEmpty(uploadUrl)) {
            sendStatusRequest(uploadUrl, STATUS_RETRY_COUNT);
            return null;
        }

        try {
            TusClient client = new TusClient();
            client.setUploadCreationURL(new URL("https://api.na-backend.odysee.com/api/v3/publish/"));
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
                    do {
                        long total = upload.getSize();
                        long bytesUploaded = uploader.getOffset();
                        int progress = (int) ((double) bytesUploaded / total * 100);
                        publishProgress(progress);
                    } while (uploader.uploadChunk() > -1);
                    uploader.finish();

                    publishProgress(-1);
                    makeNotifyRequest(uploader.getUploadURL().toString());
                }
            };
            executor.makeAttempts();
        } catch (ProtocolException | IOException ex) {
            error = ex;
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress >= 0) {
            progressView.setIndeterminate(false);
            progressView.setProgress(progress);
        } else { // -1 = not uploading, so show indeterminate
            progressView.setIndeterminate(true);
        }
    }

    void makeNotifyRequest(String uploadUrl) {
        URL notifyUrl;
        JSONObject requestBody = new JSONObject();
        try {
            notifyUrl = new URL(uploadUrl + "/notify");

            Map<String, Object> options = Helper.buildPublishOptions(claim);
            JSONObject params = Lbry.buildJsonParams(options);
            long counter = Double.valueOf(System.currentTimeMillis() / 1000.0).longValue();
            requestBody.put("jsonrpc", "2.0");
            requestBody.put("method", Lbry.METHOD_PUBLISH);
            requestBody.put("params", params);
            requestBody.put("counter", counter);
        } catch (JSONException | MalformedURLException ex) {
            error = ex;
            return;
        }

        RequestBody body = RequestBody.create(requestBody.toString(), Helper.JSON_MEDIA_TYPE);
        Request.Builder requestBuilder = new Request.Builder().url(notifyUrl).post(body);
        requestBuilder.addHeader("X-Lbry-Auth-Token", authToken);
        requestBuilder.addHeader("Tus-Resumable", "1.0.0");
        Request request = requestBuilder.build();
        OkHttpClient client  = new OkHttpClient.Builder()
                .writeTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        try {
            Response response = client.newCall(request).execute();

            try {
                Lbry.parseResponse(response);
            } catch (LbryResponseException ex) {
                if (ex.getMessage() != null &&
                        ex.getMessage().equalsIgnoreCase("upload is still in process")) {
                    Thread.sleep(NOTIFY_RETRY_INTERVAL);
                    makeNotifyRequest(uploadUrl);
                    return;
                }
            }

            sendStatusRequest(uploadUrl, STATUS_RETRY_COUNT);
        } catch (IOException | InterruptedException ex) {
            error = ex;
        }
    }

    void sendStatusRequest(String uploadUrl, int retryCount) {
        try {
            URL statusUrl = new URL(uploadUrl + "/status");
            Request.Builder requestBuilder = new Request.Builder().url(statusUrl).get();
            requestBuilder.addHeader("Content-Type", "application/json");
            requestBuilder.addHeader("X-Lbry-Auth-Token", authToken);
            requestBuilder.addHeader("Tus-Resumable", "1.0.0");
            Request request = requestBuilder.build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .build();

            Response response = client.newCall(request).execute();

            switch (response.code()) {
                case 200:
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
                    break;

                case 202:
                    if (retryCount > 0) {
                        Thread.sleep(STATUS_RETRY_INTERVAL);
                        sendStatusRequest(uploadUrl, retryCount - 1);
                    } else {
                        error = new CheckStatusException(
                                "The file is still being processed. Check back later after a few minutes.",
                                uploadUrl
                        );
                    }
                    break;

                case 403:
                case 404:
                    error = new LbryResponseException("The upload does not exist");
                    break;

                case 409:
                    // Get SDK error from response
                    try {
                        Lbry.parseResponse(response);
                    } catch (LbryResponseException ex) {
                        if (ex.getMessage() != null) {
                            error = new LbryResponseException("Failed to process the uploaded file: " + ex.getMessage());
                        } else  {
                            error = new LbryResponseException("Failed to process the uploaded file");
                        }
                    }
                    break;

                default:
                    error = new LbryResponseException("Unexpected error: " + response.code());
            }
        } catch (IOException | InterruptedException | LbryResponseException | JSONException ex) {
            error = ex;
        }
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

    // Not actually an exception, just a handy type for returning a value.
    // Used when status request returns 202 and it's exceeded retry count.
    public static class CheckStatusException extends Exception {
        @Getter
        private final String uploadUrl;
        public CheckStatusException(String message, String uploadUrl) {
            super(message);
            this.uploadUrl = uploadUrl;
        }
    }
}
