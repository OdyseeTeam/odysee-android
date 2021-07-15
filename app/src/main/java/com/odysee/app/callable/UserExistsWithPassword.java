package com.odysee.app.callable;

import android.content.Context;
import android.util.Log;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.odysee.app.utils.Lbryio.TAG;

public class UserExistsWithPassword implements Callable<Boolean> {
    private final Context ctx;
    private final String email;

    public UserExistsWithPassword(Context ctx, String email) {
        this.ctx = ctx;
        this.email = email;
    }

    @Override
    public Boolean call() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put("email", email);

        try {
            Response response = Lbryio.call("user", "exists", options, Helper.METHOD_POST, ctx);

            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseString = responseBody.string();
                    response.close();
                    JSONObject jsonData = new JSONObject(responseString);

                    if (jsonData.has("data"))
                        return (jsonData.getJSONObject("data").getBoolean("has_password"));
                } else {
                    response.close();
                }
            }
            return false;
        } catch (LbryioRequestException | LbryioResponseException e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }
}
