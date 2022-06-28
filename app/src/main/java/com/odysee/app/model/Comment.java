package com.odysee.app.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
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

    public static class StreamerChannelSpan extends ReplacementSpan
    {
        private float padding;
        private RectF rect;
        private int foregroundColour;
        private int backgroundColour;
        public StreamerChannelSpan(int foregroundColour, int backgroundColour, float padding) {
            rect = new RectF();
            this.foregroundColour = foregroundColour;
            this.backgroundColour = backgroundColour;
            this.padding = padding;
        }
        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            rect.set(x, top, x + paint.measureText(text, start, end) + padding, bottom);
            paint.setColor(backgroundColour);
            canvas.drawRect(rect, paint);


            paint.setColor(foregroundColour);
            int xPos = Math.round(x + (padding / 2));
            canvas.drawText(text, start, end, xPos, y, paint);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return Math.round(paint.measureText(text, start, end) + padding);
        }
    }
}
