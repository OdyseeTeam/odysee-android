package com.odysee.app.supplier;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Lbry;

import java.util.function.Supplier;

public class WalletGetUnusedAddressSupplier implements Supplier<String> {
    String authToken;

    public WalletGetUnusedAddressSupplier(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String get() {
        String address = null;
        try {
            address = (String) Lbry.directApiCall(Lbry.METHOD_ADDRESS_UNUSED, authToken);
        } catch (ApiCallException | ClassCastException ex) {
            ex.printStackTrace();
        }

        return address;
    }
}
