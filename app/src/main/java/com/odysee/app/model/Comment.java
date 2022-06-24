package com.odysee.app.model;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.odysee.app.R;
import com.odysee.app.utils.Helper;
import lombok.Data;

@Data
public class Comment implements Comparable<Comment> {
    public static final int MAX_LENGTH = 2000;

    private CommenterClickHandler handler;
    private Claim poster;
    private String claimId;
    private long timestamp;
    private String channelId;
    private String channelName, text, id, parentId;
    private Reactions reactions;

    public Comment(String channelId, String channelName, String text, String id, String parentId) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.text = text;
        this.id = id;
        this.parentId = parentId;
    }

    public Comment() {

    }

    /**
     * Build a chat line by using SpannableStringBuilder to be displayed in live chat
     * @param context the context
     * @return the spannable to be displayed
     */
    public Spannable getChatLine(Context context) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        SpannableString commenterSpan = new SpannableString(channelName);
        ClickableSpan cs = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                if (handler != null) {
                    handler.onCommenterClick(channelName, channelId);
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
        commenterSpan.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)), 0, cmSpanEnd, flag);
        commenterSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, cmSpanEnd, flag);

        ssb.append(commenterSpan).append(" ");
        ssb.append(text);

        return ssb;
    }

    public static Comment fromJSONObject(JSONObject jsonObject) {
        try {
            String parentId = null;
            if (jsonObject.has("parent_id")) {
                parentId = jsonObject.getString("parent_id");
            }

            Comment comment = new Comment(
                    Helper.getJSONString("channel_id", null, jsonObject),
                    jsonObject.getString("channel_name"),
                    jsonObject.getString("comment"),
                    jsonObject.getString("comment_id"),
                    parentId
            );
            comment.setClaimId(Helper.getJSONString("claim_id", null, jsonObject));
            comment.setTimestamp(Helper.getJSONLong("timestamp", 0, jsonObject));
            return comment;
        } catch (JSONException ex) {
            return null;
        }
    }

    @Override
    public int compareTo(Comment comment) {
        return (int)(this.getTimestamp() - comment.getTimestamp());
    }

    public interface CommenterClickHandler {
        void onCommenterClick(String commenter, String commenterClaimId);
    }
}
