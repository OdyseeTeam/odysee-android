package com.odysee.app.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.LbryUri;
import com.odysee.app.utils.Lbryio;

// background task to create a diff of local and remote subscriptions and try to merge
public class MergeSubscriptionsTask extends AsyncTask<Void, Void, List<Subscription>> {
    private static final String TAG = "MergeSubscriptionsTask";
    private final Context context;
    private final List<Subscription> base;
    private List<Subscription> diff;
    private final MergeSubscriptionsHandler handler;
    private Exception error;
    private final boolean replaceLocal;

    public MergeSubscriptionsTask(List<Subscription> base, boolean replaceLocal, Context context, MergeSubscriptionsHandler handler) {
        this.base = base;
        this.replaceLocal = replaceLocal;
        this.context = context;
        this.handler = handler;
    }

    protected List<Subscription> doInBackground(Void... params) {
        List<Subscription> combined = new ArrayList<>(base);
        List<Subscription> localSubs = new ArrayList<>();
        diff = new ArrayList<>();
        SQLiteDatabase db = null;
        try {
            // fetch local subscriptions
            if (context instanceof MainActivity) {
                db = MainActivity.getDatabaseHelper().getWritableDatabase();
            }
            if (db != null) {
                if (replaceLocal) {
                    DatabaseHelper.clearSubscriptions(db);
                    for (Subscription sub : base) {
                        DatabaseHelper.createOrUpdateSubscription(sub, db);
                    }
                } else {
                    localSubs = DatabaseHelper.getSubscriptions(db);
                    for (Subscription sub : localSubs) {
                        if (!combined.contains(sub)) {
                            combined.add(sub);
                        }
                    }
                }
            }

            if (!replaceLocal) {
                for (int i = 0; i < localSubs.size(); i++) {
                    Subscription local = localSubs.get(i);
                    if (!base.contains(local) && !diff.contains(local)) {
                        diff.add(local);
                    }
                }
            }
        } catch (ClassCastException | IllegalStateException | SQLiteException ex) {
            error = ex;
            return null;
        }

        return combined;
    }
    protected void onPostExecute(List<Subscription> subscriptions) {
        if (handler != null) {
            if (subscriptions != null) {
                handler.onSuccess(subscriptions, diff);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface MergeSubscriptionsHandler {
        void onSuccess(List<Subscription> subscriptions, List<Subscription> diff);
        void onError(Exception error);
    }
}
