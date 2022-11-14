package com.odysee.app.callable;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.model.Claim;
import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class SaveSharedUserState implements Callable<Boolean> {
    private static final String KEY = "shared";
    private static final String VERSION = "0.1";
    private final String authToken;
    private final SQLiteDatabase db;
    private final String defaultChannelName;

    public SaveSharedUserState(String authToken, SQLiteDatabase db, @Nullable String defaultChannelName) {
        this.authToken = authToken;
        this.db = db;
        this.defaultChannelName = defaultChannelName;
    }

    @Override
    public Boolean call() throws Exception {
        boolean loadedSubs = false;
        boolean loadedBlocked = false;

        // data to save
        // current subscriptions
        List<Subscription> subs = new ArrayList<>();
        if (db != null) {
            subs = new ArrayList<>(DatabaseHelper.getSubscriptions(db));
            loadedSubs = true;
        }

        List<String> subscriptionUrls = new ArrayList<>();
        for (Subscription subscription : subs) {
            LbryUri uri = LbryUri.parse(LbryUri.normalize(subscription.getUrl()));
            subscriptionUrls.add(uri.toString());
        }

        // followed tags
        List<String> followedTags = Helper.getTagsForTagObjects(Lbry.followedTags);

        // blocked channels
        List<LbryUri> blockedChannels = new ArrayList<>();
        if (db != null) {
            blockedChannels = new ArrayList<>(DatabaseHelper.getBlockedChannels(db));
            loadedBlocked = true;
        }

        List<String> blockedChannelUrls = new ArrayList<>();
        for (LbryUri uri : blockedChannels) {
            blockedChannelUrls.add(uri.toString());
        }

        Map<String, OdyseeCollection> allCollections = null;
        OdyseeCollection favoritesPlaylist = null;
        OdyseeCollection watchlaterPlaylist = null;
        if (db != null) {
            allCollections = DatabaseHelper.loadAllCollections(db);
            // get the built in collections
            favoritesPlaylist = allCollections.get(OdyseeCollection.BUILT_IN_ID_FAVORITES);
            watchlaterPlaylist = allCollections.get(OdyseeCollection.BUILT_IN_ID_WATCHLATER);
        }

        // Get the previous saved state
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

                // handle builtInCollections
                // check favorites last updated at, and compare
                JSONObject builtinCollections = Helper.getJSONObject("builtinCollections", value);
                if (builtinCollections != null)  {
                    if (favoritesPlaylist != null) {
                        JSONObject priorFavorites = Helper.getJSONObject(favoritesPlaylist.getId(), builtinCollections);
                        long priorFavUpdatedAt = Helper.getJSONLong("updatedAt", 0, priorFavorites);
                        if (priorFavUpdatedAt < favoritesPlaylist.getUpdatedAtTimestamp()) {
                            // the current playlist is newer, so we replace
                            builtinCollections.put(favoritesPlaylist.getId(), favoritesPlaylist.toJSONObject());
                        }
                    }

                    if (watchlaterPlaylist != null) {
                        JSONObject priorWatchLater = Helper.getJSONObject(watchlaterPlaylist.getId(), builtinCollections);
                        long priorWatchLaterUpdatedAt = Helper.getJSONLong("updatedAt", 0, priorWatchLater);
                        if (priorWatchLaterUpdatedAt < watchlaterPlaylist.getUpdatedAtTimestamp()) {
                            // the current playlist is newer, so we replace
                            builtinCollections.put(watchlaterPlaylist.getId(), watchlaterPlaylist.toJSONObject());
                        }
                    }
                }

                // handle unpublishedCollections
                JSONObject unpublishedCollections = Helper.getJSONObject("unpublishedCollections", value);
                if (unpublishedCollections != null && allCollections != null) {
                    for (Map.Entry<String, OdyseeCollection> entry : allCollections.entrySet()) {
                        String collectionId = entry.getKey();
                        if (Arrays.asList(OdyseeCollection.BUILT_IN_ID_FAVORITES, OdyseeCollection.BUILT_IN_ID_WATCHLATER).contains(collectionId)) {
                            continue;
                        }

                        OdyseeCollection localCollection = entry.getValue();
                        if (localCollection.getVisibility() != OdyseeCollection.VISIBILITY_PRIVATE) {
                            continue;
                        }

                        JSONObject priorCollection = Helper.getJSONObject(collectionId, unpublishedCollections);
                        if (priorCollection != null) {
                            long priorCollectionUpdatedAt = Helper.getJSONLong("updatedAt", 0, priorCollection);
                            if (priorCollectionUpdatedAt < localCollection.getUpdatedAtTimestamp()) {
                                unpublishedCollections.put(collectionId, localCollection.toJSONObject());
                            }
                        } else {
                            unpublishedCollections.put(collectionId, localCollection.toJSONObject());
                        }
                    }
                }

                JSONObject settings = Helper.getJSONObject("settings", value);
                if (settings != null) {
                    if (defaultChannelName != null) {
                        List<Claim> filteredClaim = Lbry.ownChannels.stream().filter(c -> c.getName().equalsIgnoreCase(defaultChannelName)).collect(Collectors.toList());
                        if (filteredClaim.size() == 1) {
                            settings.put("active_channel_claim", filteredClaim.get(0).getClaimId());
                        }
                    } else {
                        settings.put("active_channel_claim", "");
                    }
                }
                value.put("settings", settings);

                sharedObject = shared;
                sharedObject.put("value", value);
            }
        }

        if (!isExistingValid) {
            // build a  new object
            JSONObject value = new JSONObject();
            value.put("subscriptions", Helper.jsonArrayFromList(subscriptionUrls));
            value.put("tags", Helper.jsonArrayFromList(followedTags));
            value.put("following", buildUpdatedNotificationsDisabledStates(subs));
            value.put("blocked", Helper.jsonArrayFromList(blockedChannelUrls));

            JSONObject builtinCollections = new JSONObject();
            if (favoritesPlaylist != null) {
                builtinCollections.put(favoritesPlaylist.getId(), favoritesPlaylist.toJSONObject());
            }
            if (watchlaterPlaylist != null) {
                builtinCollections.put(watchlaterPlaylist.getId(), watchlaterPlaylist.toJSONObject());
            }
            value.put("builtinCollections", builtinCollections);

            JSONObject unpublishedCollections = new JSONObject();
            if (allCollections != null) {
                for (Map.Entry<String, OdyseeCollection> entry : allCollections.entrySet()) {
                    String collectionId = entry.getKey();
                    if (Arrays.asList(OdyseeCollection.BUILT_IN_ID_FAVORITES, OdyseeCollection.BUILT_IN_ID_WATCHLATER).contains(collectionId)) {
                        continue;
                    }

                    OdyseeCollection localCollection = entry.getValue();
                    if (localCollection.getVisibility() != OdyseeCollection.VISIBILITY_PRIVATE) {
                        continue;
                    }
                    unpublishedCollections.put(collectionId, localCollection.toJSONObject());
                }
            }
            value.put("unpublishedCollections", unpublishedCollections);

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
}
