package com.odysee.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.odysee.app.R;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Comment;
import com.odysee.app.ui.text.style.StreamerChannelSpan;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Comments {
    private static final String EMOTICON_BASE_URL = "https://static.odycdn.com/emoticons/48%20px";
    private static final String STICKER_BASE_URL = "https://static.odycdn.com/stickers";

    private static final String STATUS_ENDPOINT = "https://comments.lbry.com";
    public static final String COMMENT_SERVER_ENDPOINT = "https://comments.lbry.com/api/v2";

    public static JSONObject channelSignName(JSONObject params, final String channelId, final String channelName) throws ApiCallException, JSONException {
        // NOTE: Intentionally passing in channelName twice in a row.
        return channelSignPrivate(params, channelId, channelName, channelName);
    }

    public static JSONObject channelSignWithCommentData(JSONObject params, Comment comment, final String hexDataSource) throws ApiCallException, JSONException {
        return channelSignPrivate(params, comment.getChannelId(), comment.getChannelName(), hexDataSource);
    }

    public static JSONObject channelSignPrivate(JSONObject params, final String channelId, final String channelName, final String hexDataSource) throws ApiCallException, JSONException {
        final String hexData = Helper.toHexString(hexDataSource);

        Map<String, Object> signingParams = new HashMap<>(3);
        signingParams.put("hexdata", hexData);
        signingParams.put("channel_id", channelId);
        signingParams.put("channel_name", channelName);

        if (params != null && params.has("auth_token")) {
            return (JSONObject) Lbry.authenticatedGenericApiCall("channel_sign", signingParams, params.getString("auth_token"));
        }

        return (JSONObject) Lbry.genericApiCall("channel_sign", signingParams);
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

    /**
     * Build a chat line by using SpannableStringBuilder to be displayed in live chat
     * @param context the context
     * @return the spannable to be displayed
     */
    public static Spannable getChatLine(Comment comment, String streamerClaimId, Comment.CommenterClickHandler handler, TextView textView, Context context) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        SpannableString commenterSpan = new SpannableString(comment.getChannelName());
        ClickableSpan cs = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                if (handler != null) {
                    handler.onCommenterClick(comment.getChannelName(), comment.getChannelId());
                }
            }
            @Override
            public void updateDrawState(TextPaint textPaint) {
                textPaint.setUnderlineText(false);
            }
        };

        int cmSpanEnd = commenterSpan.length();
        int flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        commenterSpan.setSpan(cs, 0, cmSpanEnd, flag);

        commenterSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, cmSpanEnd, flag);
        if (streamerClaimId != null && streamerClaimId.equalsIgnoreCase(comment.getChannelId())) {
            commenterSpan.setSpan(new StreamerChannelSpan(ContextCompat.getColor(context, R.color.white),
                    ContextCompat.getColor(context, R.color.colorPrimary), 20.0f), 0, cmSpanEnd, flag);
        } else {
            commenterSpan.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(context, comment.isHyperchat() ? R.color.colorPrimary : R.color.actionGrey)), 0, cmSpanEnd, flag);
        }

        ssb.append(commenterSpan).append(" ");

        // build spans from text if there are images
        // TODO: Replace channel names with clickable links in span text
        String text = comment.getText();
        if (text.indexOf(':') > -1) {
            ssb.append(buildCommentWithStickers(text, textView));
        } else {
            ssb.append(text);
        }

        return ssb;
    }

    public static Spannable buildCommentWithStickers(String text, TextView textView) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        List<String> imageNames = new ArrayList<>();
        StringBuilder plainText = new StringBuilder();

        boolean inSticker = false;
        StringBuilder imageName = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ':') {
                if (!inSticker && (i + 1 < text.length() && text.substring(i + 1).contains(":"))) {
                    // TODO: Additional inSticker parsing (check that the regex character range is [a-z0-9_])+ within matching colon pairs
                    inSticker = true;
                    imageName = new StringBuilder();

                    // reset plainText
                    ssb.append(plainText.toString());
                    plainText = new StringBuilder();
                } else if (inSticker) {
                    inSticker = false;
                    String name = imageName.toString();
                    if (isValidEmojiOrSticker(name)) {
                        imageNames.add(name);

                        Spannable span = null;
                        try {
                            Emote emote = Emote.valueOf(name);
                            span = (Spannable) Html.fromHtml(
                                    String.format("<img src=\"%s/%s\" />", EMOTICON_BASE_URL, emote.getPath("%402x")),
                                    new GlideImageGetter(textView), null);
                        } catch (IllegalArgumentException ex) {
                            try {
                                Sticker sticker = Sticker.valueOf(name);
                                span = (Spannable) Html.fromHtml(
                                        String.format("<img src=\"%s/%s\" />", STICKER_BASE_URL, sticker.getPath()),
                                        new GlideImageGetter(textView), null);
                            } catch (IllegalArgumentException stex) {
                                // pass
                            }
                        }

                        if (span != null) {
                            ssb.append(span);
                        }
                    } else {
                        // append the text as is
                        ssb.append(String.format(":%s:", name));
                    }
                }
            }

            if (inSticker) {
                if (text.charAt(i) == ':') {
                    continue;
                }
                imageName.append(text.charAt(i));
            } else {
                plainText.append(text.charAt(i));
            }
        }

        if (plainText.length() > 0) {
            ssb.append(plainText.toString());
        }

        return ssb;
    }

    public static boolean isValidEmojiOrSticker(String name) {
        return Emote.isEmote(name) || Sticker.isSticker(name);
    }
}
