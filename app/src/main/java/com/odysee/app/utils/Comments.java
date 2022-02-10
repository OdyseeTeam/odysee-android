package com.odysee.app.utils;

import android.os.Build;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Comment;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Comments {
    private static final String STATUS_ENDPOINT = "https://comments.lbry.com";
    public static final String COMMENT_SERVER_ENDPOINT = "https://comments.lbry.com/api/v2";

    public static JSONObject channelSignName(JSONObject commentBody, final String channelId, final String channelName) throws ApiCallException, JSONException {
        // NOTE: Intentionally passing in channelName twice in a row.
        return channelSignPrivate(commentBody, channelId, channelName, channelName);
    }

    public static JSONObject channelSignWithCommentData(JSONObject commentBody, Comment comment, final String hexDataSource) throws ApiCallException, JSONException {
        return channelSignPrivate(commentBody, comment.getChannelId(), comment.getChannelName(), hexDataSource);
    }

    public static JSONObject channelSignPrivate(JSONObject commentBody, final String channelId, final String channelName, final String hexDataSource) throws ApiCallException, JSONException {

        final String hexData = toHexString(hexDataSource);

        Map<String, Object> signingParams = new HashMap<>(3);
        signingParams.put("hexdata", hexData);
        signingParams.put("channel_id", channelId);
        signingParams.put("channel_name", channelName);

        if (commentBody.has("auth_token"))
            return (JSONObject) Lbry.authenticatedGenericApiCall("channel_sign", signingParams, commentBody.getString("auth_token"));
        else
            return (JSONObject) Lbry.genericApiCall("channel_sign", signingParams);
    }

    private static String toHexString(final String value) {
        final byte[] commentBodyBytes = value.getBytes(StandardCharsets.UTF_8);

        final String hexString;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1)
            hexString = Hex.encodeHexString(commentBodyBytes, false);
        else
            hexString = new String(Hex.encodeHex(commentBodyBytes));

        return hexString;
    }

    /**
     * Performs request to default Comment Server
     * @param params JSON containing parameters to send to the server
     * @param method One of the available methods for comments
     * @return Response from the server
     * @throws IOException throwable from OkHttpClient execute()
     */
    public static Response performRequest(JSONObject params, String method) throws IOException {
        return performRequest(COMMENT_SERVER_ENDPOINT, params, method);
    }

    /**
     * Performs the request to Comment Server
     * @param commentServer Url where to direct the request
     * @param params JSON containing parameters to send to the server
     * @param method One of the available methods for comments
     * @return Response from the server
     * @throws IOException throwable from OkHttpClient execute()
     */
    public static Response performRequest(String commentServer, JSONObject params, String method) throws IOException {
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        Map<String, Object> requestParams = new HashMap<>(4);
        requestParams.put("jsonrpc", "2.0");
        requestParams.put("id", 1);
        requestParams.put("method", method);
        requestParams.put("params", params);

        final String jsonString = Lbry.buildJsonParams(requestParams).toString();
        RequestBody requestBody = RequestBody.create(jsonString, JSON);

        Request commentCreateRequest = new Request.Builder()
                                                  .url(commentServer.concat("?m=").concat(method))
                                                  .post(requestBody)
                                                  .build();

        OkHttpClient client = new OkHttpClient.Builder().writeTimeout(30, TimeUnit.SECONDS)
                                                        .readTimeout(30, TimeUnit.SECONDS)
                                                        .build();

        return client.newCall(commentCreateRequest).execute();
    }

    public static void checkCommentsEndpointStatus() throws IOException, JSONException, ApiCallException {
        Request request = new Request.Builder().url(STATUS_ENDPOINT).build();
        OkHttpClient client = new OkHttpClient.Builder().writeTimeout(30, TimeUnit.SECONDS)
                                                        .readTimeout(30, TimeUnit.SECONDS)
                                                        .build();
        Response response = client.newCall(request).execute();
        JSONObject status = new JSONObject(Objects.requireNonNull(response.body()).string());
        String statusText = Helper.getJSONString("text", null, status);
        boolean isRunning = Helper.getJSONBoolean("is_running", false, status);
        if (!"ok".equalsIgnoreCase(statusText) || !isRunning) {
            throw new ApiCallException("The comment server is not available at this time. Please try again later.");
        }
    }
}
