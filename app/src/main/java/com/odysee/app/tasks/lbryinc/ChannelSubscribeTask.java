package com.odysee.app.tasks.lbryinc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import com.odysee.app.MainActivity;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.utils.Lbryio;

public class ChannelSubscribeTask extends AsyncTask<Void, Void, Boolean> {
    private final Context context;
    private final String channelClaimId;
    private final Subscription subscription;
    private final ChannelSubscribeHandler handler;
    private Exception error;
    private final boolean isUnsubscribing;

    public ChannelSubscribeTask(Context context, String channelClaimId, Subscription subscription, boolean isUnsubscribing, ChannelSubscribeHandler handler) {
        this.context = context;
        this.channelClaimId = channelClaimId;
        this.subscription = subscription;
        this.handler = handler;
        this.isUnsubscribing = isUnsubscribing;
    }
    protected Boolean doInBackground(Void... params) {
        SQLiteDatabase db = null;
        try {
            // Save to (or delete from) local store
            if (context instanceof MainActivity) {
                db = MainActivity.getDatabaseHelper().getWritableDatabase();
            }
            if (db != null) {
                if (!isUnsubscribing) {
                    DatabaseHelper.createOrUpdateSubscription(subscription, db);
                } else {
                    DatabaseHelper.deleteSubscription(subscription, db);
                }
            }

            if (!isUnsubscribing) {
                Lbryio.addSubscription(subscription);
            } else {
                Lbryio.removeSubscription(subscription);
            }
        } catch (SQLiteException ex) {
            error = ex;
            return false;
        }

        return true;
    }
    protected void onPostExecute(Boolean success) {
        if (handler != null) {
            if (success) {
                handler.onSuccess();
            } else {
                handler.onError(error);
            }
        }
    }

    public interface ChannelSubscribeHandler {
        void onSuccess();
        void onError(Exception exception);
    }
}
