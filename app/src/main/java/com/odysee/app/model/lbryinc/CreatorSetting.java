package com.odysee.app.model.lbryinc;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreatorSetting {
    private boolean commentsEnabled;
    private String words;
    private BigDecimal minTipAmountComment;
    private BigDecimal minTipAmountSuperChat;
    private int slowModeMinGap; // Unit: seconds
    private int timeSinceFirstComment; // Unit: minutes

    public double getMinTipAmountCommentValue() {
        return minTipAmountComment != null ? minTipAmountComment.doubleValue() : 0;
    }
    public double getMinTipAmountSuperChatValue() {
        return minTipAmountSuperChat != null ? minTipAmountSuperChat.doubleValue() : 0;
    }

    public String getTimeSinceFirstCommentString() {
        if (timeSinceFirstComment <= 0) {
            return "";
        }

        if (timeSinceFirstComment < 60) {
            return String.format("%dm", timeSinceFirstComment);
        }

        if (timeSinceFirstComment < 60 * 24) {
            return String.format("%dh", Double.valueOf(Math.round(timeSinceFirstComment / 60)).intValue());
        }

        if (timeSinceFirstComment < 60 * 24 * 30) {
            return String.format("%dd", Double.valueOf(Math.round(timeSinceFirstComment / (60 * 24))).intValue());
        }

        return String.format("%dM", Double.valueOf(Math.round(timeSinceFirstComment / (60 * 24 * 30))).intValue());
    }
}
