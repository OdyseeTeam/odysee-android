package com.odysee.app.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class OdyseeCollection {
    public static final String BUILT_IN_ID_FAVORITES = "favorites";
    public static final String BUILT_IN_ID_WATCHLATER = "watchlater";

    private String id;
    private String name;
    private String type;
    private List<String> items;
    private Date lastUpdated;

    public OdyseeCollection() {
        items = new ArrayList<>();
        lastUpdated = new Date();
    }

    public void addItem(String url, boolean update) {
        if (!items.contains(url)) {
            items.add(url);
            if (update) {
                lastUpdated = new Date();
            }
        }
    }

    public void addItem(String url) {
        addItem(url, true);
    }

    public void removeItem(String url) {
        items.remove(url);
        lastUpdated = new Date();
    }
}
