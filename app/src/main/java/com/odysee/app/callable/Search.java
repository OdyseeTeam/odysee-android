package com.odysee.app.callable;

import com.odysee.app.model.Claim;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class Search implements Callable<List<Claim>> {
    private final Map<String, Object> options;

    public Search(Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public List<Claim> call() throws Exception {
        return Helper.filterInvalidReposts(Lbry.claimSearch(options, Lbry.API_CONNECTION_STRING).getClaims());
    }
}
