package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

public class NotificationUpdateTask extends AsyncTask<Void, Void, Boolean> {
    private final List<Long> ids;
    private final boolean seen;
    private final boolean read;
    private final boolean updateRead;

    public NotificationUpdateTask(List<Long> ids, boolean seen) {
        this(ids, false, true, false);
    }

    public NotificationUpdateTask(List<Long> ids, boolean read, boolean seen, boolean updateRead) {
        this.ids = ids;
        this.read = read;
        this.seen = seen;
        this.updateRead = updateRead;
    }

    protected Boolean doInBackground(Void... params) {
        Map<String, String> options = new HashMap<>();
        options.put("notification_ids", Helper.joinL(ids, ","));
        options.put("is_seen", String.valueOf(seen));
        if (updateRead) {
            options.put("is_read", String.valueOf(read));
        }

        try {
            Object result = Lbryio.parseResponse(Lbryio.call("notification", "edit", options, null));
            return "ok".equalsIgnoreCase(result.toString());
        } catch (LbryioResponseException | LbryioRequestException ex) {

        }
        return false;
    }
}
