package com.odysee.app.supplier;

import android.accounts.AccountManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Lbry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Supplier;

import okhttp3.ResponseBody;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ReactToCommentSupplier implements Supplier<Boolean> {
    private final AccountManager am;
    private final JSONObject options;

    public ReactToCommentSupplier(AccountManager am, JSONObject options) {
        this.am = am;
        this.options = options;
    }

    @Override
    public Boolean get() {
        if (Lbry.ownChannels.size() > 0) {
            try {
                options.put("channel_id", Lbry.ownChannels.get(0).getClaimId());
                options.put("channel_name", Lbry.ownChannels.get(0).getName());

                JSONObject jsonChannelSign = Comments.channelSign(options, options.getString("channel_id"), options.getString("channel_name"));

                if (jsonChannelSign.has("signature") && jsonChannelSign.has("signing_ts")) {
                    options.put("signature", jsonChannelSign.getString("signature"));
                    options.put("signing_ts", jsonChannelSign.getString("signing_ts"));
                }
            } catch (JSONException | ApiCallException e) {
                e.printStackTrace();
            }
        }

        JSONObject data = null;
        try {
            if (am.getAccounts().length > 0) {
                okhttp3.Response response = Comments.performRequest(options, "reaction.React");

                ResponseBody responseBody = response.body();

                if (responseBody!= null) {
                    JSONObject jsonResponse = new JSONObject(responseBody.string());

                    if (jsonResponse.has("result")) {
                        data = jsonResponse.getJSONObject("result");
                    } else {
                        Log.e("ReactingToComment", jsonResponse.getJSONObject("error").getString("message"));
                    }
                }
                response.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data != null && !data.has("error");
    }
}
