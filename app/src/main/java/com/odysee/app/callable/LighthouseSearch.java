package com.odysee.app.callable;

import com.odysee.app.model.Claim;
import com.odysee.app.utils.Lighthouse;

import java.util.List;
import java.util.concurrent.Callable;

public class LighthouseSearch implements Callable<List<Claim>> {
    private final String query;
    private final String relatedToClaimId;
    private final int size;
    private final int from;
    private final boolean nsfw;

    public LighthouseSearch(String query, int size, int from, boolean nsfw, String relatedToClaimId) {
        this.query = query;
        this.size = size;
        this.from = from;
        this.nsfw = nsfw;
        this.relatedToClaimId = relatedToClaimId;
    }

    @Override
    public List<Claim> call() throws Exception {
        return Lighthouse.search(query, size, from, nsfw, relatedToClaimId);
    }
}
