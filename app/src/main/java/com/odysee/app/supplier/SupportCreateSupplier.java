package com.odysee.app.supplier;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Lbry;

import java.util.Map;
import java.util.function.Supplier;

public class SupportCreateSupplier implements Supplier<String> {
    private final Map<String, Object> options;
    private final String authToken;

    public SupportCreateSupplier(Map<String, Object> options, String authToken) {
        this.options = options;
        this.authToken = authToken;
    }

    @Override
    public String get() {
        String error = null;
        try {
            Lbry.authenticatedGenericApiCall(Lbry.METHOD_SUPPORT_CREATE, options, authToken);
        } catch (ApiCallException ex) {
            ex.printStackTrace();
            return ex.getMessage();
        }
        return error;
    }
}
