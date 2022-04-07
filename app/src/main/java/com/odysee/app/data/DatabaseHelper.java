package com.odysee.app.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.odysee.app.model.OdyseeCollection;
import com.odysee.app.model.Tag;
import com.odysee.app.model.UrlSuggestion;
import com.odysee.app.model.ViewHistory;
import com.odysee.app.model.lbryinc.LbryNotification;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.LbryUri;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 11;
    public static final String DATABASE_NAME = "LbryApp.db";
    private static DatabaseHelper instance;

    private static final String[] SQL_CREATE_TABLES = {
            // local subscription store
            "CREATE TABLE subscriptions (url TEXT PRIMARY KEY NOT NULL, channel_name TEXT NOT NULL, is_notifications_disabled INTEGER DEFAULT 0 NOT NULL)",
            // url entry / suggestion history
            "CREATE TABLE url_history (id INTEGER PRIMARY KEY NOT NULL, value TEXT NOT NULL, url TEXT, type INTEGER NOT NULL, timestamp TEXT NOT NULL)",
            // tags (known and followed)
            "CREATE TABLE tags (id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL, is_followed INTEGER NOT NULL)",
            // view history (stores only stream claims that have resolved)
            "CREATE TABLE view_history (" +
                    "  id INTEGER PRIMARY KEY NOT NULL" +
                    ", url TEXT NOT NULL" +
                    ", claim_id TEXT" +
                    ", claim_name TEXT" +
                    ", cost REAL " +
                    ", currency TEXT " +
                    ", title TEXT " +
                    ", publisher_claim_id TEXT" +
                    ", publisher_name TEXT" +
                    ", publisher_title TEXT" +
                    ", thumbnail_url TEXT" +
                    ", release_time INTEGER " +
                    ", device TEXT" +
                    ", timestamp TEXT NOT NULL)",
            "CREATE TABLE notifications (" +
                    "  id INTEGER PRIMARY KEY NOT NULL" +
                    ", remote_id INTEGER NOT NULL" +
                    ", author_thumbnail_url TEXT" +
                    ", title TEXT" +
                    ", description TEXT" +
                    ", claim_thumbnail_url TEXT" +
                    ", target_url TEXT" +
                    ", rule TEXT" +
                    ", is_read INTEGER DEFAULT 0 NOT NULL" +
                    ", is_seen INTEGER DEFAULT 0 NOT NULL " +
                    ", timestamp TEXT NOT NULL)",
            "CREATE TABLE shuffle_watched (id INTEGER PRIMARY KEY NOT NULL, claim_id TEXT NOT NULL)",
            "CREATE TABLE blocked_channels (claim_id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)",
            "CREATE TABLE collections (id TEXT PRIMARY KEY NOT NULL, name TEXT, type TEXT NOT NULL, updated_at TEXT NOT NULL, visibility INTEGER DEFAULT 1 NOT NULL)",
            "CREATE TABLE collection_items (collection_id TEXT NOT NULL, url TEXT NOT NULL, item_order INTEGER DEFAULT 1 NOT NULL, PRIMARY KEY(collection_id, url))"
    };
    private static final String[] SQL_CREATE_INDEXES = {
            "CREATE UNIQUE INDEX idx_subscription_url ON subscriptions (url)",
            "CREATE UNIQUE INDEX idx_url_history_value ON url_history (value)",
            "CREATE UNIQUE INDEX idx_url_history_url ON url_history (url)",
            "CREATE UNIQUE INDEX idx_tag_name ON tags (name)",
            "CREATE UNIQUE INDEX idx_view_history_url_device ON view_history (url, device)",
            "CREATE INDEX idx_view_history_device ON view_history (device)",
            "CREATE UNIQUE INDEX idx_notification_remote_id ON notifications (remote_id)",
            "CREATE INDEX idx_notification_timestamp ON notifications (timestamp)",
            "CREATE UNIQUE INDEX idx_shuffle_watched_claim ON shuffle_watched (claim_id)",
            "CREATE INDEX idx_blocked_channel_name ON blocked_channels (name)"
    };

    private static final String[] SQL_V1_V2_UPGRADE = {
            "ALTER TABLE view_history ADD COLUMN currency TEXT"
    };

    private static final String[] SQL_V2_V3_UPGRADE = {
            "CREATE TABLE notifications (" +
                    "  id INTEGER PRIMARY KEY NOT NULL" +
                    ", title TEXT" +
                    ", description TEXT" +
                    ", thumbnail_url TEXT" +
                    ", target_url TEXT" +
                    ", is_read INTEGER DEFAULT 0 NOT NULL" +
                    ", timestamp TEXT NOT NULL)",
            "CREATE INDEX idx_notification_timestamp ON notifications (timestamp)"
    };

    private static final String[] SQL_V3_V4_UPGRADE = {
            "ALTER TABLE notifications ADD COLUMN remote_id INTEGER",
            "CREATE UNIQUE INDEX idx_notification_remote_id ON notifications (remote_id)"
    };
    private static final String[] SQL_V4_V5_UPGRADE = {
            "ALTER TABLE notifications ADD COLUMN rule TEXT",
            "ALTER TABLE notifications ADD COLUMN is_seen TEXT"
    };
    private static final String[] SQL_V5_V6_UPGRADE = {
            "ALTER TABLE notifications ADD COLUMN author_url TEXT"
    };
    private static final String[] SQL_V6_V7_UPGRADE = {
            "CREATE TABLE shuffle_watched (id INTEGER PRIMARY KEY NOT NULL, claim_id TEXT NOT NULL)",
            "CREATE UNIQUE INDEX idx_shuffle_watched_claim ON shuffle_watched (claim_id)"
    };
    private static final String[] SQL_V7_V8_UPGRADE = {
            "AlTER TABLE subscriptions ADD COLUMN is_notifications_disabled INTEGER DEFAULT 0 NOT NULL"
    };
    private static final String[] SQL_V8_V9_UPGRADE = {
            "ALTER TABLE notifications RENAME TO tmp_notifications",
            "CREATE TABLE notifications (" +
                    "  id INTEGER PRIMARY KEY NOT NULL" +
                    ", remote_id INTEGER NOT NULL" +
                    ", author_thumbnail_url TEXT" +
                    ", title TEXT" +
                    ", description TEXT" +
                    ", claim_thumbnail_url TEXT" +
                    ", target_url TEXT" +
                    ", rule TEXT" +
                    ", is_read INTEGER DEFAULT 0 NOT NULL" +
                    ", is_seen INTEGER DEFAULT 0 NOT NULL " +
                    ", timestamp TEXT NOT NULL)",
            "REPLACE INTO notifications (remote_id, author_thumbnail_url, title, description, rule, target_url, is_read, is_seen, timestamp) SELECT remote_id, author_url, title, description, rule, target_url, is_read, is_seen, timestamp FROM tmp_notifications",
            "DROP TABLE tmp_notifications",
            "CREATE UNIQUE INDEX idx_notification_remote_id ON notifications (remote_id)"
    };
    private static final String[] SQL_V9_V10_UPGRADE = {
            "CREATE TABLE blocked_channels (claim_id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)",
            "CREATE INDEX idx_blocked_channel_name ON blocked_channels (name)"
    };
    private static final String[] SQL_V10_V11_UPGRADE = {
            "CREATE TABLE collections (id TEXT PRIMARY KEY NOT NULL, name TEXT, type TEXT NOT NULL, updated_at TEXT NOT NULL, visibility INTEGER DEFAULT 1 NOT NULL)",
            "CREATE TABLE collection_items (collection_id TEXT NOT NULL, url TEXT NOT NULL, item_order INTEGER DEFAULT 1 NOT NULL, PRIMARY KEY(collection_id, url))"
    };

    private static final String SQL_INSERT_SUBSCRIPTION = "REPLACE INTO subscriptions (channel_name, url, is_notifications_disabled) VALUES (?, ?, ?)";
    private static final String SQL_UPDATE_SUBSCRIPTION_NOTIFICATION = "UPDATE subscriptions SET is_notification_disabled = ? WHERE url = ?";
    private static final String SQL_CLEAR_SUBSCRIPTIONS = "DELETE FROM subscriptions";
    private static final String SQL_DELETE_SUBSCRIPTION = "DELETE FROM subscriptions WHERE url = ?";
    private static final String SQL_GET_SUBSCRIPTIONS = "SELECT channel_name, url, is_notifications_disabled FROM subscriptions";

    private static final String SQL_INSERT_URL_HISTORY = "REPLACE INTO url_history (value, url, type, timestamp) VALUES (?, ?, ?, ?)";
    private static final String SQL_CLEAR_URL_HISTORY = "DELETE FROM url_history";
    private static final String SQL_CLEAR_URL_HISTORY_BEFORE_TIME = "DELETE FROM url_history WHERE timestamp < ?";
    private static final String SQL_GET_RECENT_URL_HISTORY = "SELECT value, url, type FROM url_history ORDER BY timestamp DESC LIMIT 10";

    private static final String SQL_INSERT_NOTIFICATION = "REPLACE INTO notifications (remote_id, author_thumbnail_url, claim_thumbnail_url, title, description, rule, target_url, is_read, is_seen, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_GET_NOTIFICATIONS = "SELECT id, remote_id, author_thumbnail_url, claim_thumbnail_url, title, description, rule, target_url, is_read, is_seen, timestamp FROM notifications ORDER BY timestamp DESC LIMIT 500";
    private static final String SQL_GET_UNREAD_NOTIFICATIONS_COUNT = "SELECT COUNT(id) FROM notifications WHERE is_read <> 1";
    private static final String SQL_GET_UNSEEN_NOTIFICATIONS_COUNT = "SELECT COUNT(id) FROM notifications WHERE is_seen <> 1";
    private static final String SQL_MARK_NOTIFICATIONS_READ = "UPDATE notifications SET is_read = 1 WHERE is_read = 0";
    private static final String SQL_MARK_NOTIFICATIONS_SEEN = "UPDATE notifications SET is_seen = 1 WHERE is_seen = 0";
    private static final String SQL_MARK_NOTIFICATION_READ_AND_SEEN = "UPDATE notifications SET is_read = 1, is_seen = 1 WHERE id = ?";
    private static final String SQL_CLEAR_NOTIFICATIONS = "DELETE FROM notifications";

    private static final String SQL_INSERT_SHUFFLE_WATCHED = "REPLACE INTO shuffle_watched (claim_id) VALUES (?)";
    private static final String SQL_GET_SHUFFLE_WATCHED_CLAIMS = "SELECT claim_id FROM shuffle_watched";

    private static final String SQL_INSERT_VIEW_HISTORY =
            "REPLACE INTO view_history (url, claim_id, claim_name, cost, currency, title, publisher_claim_id, publisher_name, publisher_title, thumbnail_url, device, release_time, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_GET_VIEW_HISTORY =
            "SELECT url, claim_id, claim_name, cost, currency, title, publisher_claim_id, publisher_name, publisher_title, thumbnail_url, device, release_time, timestamp " +
            "FROM view_history WHERE '' = ? OR timestamp < ? ORDER BY timestamp DESC LIMIT %d";
    private static final String SQL_CLEAR_VIEW_HISTORY = "DELETE FROM view_history";
    private static final String SQL_CLEAR_VIEW_HISTORY_BY_DEVICE = "DELETE FROM view_history WHERE device = ?";
    private static final String SQL_CLEAR_VIEW_HISTORY_BEFORE_TIME = "DELETE FROM view_history WHERE timestamp < ?";
    private static final String SQL_CLEAR_VIEW_HISTORY_BY_DEVICE_BEFORE_TIME = "DELETE FROM view_history WHERE device = ? AND timestamp < ?";
    private static final String SQL_DELETE_VIEW_HISTORY = "DELETE FROM view_history WHERE url = ?";

    private static final String SQL_INSERT_TAG = "REPLACE INTO tags (name, is_followed) VALUES (?, ?)";
    private static final String SQL_GET_KNOWN_TAGS = "SELECT name, is_followed FROM tags";
    private static final String SQL_UNFOLLOW_TAGS = "UPDATE tags SET is_followed = 0";
    private static final String SQL_GET_FOLLOWED_TAGS = "SELECT name FROM tags WHERE is_followed = 1";

    private static final String SQL_INSERT_BLOCKED_CHANNEL = "REPLACE INTO blocked_channels (claim_id, name) VALUES (?, ?)";
    private static final String SQL_REMOVE_BLOCKED_CHANNEL = "DELETE FROM blocked_channels WHERE claim_id = ?";
    private static final String SQL_REMOVE_ALL_BLOCKED_CHANNELS = "DELETE FROM blocked_channels";
    private static final String SQL_GET_BLOCKED_CHANNELS = "SELECT claim_id, name FROM blocked_channels";

    private static final String SQL_REMOVE_ALL_SUBSCRIPTIONS = "DELETE FROM subscriptions";
    private static final String SQL_REMOVE_ALL_URL_HISTORY = "DELETE FROM url_history";
    private static final String SQL_REMOVE_ALL_VIEW_HISTORY = "DELETE FROM view_history";
    private static final String SQL_REMOVE_ALL_TAGS = "DELETE FROM tags";
    private static final String SQL_REMOVE_ALL_NOTIFICATIONS = "DELETE FROM notifications";
    private static final String SQL_REMOVE_ALL_CUSTOM_COLLECTIONS = "DELETE FROM collections WHERE id <> 'favorites' AND  id <> 'watchlater'";
    private static final String SQL_REMOVE_ALL_COLLECTION_ITEMS = "DELETE FROM collection_items";

    private static final String SQL_GET_BUILTIN_COLLECTION_COUNT = "SELECT COUNT(id) FROM collections WHERE id = 'favorites' OR id = 'watchlater'";
    private static final String SQL_CREATE_BUILTIN_COLLECTION = "REPLACE INTO collections (id, name, type, visibility, updated_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_CREATE_COLLECTION = "REPLACE INTO collections (id, name, type, visibility, updated_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_REMOVE_COLLECTION_ITEMS_FOR_COLLECTION = "DELETE FROM collection_items WHERE collection_id = ?";
    private static final String SQL_UPDATE_COLLECTION_UPDATED_AT = "UPDATE collections SET updated_at = ? WHERE id = ?";
    private static final String SQL_INSERT_COLLECTION_ITEM_FOR_COLLECTION = "INSERT INTO collection_items (collection_id, url, item_order) VALUES  (?, ?, ?)";
    private static final String SQL_GET_EXISTING_COLLECTION_ITEM_COUNT = "SELECT COUNT(url) FROM collection_items WHERE collection_id = ? AND url = ?";
    private static final String SQL_GET_MAX_ITEM_ORDER_FOR_COLLECTION = "SELECT MAX(item_order) FROM collection_items WHERE collection_id = ?";
    private static final String SQL_GET_COLLECTION_BY_ID = "SELECT id, name, type, visibility, updated_at FROM collections WHERE id = ?";
    private static final String SQL_GET_COLLECTIONS = "SELECT id, name, type, visibility, updated_at FROM collections";
    private static final String SQL_GET_COLLECTION_ITEMS_FOR_COLLECTION = "SELECT url FROM collection_items WHERE collection_id = ? ORDER BY item_order ASC";

    public DatabaseHelper(Context context) {
        super(context, String.format("%s/%s", context.getFilesDir().getAbsolutePath(), DATABASE_NAME), null, DATABASE_VERSION);
        instance = this;
    }
    public static DatabaseHelper getInstance() {
        return instance;
    }
    public void onCreate(SQLiteDatabase db) {
        for (String sql : SQL_CREATE_TABLES) {
            db.execSQL(sql);
        }
        for (String sql : SQL_CREATE_INDEXES) {
            db.execSQL(sql);
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            for (String sql : SQL_V1_V2_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 3) {
            for (String sql : SQL_V2_V3_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 4) {
            for (String sql : SQL_V3_V4_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 5) {
            for (String sql : SQL_V4_V5_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 6) {
            for (String sql : SQL_V5_V6_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 7) {
            for (String sql : SQL_V6_V7_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 8) {
            for (String sql : SQL_V7_V8_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 9) {
            for (String sql : SQL_V8_V9_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 10) {
            for (String sql : SQL_V9_V10_UPGRADE) {
                db.execSQL(sql);
            }
        }
        if (oldVersion < 11) {
            for (String sql : SQL_V10_V11_UPGRADE) {
                db.execSQL(sql);
            }
        }
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static void createOrUpdateUrlHistoryItem(String text, String url, int type, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_URL_HISTORY, new Object[] {
                text, url, type, new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(new Date())
        });
    }
    public static void clearUrlHistory(SQLiteDatabase db) {
        db.execSQL(SQL_CLEAR_URL_HISTORY);
    }
    public static void clearUrlHistoryBefore(Date date, SQLiteDatabase db) {
        db.execSQL(SQL_CLEAR_URL_HISTORY_BEFORE_TIME, new Object[] { new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(new Date()) });
    }
    public static void clearViewHistory(SQLiteDatabase db) {
        db.execSQL(SQL_CLEAR_VIEW_HISTORY);
    }

    // History items are essentially url suggestions
    public static List<UrlSuggestion> getRecentHistory(SQLiteDatabase db) {
        List<UrlSuggestion> suggestions = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_RECENT_URL_HISTORY, null);
            while (cursor.moveToNext()) {
                UrlSuggestion suggestion = new UrlSuggestion();
                suggestion.setText(cursor.getString(0));
                suggestion.setUri(cursor.isNull(1) ? null : LbryUri.tryParse(cursor.getString(1)));
                suggestion.setType(cursor.getInt(2));
                suggestion.setTitleUrlOnly(true);
                suggestions.add(suggestion);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return suggestions;
    }

    // View history items are stream claims
    public static void createOrUpdateViewHistoryItem(ViewHistory viewHistory, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_VIEW_HISTORY, new Object[] {
                viewHistory.getUri().toString(),
                viewHistory.getClaimId(),
                viewHistory.getClaimName(),
                viewHistory.getCost() != null ? viewHistory.getCost().doubleValue() : 0,
                viewHistory.getCurrency(),
                viewHistory.getTitle(),
                viewHistory.getPublisherClaimId(),
                viewHistory.getPublisherName(),
                viewHistory.getPublisherTitle(),
                viewHistory.getThumbnailUrl(),
                viewHistory.getDevice(),
                viewHistory.getReleaseTime(),
                new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(new Date())
        });
    }

    public static List<ViewHistory> getViewHistory(String lastTimestamp, int pageLimit, SQLiteDatabase db) {
        List<ViewHistory> history = new ArrayList<>();
        Cursor cursor = null;
        try {
            String arg = lastTimestamp == null ? "" : lastTimestamp;
            cursor = db.rawQuery(String.format(SQL_GET_VIEW_HISTORY, pageLimit), new String[] { arg, arg });
            while (cursor.moveToNext()) {
                ViewHistory item = new ViewHistory();
                int cursorIndex = 0;
                item.setUri(LbryUri.tryParse(cursor.getString(cursorIndex++)));
                item.setClaimId(cursor.getString(cursorIndex++));
                item.setClaimName(cursor.getString(cursorIndex++));
                item.setCost(new BigDecimal(cursor.getDouble(cursorIndex++)));
                item.setCurrency(cursor.getString(cursorIndex++));
                item.setTitle(cursor.getString(cursorIndex++));
                item.setPublisherClaimId(cursor.getString(cursorIndex++));
                item.setPublisherName(cursor.getString(cursorIndex++));
                item.setPublisherTitle(cursor.getString(cursorIndex++));
                item.setThumbnailUrl(cursor.getString(cursorIndex++));
                item.setDevice(cursor.getString(cursorIndex++));
                item.setReleaseTime(cursor.getLong(cursorIndex++));
                try {
                    item.setTimestamp(new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).parse(cursor.getString(cursorIndex)));
                } catch (ParseException ex) {
                    // invalid timestamp (which shouldn't happen). Skip this item
                    continue;
                }

                history.add(item);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return history;
    }

    /**
     * Removes a collection of items from the viewed claims history list
     * @param db
     * @param url URL from item to be removed
     */
    public static void removeViewHistoryItem(SQLiteDatabase db, String url) {
        db.execSQL(SQL_DELETE_VIEW_HISTORY, new String[]{ url });
    }
    public static void createOrUpdateTag(Tag tag, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_TAG, new Object[] { tag.getLowercaseName(), tag.isFollowed() ? 1 : 0 });
    }
    public static void setAllTagsUnfollowed(SQLiteDatabase db) {
        db.execSQL(SQL_UNFOLLOW_TAGS);
    }
    public static List<Tag> getTags(SQLiteDatabase db) {
        List<Tag> tags = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_KNOWN_TAGS, null);
            while (cursor.moveToNext()) {
                Tag tag = new Tag();
                tag.setName(cursor.getString(0));
                tag.setFollowed(cursor.getInt(1) == 1);
                tags.add(tag);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return tags;
    }

    public static void createOrUpdateSubscription(Subscription subscription, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_SUBSCRIPTION, new Object[] {
                subscription.getChannelName(),
                subscription.getUrl(),
                subscription.isNotificationsDisabled() ? 1 : 0
        });
    }
    public static void setSubscriptionNotificationDisabled(boolean flag, String url, SQLiteDatabase db) {
        db.execSQL(SQL_UPDATE_SUBSCRIPTION_NOTIFICATION, new Object[] { flag ? 1 : 0, url });
    }
    public static void deleteSubscription(Subscription subscription, SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_SUBSCRIPTION, new Object[] { subscription.getUrl() });
    }
    public static void clearSubscriptions(SQLiteDatabase db) {
        db.execSQL(SQL_CLEAR_SUBSCRIPTIONS);
    }
    public static List<Subscription> getSubscriptions(SQLiteDatabase db) {
        List<Subscription> subscriptions = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_SUBSCRIPTIONS, null);
            while (cursor.moveToNext()) {
                Subscription subscription = new Subscription();
                subscription.setChannelName(cursor.getString(0));
                subscription.setUrl(cursor.getString(1));
                subscription.setNotificationsDisabled(cursor.getInt(2) == 1);
                subscriptions.add(subscription);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return subscriptions;
    }

    public static void createOrUpdateNotification(LbryNotification notification, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_NOTIFICATION, new Object[] {
                notification.getRemoteId(),
                notification.getAuthorThumbnailUrl(),
                notification.getClaimThumbnailUrl(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getRule(),
                notification.getTargetUrl(),
                notification.isRead() ? 1 : 0,
                notification.isSeen() ? 1 : 0,
                new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(notification.getTimestamp() != null ? notification.getTimestamp() : new Date())
        });
    }
    public static List<LbryNotification> getNotifications(SQLiteDatabase db) {
        List<LbryNotification> notifications = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_NOTIFICATIONS, null);
            while (cursor.moveToNext()) {
                LbryNotification notification = new LbryNotification();
                int columnIndex = 0;
                notification.setId(cursor.getLong(columnIndex++));
                notification.setRemoteId(cursor.getLong(columnIndex++));
                notification.setAuthorThumbnailUrl(cursor.getString(columnIndex++));
                notification.setClaimThumbnailUrl(cursor.getString(columnIndex++));
                notification.setTitle(cursor.getString(columnIndex++));
                notification.setDescription(cursor.getString(columnIndex++));
                notification.setRule(cursor.getString(columnIndex++));
                notification.setTargetUrl(cursor.getString(columnIndex++));
                notification.setRead(cursor.getInt(columnIndex++) == 1);
                notification.setSeen(cursor.getInt(columnIndex++) == 1);
                try {
                    notification.setTimestamp(new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).parse(cursor.getString(columnIndex++)));
                } catch (ParseException ex) {
                    // invalid timestamp (which shouldn't happen). Skip this item
                    continue;
                }
                notifications.add(notification);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return notifications;
    }
    public static void deleteNotifications(List<LbryNotification> notifications, SQLiteDatabase db) {
        StringBuilder sb = new StringBuilder("DELETE FROM notifications WHERE remote_id IN (");
        List<Object> remoteIds = new ArrayList<>();
        String delim = "";
        for (int i = 0; i < notifications.size(); i++) {
            remoteIds.add(String.valueOf(notifications.get(i).getRemoteId()));
            sb.append(delim).append("?");
            delim = ",";
        }
        sb.append(")");

        String sql = sb.toString();
        db.execSQL(sql, remoteIds.toArray());
    }
    public static int getUnreadNotificationsCount(SQLiteDatabase db) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_UNREAD_NOTIFICATIONS_COUNT, null);
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return count;
    }
    public static int getUnseenNotificationsCount(SQLiteDatabase db) {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_UNSEEN_NOTIFICATIONS_COUNT, null);
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return count;
    }
    public static void markNotificationsSeen(SQLiteDatabase db) {
        db.execSQL(SQL_MARK_NOTIFICATIONS_SEEN);
    }
    public static void markNotificationsRead(SQLiteDatabase db) {
        db.execSQL(SQL_MARK_NOTIFICATIONS_READ);
    }
    public static void markNotificationReadAndSeen(long notificationId, SQLiteDatabase db) {
        db.execSQL(SQL_MARK_NOTIFICATION_READ_AND_SEEN, new Object[] { notificationId });
    }
    public static void clearNotifications(SQLiteDatabase db) {
        db.execSQL(SQL_CLEAR_NOTIFICATIONS);
    }
    public static void createOrUpdateShuffleWatched(String claimId, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_SHUFFLE_WATCHED, new Object[] { claimId });
    }
    public static List<String> getShuffleWatchedClaims(SQLiteDatabase db) {
        List<String> claimIds = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_SHUFFLE_WATCHED_CLAIMS, null);
            while (cursor.moveToNext()) {
                claimIds.add(cursor.getString(0));
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return claimIds;
    }

    public static void createOrUpdateBlockedChannel(String claimId, String channelName, SQLiteDatabase db) {
        db.execSQL(SQL_INSERT_BLOCKED_CHANNEL, new Object[] { claimId, channelName });
    }

    public static void removeBlockedChannel(String claimId, SQLiteDatabase db) {
        db.execSQL(SQL_REMOVE_BLOCKED_CHANNEL, new Object[] { claimId });
    }

    public static void removeAllBlockedChannels(SQLiteDatabase db) {
        db.execSQL(SQL_REMOVE_ALL_BLOCKED_CHANNELS);
    }

    public static List<LbryUri> getBlockedChannels(SQLiteDatabase db) {
        List<LbryUri> blockedChannels = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_BLOCKED_CHANNELS, null);
            while (cursor.moveToNext()) {
                LbryUri uri = LbryUri.tryParse(String.format("lbry://%s:%s", cursor.getString(1), cursor.getString(0)));
                if (uri != null) {
                    blockedChannels.add(uri);
                }
            }
        } finally {
            Helper.closeCursor(cursor);
        }
        return blockedChannels;
    }

    @SuppressLint("SimpleDateFormat")
    public static void checkAndCreateBuiltinPlaylists(SQLiteDatabase db) {
        int count = 0;
        Cursor cursor = null;

        db.beginTransaction();
        try {
            cursor = db.rawQuery(SQL_GET_BUILTIN_COLLECTION_COUNT, null);
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
            if (count != 2) {
                Date now = new Date();
                db.execSQL(SQL_CREATE_BUILTIN_COLLECTION, new Object[]{
                        OdyseeCollection.BUILT_IN_ID_FAVORITES,
                        "Favorites",
                        "playlist",
                        OdyseeCollection.VISIBILITY_PRIVATE,
                        new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(now)
                });
                db.execSQL(SQL_CREATE_BUILTIN_COLLECTION, new Object[]{
                        OdyseeCollection.BUILT_IN_ID_WATCHLATER,
                        "Watch Later",
                        "playlist",
                        OdyseeCollection.VISIBILITY_PRIVATE,
                        new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(now)
                });
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            Helper.closeCursor(cursor);
        }
    }

    public static void saveCollection(OdyseeCollection collection, SQLiteDatabase db) {
        db.beginTransaction();
        try {
            if (collection.getId() == null) {
                collection.setId(UUID.randomUUID().toString());
            }

            db.execSQL(SQL_CREATE_COLLECTION, new Object[] {
                    collection.getId(),
                    collection.getName(),
                    collection.getType(),
                    collection.getVisibility(),
                    new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(collection.getUpdatedAt())
            });

            db.execSQL(SQL_REMOVE_COLLECTION_ITEMS_FOR_COLLECTION, new Object[] { collection.getId() });

            List<String> items = new ArrayList<>(collection.getItems());
            for (int i = 0; i < items.size(); i++)  {
                db.execSQL(SQL_INSERT_COLLECTION_ITEM_FOR_COLLECTION, new Object[] {
                        collection.getId(),
                        items.get(i),
                        (i + 1)  //  item order
                });
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // only load the collection details without the items (for listing)
    public static List<OdyseeCollection> getSimpleCollections(SQLiteDatabase db) {
        List<OdyseeCollection> collections = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_COLLECTIONS, null);
            while (cursor.moveToNext()) {
                int columnIndex = 0;
                OdyseeCollection collection = new OdyseeCollection();
                collection.setId(cursor.getString(columnIndex++));
                collection.setName(cursor.getString(columnIndex++));
                collection.setType(cursor.getString(columnIndex++));
                collection.setVisibility(cursor.getInt(columnIndex++));
                try {
                    collection.setUpdatedAt(new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).parse(cursor.getString(columnIndex++)));
                } catch (ParseException ex)  {
                    collection.setUpdatedAt(new Date());
                }
                collections.add(collection);
            }
        } finally {
            Helper.closeCursor(cursor);
        }

        return collections;
    }

    public static Map<String, OdyseeCollection> loadAllCollections(SQLiteDatabase db) {
        Map<String, OdyseeCollection> collections = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_COLLECTIONS, null);
            while (cursor.moveToNext()) {
                int columnIndex = 0;
                OdyseeCollection collection = new OdyseeCollection();
                collection.setId(cursor.getString(columnIndex++));
                collection.setName(cursor.getString(columnIndex++));
                collection.setType(cursor.getString(columnIndex++));
                collection.setVisibility(cursor.getInt(columnIndex++));
                try {
                    collection.setUpdatedAt(new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).parse(cursor.getString(columnIndex++)));
                } catch (ParseException ex)  {
                    collection.setUpdatedAt(new Date());
                }
                collections.put(collection.getId(), collection);
            }
            Helper.closeCursor(cursor);

            for (Map.Entry<String, OdyseeCollection> entry : collections.entrySet()) {
                OdyseeCollection collection = entry.getValue();
                cursor = db.rawQuery(SQL_GET_COLLECTION_ITEMS_FOR_COLLECTION, new String[] { collection.getId() });
                while (cursor.moveToNext()) {
                    collection.addItem(cursor.getString(0), false);
                }
                Helper.closeCursor(cursor);
            }
        } finally {
            Helper.closeCursor(cursor);
        }

        return collections;
    }

    public static OdyseeCollection loadCollection(String id, SQLiteDatabase db) {
        OdyseeCollection collection = null;

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_GET_COLLECTION_BY_ID, new String[] { id });
            if (cursor.moveToNext()) {
                int columnIndex = 0;
                collection = new OdyseeCollection();
                collection.setId(cursor.getString(columnIndex++));
                collection.setName(cursor.getString(columnIndex++));
                collection.setType(cursor.getString(columnIndex++));
                collection.setVisibility(cursor.getInt(columnIndex++));
                try {
                    collection.setUpdatedAt(new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).parse(cursor.getString(columnIndex++)));
                } catch (ParseException ex)  {
                    collection.setUpdatedAt(new Date());
                }
            }
            Helper.closeCursor(cursor);

            if (collection != null) {
                cursor = db.rawQuery(SQL_GET_COLLECTION_ITEMS_FOR_COLLECTION, new String[] { collection.getId() });
                while (cursor.moveToNext()) {
                    collection.addItem(cursor.getString(0), false);
                }
            }
        } finally {
            Helper.closeCursor(cursor);
        }

        return collection;
    }

    @SuppressLint("SimpleDateFormat")
    public static void addCollectionItem(String id, String url, SQLiteDatabase db) {
        db.beginTransaction();
        Cursor cursor = null;
        try {
            // check if the item exists first. If it does, we can skip everything else
            boolean exists = false;
            cursor = db.rawQuery(SQL_GET_EXISTING_COLLECTION_ITEM_COUNT, new String[] { id, url });
            if (cursor.moveToNext()) {
                exists = cursor.getInt(0) > 0;
            }
            Helper.closeCursor(cursor);

            if (!exists) {
                int nextItemOrder = 1;
                cursor = db.rawQuery(SQL_GET_MAX_ITEM_ORDER_FOR_COLLECTION, new String[]{id});
                if (cursor.moveToNext()) {
                    nextItemOrder = cursor.getInt(0) + 1;
                }

                db.execSQL(SQL_INSERT_COLLECTION_ITEM_FOR_COLLECTION, new Object[]{
                        id, url, nextItemOrder
                });
                db.execSQL(SQL_UPDATE_COLLECTION_UPDATED_AT, new Object[] {
                        new SimpleDateFormat(Helper.ISO_DATE_FORMAT_PATTERN).format(new Date()), id
                });
            }
            db.setTransactionSuccessful();
        } finally {
            Helper.closeCursor(cursor);
            db.endTransaction();
        }
    }

    public static void clearLocalUserData(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(SQL_REMOVE_ALL_SUBSCRIPTIONS);
            db.execSQL(SQL_REMOVE_ALL_URL_HISTORY);
            db.execSQL(SQL_REMOVE_ALL_VIEW_HISTORY);
            db.execSQL(SQL_REMOVE_ALL_TAGS);
            db.execSQL(SQL_REMOVE_ALL_NOTIFICATIONS);
            db.execSQL(SQL_REMOVE_ALL_BLOCKED_CHANNELS);
            db.execSQL(SQL_REMOVE_ALL_COLLECTION_ITEMS);
            db.execSQL(SQL_REMOVE_ALL_CUSTOM_COLLECTIONS);
            db.setTransactionSuccessful();
        } catch (SQLiteException ex) {
            // pass
        } finally {
            db.endTransaction();
        }
    }
}
