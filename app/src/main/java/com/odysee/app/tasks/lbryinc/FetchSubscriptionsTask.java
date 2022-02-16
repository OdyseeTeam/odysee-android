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

                if (subscriptions.size() == 0) {
                    // if there are no subs in the local store, check the wallet shared object
                    JSONObject result = (JSONObject) Lbry.authenticatedGenericApiCall(Lbry.METHOD_PREFERENCE_GET, Lbry.buildSingleParam("key", "shared"), authToken);
                    JSONObject shared = result.getJSONObject("shared");
                    if (shared.has("type")
                            && "object".equalsIgnoreCase(shared.getString("type"))
                            && shared.has("value")) {
                        JSONObject value = shared.getJSONObject("value");

                        JSONArray subscriptionUrls =
                                value.has("subscriptions") && !value.isNull("subscriptions") ? value.getJSONArray("subscriptions") : null;
                        JSONArray following =
                                value.has("following") && !value.isNull("following") ? value.getJSONArray("following") : null;

                        if (subscriptionUrls != null) {
                            subscriptions = new ArrayList<>();
                            for (int i = 0; i < subscriptionUrls.length(); i++) {
                                String url = subscriptionUrls.getString(i);
                                try {
                                    LbryUri uri = LbryUri.parse(LbryUri.normalize(url));
                                    Subscription subscription = new Subscription();
                                    subscription.setChannelName(uri.getChannelName());
                                    subscription.setUrl(uri.toString());
                                    subscription.setNotificationsDisabled(LoadSharedUserStateTask.isNotificationsDisabledForSubUrl(uri.toString(), following));
                                    subscriptions.add(subscription);
                                    if (db != null) {
                                        DatabaseHelper.createOrUpdateSubscription(subscription, db);
                                    }
                                } catch (LbryUriException | SQLiteException | IllegalStateException ex) {
                                    // pass
                                }
                            }
                        }
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
