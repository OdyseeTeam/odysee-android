package com.odysee.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import lombok.Data;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class ContentSources {
    private static final String TAG = "OdyseeContent";

    private static final String LANG_CODE_EN = "en";
    private static final String REGION_CODE_BR = "BR"; // special check for pt-BR

    private static final String CACHE_KEY = "ContentSourcesCache";
    private static final String ENDPOINT = "https://odysee.com/$/api/content/v1/get";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 60 * 60 * 24 * 1000;

    public static List<Category> DYNAMIC_CONTENT_CATEGORIES = new ArrayList<>();

    @Data
    public static class Category {
        private int sortOrder;
        private String key;
        private String name;
        private String label;
        private String[] channelIds;
        public boolean isValid() {
            return !Helper.isNullOrEmpty(name) && channelIds != null && channelIds.length > 0;
        }
    }

    @Data
    public static class ContentSourceCache {
         private List<Category> categories;
         private Date lastUpdated;
         public ContentSourceCache() {
             categories = new ArrayList<>();
             lastUpdated = new Date();
         }
    }

    public interface CategoriesLoadedHandler {
        void onCategoriesLoaded(List<Category> categories);
    }

    public static void loadCategories(ExecutorService executorService, final CategoriesLoadedHandler handler, final Context context) {
         executorService.execute(new Runnable() {
             @Override
             public void run() {
                 if (context == null) {
                     return;
                 }

                 SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                 String cachedJsonString = sp.getString(CACHE_KEY, null);

                 if (!Helper.isNullOrEmpty(cachedJsonString)) {
                     Gson gson = new Gson();
                     Type type = new TypeToken<ContentSourceCache>(){}.getType();
                     try {
                         ContentSourceCache cache = gson.fromJson(cachedJsonString, type);
                         Date now = new Date();
                         if (now.getTime() - cache.getLastUpdated().getTime() < TWENTY_FOUR_HOURS_MILLIS) {
                             if (handler != null) {
                                 Log.d(TAG, "Loaded local categories");
                                 DYNAMIC_CONTENT_CATEGORIES = new ArrayList<>(cache.getCategories());
                                 handler.onCategoriesLoaded(cache.getCategories());
                             }
                             return;
                         }
                     } catch (JsonSyntaxException ex) {
                         ex.printStackTrace();
                     }
                 }

                 // if local categories were not loaded, go remote
                 loadRemoteCategories(executorService, handler, context);
             }
         });
    }

    public static void loadRemoteCategories(ExecutorService executorService, final CategoriesLoadedHandler handler, final Context context) {
        Log.d(TAG, "Attempting to load remote categories");

        executorService.execute(new Runnable() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void run() {
                List<Category> loadedCategories = new ArrayList<>();

                Request request = new Request.Builder().url(ENDPOINT).build();
                OkHttpClient client = new OkHttpClient.Builder().build();
                ResponseBody responseBody = null;
                try {
                    Response response = client.newCall(request).execute();
                    responseBody = response.body();

                    if (responseBody != null) {
                        String jsonString = responseBody.string();
                        JSONObject json = new JSONObject(jsonString);

                        if (!json.isNull("data") && !json.getJSONObject("data").toString().equals("{}")) {
                            JSONObject data = json.getJSONObject("data");
                            loadedCategories = processJSONdata(data);
                        } else {
                            // Load default categories from APK assets
                            try {
                                String string;
                                InputStream inputStream = context.getAssets().open("default_categories.json");
                                int size = inputStream.available();
                                byte[] buffer = new byte[size];
                                //noinspection ResultOfMethodCallIgnored
                                inputStream.read(buffer);
                                string = new String(buffer);

                                loadedCategories = processJSONdata(new JSONObject(string));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException | JSONException ex) {
                    if (handler != null) {
                        // if categories list is empty, that means nothing could be loaded
                        handler.onCategoriesLoaded(new ArrayList<>());
                    }
                    return;
                } finally {
                    if (responseBody != null) {
                        responseBody.close();
                    }
                }

                if (loadedCategories.size() == 0 && handler != null) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    String cachedJsonString = sp.getString(CACHE_KEY, null);

                    if (!Helper.isNullOrEmpty(cachedJsonString)) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<ContentSourceCache>(){}.getType();
                        try {
                            ContentSourceCache cache = gson.fromJson(cachedJsonString, type);
                            Log.d(TAG, "Using cached categories");
                            DYNAMIC_CONTENT_CATEGORIES = new ArrayList<>(cache.getCategories());
                            handler.onCategoriesLoaded(cache.getCategories());
                        } catch (JsonSyntaxException exc) {
                            exc.printStackTrace();
                        }
                        return;
                    }
                }

                Collections.sort(loadedCategories, new Comparator<Category>() {
                    @Override
                    public int compare(Category o1, Category o2) {
                        return o1.sortOrder - o2.sortOrder;
                    }
                });

                // Cache the loaded remote categories (only if anything was loaded)
                if (loadedCategories.size() > 0) {
                    DYNAMIC_CONTENT_CATEGORIES = new ArrayList<>(loadedCategories);

                    ContentSourceCache cache = new ContentSourceCache();
                    cache.setCategories(new ArrayList<>(loadedCategories));
                    cache.setLastUpdated(new Date());

                    if (context != null) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<ContentSourceCache>(){}.getType();
                        String jsonString = gson.toJson(cache, type);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                        sp.edit().putString(CACHE_KEY, jsonString).commit();
                    }
                }

                if (handler != null) {
                    handler.onCategoriesLoaded(loadedCategories);
                }
            }

            private List<Category> processJSONdata(JSONObject data) {
                List<Category> lc = new ArrayList<>();
                String langCode = Locale.getDefault().getLanguage();
                String regionCode = Locale.getDefault().getCountry();
                Log.d(TAG, "LangCode=" + langCode + ";RegionCode=" + regionCode);
                String langKey = langCode;
                if (!LANG_CODE_EN.equals(langCode) && REGION_CODE_BR.equals(regionCode)) {
                    langKey = String.format("%s-%s", langCode, regionCode);
                }

                JSONObject langData = data.has(langKey) ? Helper.getJSONObject(langKey, data) : Helper.getJSONObject(LANG_CODE_EN, data);
                if (langData != null) {
                    Iterator<String> keys = langData.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject srcJson = Helper.getJSONObject(key, langData);
                        if (srcJson != null) {
                            Category category = new Category();
                            category.setKey(key);
                            category.setSortOrder(Helper.getJSONInt("sortOrder", 1, srcJson));
                            category.setName(Helper.getJSONString("name", null, srcJson));
                            category.setLabel(Helper.getJSONString("label", null, srcJson));

                            List<String> channelIdList = Helper.getJsonStringArrayAsList("channelIds", srcJson);
                            category.setChannelIds(channelIdList.toArray(new String[channelIdList.size()]));

                            if (category.isValid()) {
                                lc.add(category);
                            }
                        }
                    }
                }
                return lc;
            }
        });
    }
}
