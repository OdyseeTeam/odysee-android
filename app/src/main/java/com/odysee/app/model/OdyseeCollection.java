package com.odysee.app.model;

import com.odysee.app.model.ItemOrderSortable;
import com.odysee.app.utils.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OdyseeCollection {
    public static final String PLACEHOLDER_ID_NEW = "__new";
    public static final String PLACEHOLDER_ID_NOW_PLAYING = "__now_playing";
    public static final String BUILT_IN_ID_FAVORITES = "favorites";
    public static final String BUILT_IN_ID_WATCHLATER = "watchlater";

    public static final int VISIBILITY_PRIVATE = 1;
    public static final int VISIBILITY_PUBLIC = 2; // published
    public static final int VISIBILITY_UNLISTED = 3; // How about unlisted visibility? Will this be used eventually?

    public static final String TYPE_PLAYLIST = "playlist";

    private boolean newPlaceholder;
    private String id;
    private String name;
    private String type;
    private List<Item> items;
    private List<Claim> claims;
    private Date updatedAt;
    private int visibility;

    // published playlist
    private Claim actualClaim;
    private String claimId;
    private String claimName;
    private String permanentUrl;

    public OdyseeCollection() {
        items = new ArrayList<>();
        updatedAt = new Date();
    }

    public void addClaim(Claim claim) {
        if (claims == null) {
            claims = new ArrayList<>();
        }
        claims.add(claim);
    }

    public void addItem(Item item, boolean update) {
        if (!items.contains(item)) {
            items.add(item);
            if (update) {
                updatedAt = new Date();
            }
        }
    }

    public void addItem(String url, int itemOrder, boolean update) {
        Item item = new Item(url, itemOrder);
        addItem(item, update);
    }

    public void addItem(String url, int itemOrder) {
        addItem(url, itemOrder, true);
    }

    public void removeItem(String url) {
        items.removeIf(item -> url.equalsIgnoreCase(item.getUrl()));
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
        collection.setItemsFromStringList(Helper.getJsonStringArrayAsList("items", jsonObject));
        collection.setName(Helper.getJSONString("name", null, jsonObject));
        collection.setType(Helper.getJSONString("type", null, jsonObject));
        collection.setUpdatedAt(new Date(Helper.getJSONLong("updatedAt", now, jsonObject) * 1000));
        collection.setVisibility(visibility);

        return collection;
    }

    public static  OdyseeCollection fromClaim(Claim claim, List<String> items) {
        OdyseeCollection collection = new OdyseeCollection();
        collection.setId(claim.getClaimId());
        collection.setClaimId(claim.getClaimId());
        collection.setClaimName(claim.getName());
        collection.setPermanentUrl(claim.getPermanentUrl());
        collection.setItemsFromStringList(items);
        collection.setName(claim.getTitle());
        collection.setType(OdyseeCollection.TYPE_PLAYLIST);
        collection.setUpdatedAt(new Date(claim.getTimestamp() * 1000));
        collection.setVisibility(OdyseeCollection.VISIBILITY_PUBLIC);  // claims are published, so public
        collection.setActualClaim(claim);

        return collection;
    }

    public void setItemsFromStringList(List<String> itemStringList) {
        List<Item> items = new ArrayList<>(itemStringList.size());
        int itemOrder = 0;
        for (int i = 0; i < itemStringList.size(); i++) {
            String thisItemString = itemStringList.get(i);
            items.add(new Item(thisItemString, ++itemOrder));
        }
        this.items = new ArrayList<>(items);
    }

    public static OdyseeCollection createPrivatePlaylist(String title) {
        OdyseeCollection collection = new OdyseeCollection();
        collection.setName(title);
        collection.setType(TYPE_PLAYLIST);
        collection.setVisibility(VISIBILITY_PRIVATE);
        return collection;
    }

    @Data
    public static class Item implements ItemOrderSortable {
        private String url;
        private int itemOrder;
        public Item() {

        }
        public Item(String url, int itemOrder) {
            this.url = url;
            this.itemOrder = itemOrder;
        }
    }
}
