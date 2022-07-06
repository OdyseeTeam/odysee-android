package com.odysee.app.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

import com.odysee.app.utils.Currency;
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
    private boolean hidden;
    private boolean pinned;
    private Reactions reactions;

    // Hyperchat fields
    private String currency;
    private BigDecimal supportAmount; // any comment with supportAmount > 0 will be treated as a hyperchat
    private boolean fiat;

    public Comment(String channelId, String channelName, String text, String id, String parentId) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.text = text;
        this.id = id;
        this.parentId = parentId;
    }

    public Comment() {

    }

    public boolean isHyperchat() {
        return supportAmount != null && supportAmount.doubleValue() > 0;
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
            comment.setHidden(Helper.getJSONBoolean("is_hidden", false, jsonObject));
            comment.setPinned(Helper.getJSONBoolean("is_pinned", false, jsonObject));

            // hyperchat fields
            comment.setSupportAmount(new BigDecimal(Helper.getJSONDouble("support_amount", 0, jsonObject)));
            comment.setCurrency(Helper.getJSONString("currency", "", jsonObject));
            comment.setFiat(Helper.getJSONBoolean("is_fiat", false, jsonObject));

            return comment;
        } catch (JSONException ex) {
            return null;
        }
    }

    public String getHyperchatValue() {
        if (supportAmount != null && supportAmount.doubleValue() > 0) {
            String value = Helper.SIMPLE_CURRENCY_FORMAT.format(supportAmount.doubleValue());
            if (fiat) {
                String currencyCode = currency.toUpperCase();
                if (Currency.isCurrency(currencyCode)) {
                    Currency curr = Currency.valueOf(currencyCode);
                    return String.format("%s%s",
                            curr.isSuffix() ? curr.getSymbol() : value,
                            curr.isSuffix() ? value : curr.getSymbol());
                }

                return String.format("%s %s", currencyCode, value);
            } else {
                return value;
            }
        }

        return "";
    }

    @Override
    public int compareTo(Comment comment) {
        return (int)(this.getTimestamp() - comment.getTimestamp());
    }

    public interface CommenterClickHandler {
        void onCommenterClick(String commenter, String commenterClaimId);
    }
}
