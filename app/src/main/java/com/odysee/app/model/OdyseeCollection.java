package com.odysee.app.model;

import com.odysee.app.utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OdyseeCollection {
    public static final String BUILT_IN_ID_FAVORITES = "favorites";
    public static final String BUILT_IN_ID_WATCHLATER = "watchlater";

    public static final int VISIBILITY_PRIVATE = 1;
    public static final int VISIBILITY_PUBLIC = 2; // published
    public static final int VISIBILITY_UNLISTED = 3; // How about unlisted visibility? Will this be used eventually?

    public static final String TYPE_PLAYLIST = "playlist";

    private String id;
    private String name;
    private String type;
    private List<String> items;
    private Date updatedAt;
    private int visibility;

    public OdyseeCollection() {
        items = new ArrayList<>();
        updatedAt = new Date();
    }

    public void addItem(String url, boolean update) {
        if (!items.contains(url)) {
            items.add(url);
            if (update) {
                updatedAt = new Date();
            }
        }
    }

    public void addItem(String url) {
        addItem(url, true);
    }

    public void removeItem(String url) {
        items.remove(url);
        updatedAt = new Date();
    }

    public long getUpdatedAtTimestamp() {
        return Double.valueOf((double) updatedAt.getTime() / 1000.0).longValue();
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            object.put("items", Helper.jsonArrayFromList(items));
            object.put("name", name);
            object.put("type", type);
            object.put("updatedAt", Double.valueOf((double) updatedAt.getTime() / 1000.0).longValue());
        } catch (JSONException ex) {
            // pass
        }
        return object;
    }

    public static OdyseeCollection fromJSONObject(String id, int visibility, JSONObject jsonObject) {
        long now = Double.valueOf((double) new Date().getTime() / 1000.0).longValue();

        OdyseeCollection collection = new OdyseeCollection();
        collection.setId(id);
        collection.setItems(Helper.getJsonStringArrayAsList("items", jsonObject));
        collection.setName(Helper.getJSONString("name", null, jsonObject));
        collection.setType(Helper.getJSONString("type", null, jsonObject));
        collection.setUpdatedAt(new Date(Helper.getJSONLong("updatedAt", now, jsonObject) * 1000));
        collection.setVisibility(visibility);

        return collection;
    }

    public static OdyseeCollection createPrivatePlaylist(String title) {
        OdyseeCollection collection = new OdyseeCollection();
        collection.setName(title);
        collection.setType(TYPE_PLAYLIST);
        collection.setVisibility(VISIBILITY_PRIVATE);
        return collection;
    }
}
