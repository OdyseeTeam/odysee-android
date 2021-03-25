package com.odysee.app.tasks.localdata;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.model.UrlSuggestion;

public class FetchRecentUrlHistoryTask extends AsyncTask<Void, Void, List<UrlSuggestion>> {
    private final DatabaseHelper dbHelper;
    private final FetchRecentUrlHistoryHandler handler;
    public FetchRecentUrlHistoryTask(DatabaseHelper dbHelper, FetchRecentUrlHistoryHandler handler) {
        this.dbHelper = dbHelper;
        this.handler = handler;
    }
    protected List<UrlSuggestion> doInBackground(Void... params) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            return DatabaseHelper.getRecentHistory(db);
        } catch (SQLiteException ex) {
            return new ArrayList<>();
        }
    }
    protected void onPostExecute(List<UrlSuggestion> recentHistory) {
        if (handler != null) {
            handler.onSuccess(recentHistory);
        }
    }

    public interface FetchRecentUrlHistoryHandler {
        void onSuccess(List<UrlSuggestion> recentHistory);
    }
}
