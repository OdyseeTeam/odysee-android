package com.odysee.app.model;

import lombok.Data;

@Data
public class YouTubeSyncItem {
    private Channel channel;
    private int totalPublishedVideos;
    private int totalTransferred;
    private boolean changed;
    private Claim claim;

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

        private int followerCount;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof YouTubeSyncItem)) {
            return false;
        }

        YouTubeSyncItem eo = (YouTubeSyncItem) o;
        if (eo.getClaim() != null && claim != null) {
            return eo.getClaim().getClaimId().equals(claim.getClaimId());
        }

        return eo.getChannel().getChannelClaimId().equals(channel.getChannelClaimId()) &&
                eo.getChannel().getYtChannelName().equalsIgnoreCase(channel.getYtChannelName());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
