package com.odysee.app.supplier;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.LbryNotification;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NotificationListSupplier implements Supplier<List<LbryNotification>> {
    private final Map<String, String> options;

    public NotificationListSupplier(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public List<LbryNotification> get() {
        List<LbryNotification> notifications = new ArrayList<>();
        try {
            JSONArray array = (JSONArray) Lbryio.parseResponse(Lbryio.call("notification", "list", options, null));
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    if (item.has("notification_parameters")) {
                        LbryNotification notification = new LbryNotification();

                        JSONObject notificationParams = item.getJSONObject("notification_parameters");
                        if (notificationParams.has("device")) {
                            JSONObject device = notificationParams.getJSONObject("device");
                            notification.setTitle(Helper.getJSONString("title", null, device));
                            notification.setDescription(Helper.getJSONString("text", null, device));
                            notification.setTargetUrl(Helper.getJSONString("target", null, device));
                        }
                        if (notificationParams.has("dynamic") && !notificationParams.isNull("dynamic")) {
                            JSONObject dynamic = notificationParams.getJSONObject("dynamic");
                            if (dynamic.has("comment_author") || dynamic.has("channel_thumbnail")) {
                                String url = null;
                                if (dynamic.has("comment_author")) {
                                    url = Helper.getJSONString("comment_author", null, dynamic);
                                } else if (dynamic.has("channel_thumbnail")) {
                                    url = Helper.getJSONString("channel_thumbnail", null, dynamic);
                                }
                                notification.setAuthorThumbnailUrl(url);
                            }
                            if (dynamic.has("channelURI")) {
                                String channelUrl = Helper.getJSONString("channelURI", null, dynamic);
                                if (!Helper.isNullOrEmpty(channelUrl)) {
                                    notification.setTargetUrl(channelUrl);
                                }
                            }
                            if (dynamic.has("claim_thumbnail")) {
                                String claimThumbnailUrl = Helper.getJSONString("claim_thumbnail", null, dynamic);
                                if (!Helper.isNullOrEmpty(claimThumbnailUrl)) {
                                    notification.setClaimThumbnailUrl(claimThumbnailUrl);
                                }
                            }
                            if (dynamic.has("hash") && isCommentNotification(item)) {
                                notification.setTargetUrl(String.format("%s?comment_hash=%s", notification.getTargetUrl(), dynamic.getString("hash")));
                            }
                        }

                        notification.setRule(Helper.getJSONString("notification_rule", null, item));
                        notification.setRemoteId(Helper.getJSONLong("id", 0, item));
                        notification.setRead(Helper.getJSONBoolean("is_read", false, item));
                        notification.setSeen(Helper.getJSONBoolean("is_seen", false, item));

                        try {
                            String created = Helper.getJSONString("created_at", null, item);
                            notification.setTimestamp(created == null ? new Date() : Date.from(Instant.parse(created)));
                        } catch (DateTimeParseException ex) {
                            notification.setTimestamp(new Date());
                        }

                        if (notification.getRemoteId() > 0 && !Helper.isNullOrEmpty(notification.getDescription())) {
                            notifications.add(notification);
                        }
                    }
                }
            }
        } catch (ClassCastException | LbryioRequestException | LbryioResponseException | JSONException | IllegalStateException ex) {
            ex.printStackTrace();
            return null;
        }

        return notifications;
    }

    private boolean isCommentNotification(JSONObject item) {
        return "comment".equalsIgnoreCase(Helper.getJSONString("notification_rule", null, item))
                || "creator_comment".equalsIgnoreCase(Helper.getJSONString("notification_rule", null, item))
                || "comment-reply".equalsIgnoreCase(Helper.getJSONString("notification_rule", null, item));
    }
}
