package com.odysee.app.listener;

import java.util.List;

import com.odysee.app.model.Claim;

public interface FetchClaimsListener {
    void onClaimsFetched(List<Claim> claims);
}
