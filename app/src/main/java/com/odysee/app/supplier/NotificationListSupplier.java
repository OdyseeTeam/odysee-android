package com.odysee.app.supplier;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.LbryNotification;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
                            if (dynamic.has("comment_author")) {
                                notification.setAuthorUrl(Helper.getJSONString("comment_author", null, dynamic));
                            }
                            if (dynamic.has("channelURI")) {
                                String channelUrl = Helper.getJSONString("channelURI", null, dynamic);
                                if (!Helper.isNullOrEmpty(channelUrl)) {
                                    notification.setTargetUrl(channelUrl);
                                }
                            }
                            if (dynamic.has("hash") && "comment".equalsIgnoreCase(Helper.getJSONString("notification_rule", null, item))) {
                                notification.setTargetUrl(String.format("%s?comment_hash=%s", notification.getTargetUrl(), dynamic.getString("hash")));
                            }
                        }

                        notification.setRule(Helper.getJSONString("notification_rule", null, item));
                        notification.setRemoteId(Helper.getJSONLong("id", 0, item));
                        notification.setRead(Helper.getJSONBoolean("is_read", false, item));
                        notification.setSeen(Helper.getJSONBoolean("is_seen", false, item));

                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat(Helper.ISO_DATE_FORMAT_JSON, Locale.US);
                            notification.setTimestamp(dateFormat.parse(Helper.getJSONString("created_at", dateFormat.format(new Date()), item)));
                        } catch (ParseException ex) {
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
}
