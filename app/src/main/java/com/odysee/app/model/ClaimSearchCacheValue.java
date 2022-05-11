package com.odysee.app.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ClaimSearchCacheValue {
    @Getter
    private final Page claimsPage;
    @Getter
    private final long timestamp;

    public ClaimSearchCacheValue(Page claimsPage, long timestamp) {
        this.claimsPage = claimsPage;
        this.timestamp = timestamp;
    }

    public boolean isExpired(long ttl) {
        return System.currentTimeMillis() - timestamp > ttl;
    }
}
