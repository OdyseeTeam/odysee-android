package com.odysee.app.supplier;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Lbry;

import java.util.Map;
import java.util.function.Supplier;

@RequiresApi(api = Build.VERSION_CODES.N)
public class UnlockingTipsSupplier implements Supplier<Boolean> {
    private final Map<String, Object> options;
    private final String authToken;
    public UnlockingTipsSupplier(Map<String, Object> options, String authToken) {
        this.options = options;
        this.authToken = authToken;
    }

    @Override
    public Boolean get() {
        try {
            Lbry.directApiCall(Lbry.METHOD_TXO_SPEND, options, authToken);
            return true;
        } catch (ApiCallException | ClassCastException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
