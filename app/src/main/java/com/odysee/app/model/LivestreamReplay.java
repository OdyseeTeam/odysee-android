package com.odysee.app.model;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class LivestreamReplay {
    private String status;
    private String percentComplete;
    @SerializedName("URL")
    private String url;
    @SerializedName("ThumbnailURLs")
    private List<String> thumbnailUrls;
    private long duration;
    private Date created;

    public boolean selected;

    public static LivestreamReplay fromJSONObject(JSONObject replayObject) {
        String replayJson = replayObject.toString();

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

        return gson.fromJson(replayJson, LivestreamReplay.class);
    }
}
