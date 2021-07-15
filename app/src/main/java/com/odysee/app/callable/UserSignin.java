package com.odysee.app.callable;

import android.content.Context;
import android.util.Log;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Callable;

import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.odysee.app.utils.Lbryio.TAG;

public class UserSignin implements Callable<Boolean> {
    private final Context ctx;
    private final Map<String, String> options;

    public UserSignin(Context ctx, Map<String, String> options) {
        this.ctx = ctx;
        this.options = options;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            Response responseSignIn = Lbryio.call("user", "signin", options, Helper.METHOD_POST, ctx);
            if (responseSignIn.isSuccessful()) {
                ResponseBody responseBody = responseSignIn.body();
                if (responseBody != null) {
                    String responseString = responseBody.string();
                    responseSignIn.close();
                    JSONObject responseJson = new JSONObject(responseString);
                    if (responseJson.getBoolean("success")) {
                        JSONObject jsondata = responseJson.getJSONObject("data");
                        return jsondata.has("primary_email") && jsondata.getString("primary_email").equals(options.get("email"));
                    }
                }
            }
            return false;
        } catch (LbryioRequestException | LbryioResponseException e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }
}
