package com.odysee.app.tasks.wallet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

/*
  version: '0.1',
  value: {
    subscriptions?: Array<string>,
    tags?: Array<string>,
    blocked?: Array<string>,
    settings?: any,
    app_welcome_version?: number,
    sharing_3P?: boolean,
  },
 */
public class SaveSharedUserStateTask extends AsyncTask<Void, Void, Boolean> {
    private static final String KEY = "shared";
    private static final String VERSION = "0.1";
    private final SaveSharedUserStateHandler handler;
    private Exception error;
    private String authToken;
    private Context context;

    public SaveSharedUserStateTask(String authToken, Context context, SaveSharedUserStateHandler handler) {
        this.authToken = authToken;
        this.handler = handler;
        this.context = context;
    }

    protected Boolean doInBackground(Void... params) {
        boolean loadedSubs = false;
        boolean loadedBlocked = false;

        SQLiteDatabase db = null;
        if (context instanceof MainActivity) {
            db = ((MainActivity) context).getDbHelper().getReadableDatabase();
        }

        // data to save
        // current subscriptions
        List<Subscription> subs = new ArrayList<>();
        try {
            if (db != null) {
                subs = new ArrayList<>(DatabaseHelper.getSubscriptions(db));
                loadedSubs = true;
            }
        } catch (SQLiteException ex) {
            // pass
        }

        List<String> subscriptionUrls = new ArrayList<>();
        try {
            for (Subscription subscription : subs) {
                LbryUri uri = LbryUri.parse(LbryUri.normalize(subscription.getUrl()));
                subscriptionUrls.add(uri.toString());
            }
        } catch (LbryUriException ex) {
            error = ex;
            return false;
        }

        // followed tags
        List<String> followedTags = Helper.getTagsForTagObjects(Lbry.followedTags);

        // blocked channels
        List<LbryUri> blockedChannels = new ArrayList<>();
        try {
            if (db != null) {
                blockedChannels = new ArrayList<>(DatabaseHelper.getBlockedChannels(db));
                loadedBlocked = true;
            }
        } catch (SQLiteException ex) {
            // pass
        }

        List<String> blockedChannelUrls = new ArrayList<>();
        for (LbryUri uri : blockedChannels) {
            blockedChannelUrls.add(uri.toString());
        }

        // Get the previous saved state
        try {
            boolean isExistingValid = false;
            JSONObject sharedObject = null;
            JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_PREFERENCE_GET, Lbry.buildSingleParam("key", KEY), authToken);
            if (result != null) {
                JSONObject shared = result.getJSONObject("shared");
                if (shared.has("type")
                        && "object".equalsIgnoreCase(shared.getString("type"))
                        && shared.has("value")) {
                    isExistingValid = true;
                    JSONObject value = shared.getJSONObject("value");
                    if (loadedSubs) {
                        // make sure the subs were actually loaded from the local store before overwriting the data
                        value.put("subscriptions", Helper.jsonArrayFromList(subscriptionUrls));
                        value.put("following", buildUpdatedNotificationsDisabledStates(subs));
                    }
                    value.put("tags", Helper.jsonArrayFromList(followedTags));
                    if (loadedBlocked) {
                        // make sure blocked list was actually loaded from the local store before overwriting
                        value.put("blocked", Helper.jsonArrayFromList(blockedChannelUrls));
                    }
                    sharedObject = shared;
                }
            }

            if (!isExistingValid) {
                // build a  new object
                JSONObject value = new JSONObject();
                value.put("subscriptions", Helper.jsonArrayFromList(subscriptionUrls));
                value.put("tags", Helper.jsonArrayFromList(followedTags));
                value.put("following", buildUpdatedNotificationsDisabledStates(subs));
                value.put("blocked", Helper.jsonArrayFromList(blockedChannelUrls));

                sharedObject = new JSONObject();
                sharedObject.put("type", "object");
                sharedObject.put("value", value);
                sharedObject.put("version", VERSION);
            }

            Map<String, Object> options = new HashMap<>();
            options.put("key", KEY);
            options.put("value", sharedObject.toString());

            Lbry.authenticatedGenericApiCall(Lbry.METHOD_PREFERENCE_SET, options, authToken);

            return true;
        } catch (ApiCallException | JSONException ex) {
            // failed
            error = ex;
        }
        return false;
    }

    private static JSONArray buildUpdatedNotificationsDisabledStates(List<Subscription> subscriptions) {
        JSONArray states = new JSONArray();
        for (Subscription subscription : subscriptions) {
            if (!Helper.isNullOrEmpty(subscription.getUrl())) {
                try {
                    JSONObject item = new JSONObject();
                    LbryUri uri = LbryUri.parse(LbryUri.normalize(subscription.getUrl()));
                    item.put("uri", uri.toString());
                    item.put("notificationsDisabled", subscription.isNotificationsDisabled());
                    states.put(item);
                } catch (JSONException | LbryUriException ex) {
                    // pass

                }
            }
        }

        return states;
    }

    protected void onPostExecute(Boolean result) {
        if (handler != null) {
            if (result) {
                handler.onSuccess();
            } else {
                handler.onError(error);
            }
        }
    }

    public interface SaveSharedUserStateHandler {
        void onSuccess();
        void onError(Exception error);
    }
}
