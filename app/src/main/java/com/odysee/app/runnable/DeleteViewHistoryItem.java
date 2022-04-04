package com.odysee.app.runnable;

import android.database.sqlite.SQLiteDatabase;
import com.odysee.app.data.DatabaseHelper;

public class DeleteViewHistoryItem implements Runnable {
    private final String url;

    public DeleteViewHistoryItem(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (db != null) {
            try {
                DatabaseHelper.removeViewHistoryItem(db, url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dbHelper.close();
        }
    }
}
