package com.odysee.app.model.lbryinc;

import com.odysee.app.model.Claim;
import com.odysee.app.utils.LbryUri;

import lombok.Getter;
import lombok.Setter;

public class Subscription {
    @Getter
    @Setter
    private String channelName;
    @Getter
    @Setter
    private String url;
    @Getter
    @Setter
    private boolean isNotificationsDisabled;

    public Subscription() {

    }
    public Subscription(String channelName, String url, boolean isNotificationsDisabled) {
        this.channelName = channelName;
        this.url = url;
        this.isNotificationsDisabled = isNotificationsDisabled;
    }

    public static Subscription fromClaim(Claim claim) {
        String u = claim.getPermanentUrl();
        LbryUri lbryUri = LbryUri.tryParse(u);

        if (lbryUri != null)
            u = lbryUri.toString();

        return new Subscription(claim.getName(), u, true);
    }
    public String toString() {
        return url;
    }

    public boolean equals(Object o) {
        return (o instanceof Subscription) && url != null && url.equalsIgnoreCase(((Subscription) o).getUrl());
    }
    public int hashCode() {
        return url.toLowerCase().hashCode();
    }
}
