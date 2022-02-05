package com.odysee.app.tasks;

import android.os.AsyncTask;

import com.odysee.app.adapter.CommentOption;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Comment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Performs the network call for a {@link CommentOption}
 */
public class CommentOptionTask extends AsyncTask<Void, Void, Void> {
    private final Comment comment;
    private final String                   authToken;
//    private final View progressView;
    private final CommentOption            option;
    private final CommentOptionTaskHandler handler;
    private final String hexDataSource;

    private Exception error = null;

    public interface CommentOptionTaskHandler {
        void fillJsonRpcParams(JSONObject params) throws JSONException;
        void onSuccess();
        void onError(Exception error);
    }

    public CommentOptionTask(final Comment comment, final CommentOption option, final String authToken, final String hexDataSource, final CommentOptionTaskHandler handler) {
        this.comment = comment;
//        this.progressView = progressView;
        this.option = option;
        this.authToken = authToken;
        this.handler = handler;
        this.hexDataSource = hexDataSource;
    }

    protected void onPreExecute() {
//        Helper.setViewVisibility(progressView, View.VISIBLE);
    }

    public Void doInBackground(Void... params) {
        Comment createdComment = null;
        ResponseBody responseBody = null;
        try {
            // check comments status endpoint
            Comments.checkCommentsEndpointStatus();

            JSONObject comment_body = new JSONObject();

            if (!Helper.isNullOrEmpty(comment.getParentId())) {
                comment_body.put("parent_id", comment.getParentId());
            }

            comment_body.put("comment_id", comment.getId());

            handler.fillJsonRpcParams(comment_body);

            if (authToken != null) {
                comment_body.put("auth_token", authToken);
            }

            JSONObject jsonChannelSign = Comments.channelSignWithCommentData(comment_body, comment, hexDataSource);

            if (jsonChannelSign.has("signature") && jsonChannelSign.has("signing_ts")) {
                comment_body.put("signature", jsonChannelSign.getString("signature"));
                comment_body.put("signing_ts", jsonChannelSign.getString("signing_ts"));
            }

            Response resp = Comments.performRequest(comment_body, "comment." + option.jsonRpcMethod);
            responseBody = resp.body();
            if (responseBody != null) {
                String responseString = responseBody.string();
                resp.close();
                JSONObject jsonResponse = new JSONObject(responseString);

                if ( this.option.isSuccess(jsonResponse) == false ) {
                    error = new Exception("JSONRPC call failed.");
                }
            }
        } catch (ApiCallException | ClassCastException | IOException | JSONException ex) {
            error = ex;
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }

        return (Void) null;
    }

    @Override
    protected void onPostExecute(Void voidParam) {
        if ( error == null ) {
            handler.onSuccess();
        } else {
            handler.onError(error);
        }
    }
}
