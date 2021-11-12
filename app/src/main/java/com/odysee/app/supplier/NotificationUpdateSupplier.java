package com.odysee.app.supplier;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.utils.Lbryio;

import java.util.Map;
import java.util.function.Supplier;

public class NotificationUpdateSupplier implements Supplier<Boolean> {
    private final Map<String, String> options;

    public NotificationUpdateSupplier(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public Boolean get() {
        try {
            Object result = Lbryio.parseResponse(Lbryio.call("notification", "edit", options, null));
            String resultString = "";

            if (result != null)
                resultString = result.toString();

            return "ok".equalsIgnoreCase(resultString);
        } catch (LbryioResponseException | LbryioRequestException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
