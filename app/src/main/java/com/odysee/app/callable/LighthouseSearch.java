package com.odysee.app.callable;

import com.odysee.app.utils.Lighthouse;

import java.util.List;
import java.util.concurrent.Callable;

public class LighthouseSearch implements Callable<List<String>> {
    private final String query;
    private final String relatedToClaimId;
    private final String claimType;
    private final String mediaTypes;
    private final String timeFilter;
    private final String sortBy;
    private final int size;
    private final int from;
    private final boolean nsfw;

    public LighthouseSearch(String query,
                            int size,
                            int from,
                            boolean nsfw,
                            String relatedToClaimId,
                            String claimType,
                            String mediaTypes,
                            String timeFilter,
                            String sortBy) {
        this.query = query;
        this.size = size;
        this.from = from;
        this.nsfw = nsfw;
        this.relatedToClaimId = relatedToClaimId;
        this.claimType = claimType;
        this.mediaTypes = mediaTypes;
        this.timeFilter = timeFilter;
        this.sortBy = sortBy;
    }

    @Override
    public List<String> call() throws Exception {
        return Lighthouse.search(query, size, from, nsfw, relatedToClaimId, claimType, mediaTypes, timeFilter, sortBy);
    }
}
