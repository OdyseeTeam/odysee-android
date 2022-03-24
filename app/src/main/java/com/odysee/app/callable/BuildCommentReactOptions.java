package com.odysee.app.callable;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.utils.Comments;
import com.odysee.app.utils.Lbry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class BuildCommentReactOptions implements Callable<JSONObject> {
    private final JSONObject options;

    public BuildCommentReactOptions(JSONObject options) {
        this.options = options;
    }

    @Override
    public JSONObject call() throws Exception {
        if (Lbry.ownChannels.size() > 0) {
            try {
                String channelId = Lbry.ownChannels.get(0).getClaimId();
                String channelName= Lbry.ownChannels.get(0).getName();

                options.put("channel_id", channelId);
                options.put("channel_name", channelName);

                JSONObject jsonChannelSign = Comments.channelSignName(options, options.getString("channel_id"), options.getString("channel_name"));

                if (jsonChannelSign.has("signature") && jsonChannelSign.has("signing_ts")) {
                    options.put("signature", jsonChannelSign.getString("signature"));
                    options.put("signing_ts", jsonChannelSign.getString("signing_ts"));
                }
            } catch (JSONException | ApiCallException e) {
                e.printStackTrace();
            }
        }

        return options;
    }
}
