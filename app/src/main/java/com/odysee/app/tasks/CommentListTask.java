package com.odysee.app.tasks;

import static java.util.stream.Collectors.groupingBy;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.LbryResponseException;
import com.odysee.app.model.Comment;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

public class CommentListTask extends AsyncTask<Void, Void, List<Comment>> {
    private final int page;
    private final int pageSize;
    private final String claim;
    private final ProgressBar progressBar;
    private final CommentListHandler handler;
    private Exception error;

    public CommentListTask(int page, int pageSize, String claim, ProgressBar progressBar, CommentListHandler handler) {
        this.page = page;
        this.pageSize = pageSize;
        this.claim = claim;
        this.progressBar = progressBar;
        this.handler = handler;
    }

    protected void onPreExecute() {
        Helper.setViewVisibility(progressBar, View.VISIBLE);
    }

    protected List<Comment> doInBackground(Void... voids) {
        List<Comment> comments = null;

        try {
            Map<String, Object> options = new HashMap<>();

            options.put("claim_id", claim);
            options.put("page", page);
            options.put("page_size", pageSize);
            options.put("hidden", false);
            options.put("include_replies", false);
            options.put("is_channel_signature_valid", true);
            options.put("skip_validation", true);
            options.put("visible", true);

            JSONObject result = (JSONObject) Lbry.parseResponse(Comments.performRequest(Lbry.buildJsonParams(options), "comment.List"));

            if (result != null && result.has("items")) {
                JSONArray items = result.getJSONArray("items");

                List<Comment> children = new ArrayList<>();
                comments = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    Comment comment = Comment.fromJSONObject(items.getJSONObject(i));
                    if (comment != null) {
                        if (!Helper.isNullOrEmpty(comment.getParentId())) {
                            children.add(comment);
                        } else {
                            comments.add(comment);
                        }
                    }
                }

                // Sort all replies from oldest to newest at once and then group them by its parent comment
                Collections.sort(children);

                Map<String, List<Comment>> groupedChildrenList = children.stream().collect(groupingBy(Comment::getParentId));
                List<Comment> finalComments = comments;
                groupedChildrenList.forEach((key, value) -> {
                    Comment c = finalComments.stream().filter(v -> key.equalsIgnoreCase(v.getId())).findFirst().orElse(null);
                    finalComments.addAll(finalComments.indexOf(c) + 1, value);
                });
                comments = finalComments;
            }
        } catch (JSONException | LbryResponseException | IOException ex) {
            error = ex;
        }
        return comments;
    }

    protected void onPostExecute(List<Comment> comments) {
        Helper.setViewVisibility(progressBar, View.GONE);
        if (handler != null) {
            if (comments != null) {
                handler.onSuccess(comments, comments.size() < pageSize);
            } else {
                handler.onError(error);
            }
        }
    }
}
