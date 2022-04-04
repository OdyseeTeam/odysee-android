package com.odysee.app.tasks.wallet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.Tag;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

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
public class LoadSharedUserStateTask extends AsyncTask<Void, Void, Boolean> {
    private static final String KEY = "shared";

    private final Context context;
    private final LoadSharedUserStateHandler handler;
    private Exception error;
    private final String authToken;

    private List<Subscription> subscriptions;
    private List<Tag> followedTags;
    private List<LbryUri> blockedChannels;

    public LoadSharedUserStateTask(Context context, LoadSharedUserStateHandler handler, String authToken) {
        this.context = context;
        this.handler = handler;
        this.authToken = authToken;
    }

    protected Boolean doInBackground(Void... params) {
        // data to save
        // current subscriptions
        // Get the previous saved state
        try {
            SQLiteDatabase db = null;
            JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_PREFERENCE_GET, Lbry.buildSingleParam("key", KEY), authToken);
            if (result != null) {
                MainActivity activity =  null;
                if (context instanceof MainActivity) {
                    activity = (MainActivity) context;
                    db = activity.getDbHelper().getWritableDatabase();
                }

                // get the built in collections
                OdyseeCollection favoritesPlaylist = null;
                OdyseeCollection watchlaterPlaylist = null;
                if (db != null) {
                    Map<String, OdyseeCollection> allCollections = DatabaseHelper.loadAllCollections(db);
                    favoritesPlaylist = allCollections.get(OdyseeCollection.BUILT_IN_ID_FAVORITES);
                    watchlaterPlaylist = allCollections.get(OdyseeCollection.BUILT_IN_ID_WATCHLATER);
                }

                JSONObject shared = result.getJSONObject("shared");
                if (shared.has("type") && "object".equalsIgnoreCase(shared.getString("type")) && shared.has("value")) {
                    JSONObject value = shared.getJSONObject("value");
                    JSONArray tags =  value.has("tags") && !value.isNull("tags") ? value.getJSONArray("tags") : null;
                    JSONArray blocked = value.has("blocked") && !value.isNull("blocked") ? value.getJSONArray("blocked") : null;

                    JSONObject builtInCollections = Helper.getJSONObject("builtinCollections", value);
                    OdyseeCollection favoritesCollection = OdyseeCollection.fromJSONObject(
                            OdyseeCollection.BUILT_IN_ID_FAVORITES,
                            OdyseeCollection.VISIBILITY_PRIVATE,
                            Helper.getJSONObject(OdyseeCollection.BUILT_IN_ID_FAVORITES, builtInCollections));
                    OdyseeCollection watchLaterCollection = OdyseeCollection.fromJSONObject(
                            OdyseeCollection.BUILT_IN_ID_WATCHLATER,
                            OdyseeCollection.VISIBILITY_PRIVATE,
                            Helper.getJSONObject(OdyseeCollection.BUILT_IN_ID_WATCHLATER, builtInCollections));
                    if (activity != null)  {
                        if (favoritesPlaylist == null || favoritesCollection.getUpdatedAtTimestamp() > favoritesPlaylist.getUpdatedAtTimestamp()) {
                            // only replace the locally saved collections if there are items
                            DatabaseHelper.saveCollection(favoritesCollection, db);
                        }
                        android.util.Log.d("#HELP", "LoadUserState: WalletTimestamp=" + watchLaterCollection.getUpdatedAtTimestamp() + "; LocalTimestamp=" + watchlaterPlaylist.getUpdatedAtTimestamp());
                        if (watchlaterPlaylist == null || watchLaterCollection.getUpdatedAtTimestamp() > watchlaterPlaylist.getUpdatedAtTimestamp()) {
                            DatabaseHelper.saveCollection(watchLaterCollection, db);
                        }
                    }

                    subscriptions = loadSubscriptionsFromSharedUserState(shared);
                    if (db != null) {
                        for (Subscription subscription : subscriptions) {
                            try {
                                DatabaseHelper.createOrUpdateSubscription(subscription, db);
                            } catch (IllegalStateException ex) {
                                // pass
                            }
                        }
                    }

                    if (tags != null) {
                        if (db != null && tags.length() > 0) {
                            try {
                                DatabaseHelper.setAllTagsUnfollowed(db);
                            } catch (IllegalStateException ex) {
                                // pass
                            }
                        }

                        followedTags = new ArrayList<>();
                        for (int i = 0; i < tags.length(); i++) {
                            String tagName = tags.getString(i);
                            Tag tag = new Tag(tagName);
                            tag.setFollowed(true);
                            followedTags.add(tag);

                            try {
                                if (db != null) {
                                    DatabaseHelper.createOrUpdateTag(tag, db);
                                }
                            } catch (SQLiteException | IllegalStateException ex) {
                                // pass
                            }
                        }
                    }

                    if (blocked != null) {
                        blockedChannels = new ArrayList<>();
                        if (db != null) {
                            for (int i = 0; i < blocked.length(); i++) {
                                LbryUri uri = LbryUri.tryParse(blocked.getString(i));
                                if (uri != null) {
                                    blockedChannels.add(uri);
                                    DatabaseHelper.createOrUpdateBlockedChannel(uri.getClaimId(), uri.getClaimName(), db);
                                }
                            }
                        }
                    }
                }
            }

            return true;
        } catch (ApiCallException | JSONException ex) {
            // failed
            error = ex;
        }
        return false;
    }

    public static boolean isNotificationsDisabledForSubUrl(String url, JSONArray following) {
        if (following == null) {
            return true;
        }

        try {
            for (int i = 0; i < following.length(); i++) {
                JSONObject item = following.getJSONObject(i);
                String itemUrl = Helper.getJSONString("url", null, item);
                boolean notificationsDisabled = Helper.getJSONBoolean("notificationsDisabled", true, item);
                if (url.equalsIgnoreCase(itemUrl)) {
                    return notificationsDisabled;
                }
            }
        } catch (JSONException ex) {
            // pass
        }

        // always default notifications disabled to true
        return true;
    }

    public static List<Subscription> loadSubscriptionsFromSharedUserState(JSONObject shared) {
        List<Subscription> subscriptions = new ArrayList<>();

        try {
            JSONObject value = shared.getJSONObject("value");
            JSONArray subscriptionUrls =
                    value.has("subscriptions") && !value.isNull("subscriptions") ? value.getJSONArray("subscriptions") : null;
            JSONArray following =
                    value.has("following") && !value.isNull("following") ? value.getJSONArray("following") : null;

            if (subscriptionUrls != null) {
                subscriptions = new ArrayList<>();
                for (int i = 0; i < subscriptionUrls.length(); i++) {
                    String url = subscriptionUrls.getString(i);
                    try {
                        LbryUri uri = LbryUri.parse(LbryUri.normalize(url));
                        Subscription subscription = new Subscription();
                        subscription.setChannelName(uri.getChannelName());
                        subscription.setUrl(uri.toString());
                        subscription.setNotificationsDisabled(isNotificationsDisabledForSubUrl(uri.toString(), following));
                        subscriptions.add(subscription);
                    } catch (LbryUriException | SQLiteException ex) {
                        // pass
                    }
                }
            }
        } catch (JSONException ex) {
            // pass
        }

        return subscriptions;
    }

    protected void onPostExecute(Boolean result) {
        if (handler != null) {
            if (result) {
                handler.onSuccess(subscriptions, followedTags, blockedChannels);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface LoadSharedUserStateHandler {
        void onSuccess(List<Subscription> subscriptions, List<Tag> followedTags, List<LbryUri> blockedChannels);
        void onError(Exception error);
    }
}
