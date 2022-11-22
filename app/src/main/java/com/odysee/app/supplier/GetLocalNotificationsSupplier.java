package com.odysee.app.supplier;

import android.database.sqlite.SQLiteDatabase;

import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.model.lbryinc.LbryNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GetLocalNotificationsSupplier implements Supplier<List<LbryNotification>> {
    @Override
    public List<LbryNotification> get() {
        List<LbryNotification> notifications = new ArrayList<>();
        DatabaseHelper dbHelper = DatabaseHelper.getInstance();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            notifications = DatabaseHelper.getNotifications(db);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return notifications;
    }
}