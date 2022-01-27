package com.odysee.app.model;

import lombok.Data;

@Data
public class YouTubeSyncItem {
    private Channel channel;
    private int totalPublishedVideos;
    private int totalTransferred;
    private boolean changed;

    @Data
    public static class Channel {
        private String ytChannelName;
        private String lbryChannelName;
        private String channelClaimId;
        private String syncStatus;
        private String statusToken;
        private boolean transferable;
        private String transferState;
        private boolean shouldSync;
        private boolean reviewed;
        private int totalSubs;
        private int totalVideos;
        private String[] publishToAddress;
        private String publicKey;
        private String channelCertificate;
    }
}
