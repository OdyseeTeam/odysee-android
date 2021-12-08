package com.odysee.app.callable;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Lbry;

import java.util.concurrent.Callable;

public class WalletGetUnusedAddress implements Callable<String> {
    String token;

    public WalletGetUnusedAddress(String token) {
        this.token = token;
    }

    @Override
    public String call() throws Exception {
        String address = null;
        try {
            address = (String) Lbry.directApiCall(Lbry.METHOD_ADDRESS_UNUSED, token);
        } catch (ApiCallException | ClassCastException ex) {
            ex.printStackTrace();
        }

        return address;
    }
}