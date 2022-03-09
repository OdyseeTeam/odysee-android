package com.odysee.app.tasks.lbryinc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.odysee.app.MainActivity;
import com.odysee.app.data.DatabaseHelper;
import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.exceptions.LbryUriException;
import com.odysee.app.model.lbryinc.Subscription;
import com.odysee.app.tasks.wallet.LoadSharedUserStateTask;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbry;
import com.odysee.app.utils.LbryUri;

public class FetchSubscriptionsTask extends AsyncTask<Void, Void, List<Subscription>> {
    private final Context context;
    private final FetchSubscriptionsHandler handler;
    private final ProgressBar progressBar;
    private String authToken;
    private Exception error;

    public FetchSubscriptionsTask(Context context, ProgressBar progressBar, String authToken, FetchSubscriptionsHandler handler) {
        this.context = context;
        this.progressBar = progressBar;
        this.handler = handler;
        this.authToken = authToken;
    }
    protected void onPreExecute() {
        Helper.setViewVisibility(progressBar, View.VISIBLE);
    }
    protected List<Subscription> doInBackground(Void... params) {
        List<Subscription> subscriptions = new ArrayList<>();
        SQLiteDatabase db = null;
        try {
            if (context instanceof MainActivity) {
                db = ((MainActivity) context).getDbHelper().getWritableDatabase();
                if (db != null) {
                    subscriptions = new ArrayList<>(DatabaseHelper.getSubscriptions(db));
                }

                // obtain subscriptions from the wallet shared object
                List<Subscription> sharedStateSubs = new ArrayList<>();
                JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_PREFERENCE_GET, Lbry.buildSingleParam("key", "shared"), authToken);
                JSONObject shared = result.getJSONObject("shared");
                if (shared.has("type") && "object".equalsIgnoreCase(shared.getString("type")) && shared.has("value")) {
                     sharedStateSubs = new ArrayList<>(LoadSharedUserStateTask.loadSubscriptionsFromSharedUserState(shared));
                }

                for (Subscription sub : sharedStateSubs) {
                    // merge with subscriptions in local store
                    if (!subscriptions.contains(sub)) {
                        subscriptions.add(sub);
                    }
                    if (db != null) {
                        DatabaseHelper.createOrUpdateSubscription(sub, db);
                    }
                }
            }
        } catch (ClassCastException | ApiCallException | JSONException | IllegalStateException ex) {
            error = ex;
            return null;
        }

        return subscriptions;
    }
    protected void onPostExecute(List<Subscription> subscriptions) {
        Helper.setViewVisibility(progressBar, View.GONE);
        if (handler != null) {
            if (subscriptions != null) {
                handler.onSuccess(subscriptions);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface FetchSubscriptionsHandler {
        void onSuccess(List<Subscription> subscriptions);
        void onError(Exception exception);
    }
}
