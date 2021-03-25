package com.odysee.app.tasks.lbryinc;

import android.content.Context;
import android.os.AsyncTask;

import com.odysee.app.model.lbryinc.User;
import com.odysee.app.utils.Lbryio;

public class FetchCurrentUserTask extends AsyncTask<Void, Void, User> {
    private final Context context;
    private Exception error;
    private final FetchUserTaskHandler handler;

    public FetchCurrentUserTask(Context context, FetchUserTaskHandler handler) {
        this.context = context;
        this.handler = handler;
    }
    protected User doInBackground(Void... params) {
        try {
            return Lbryio.fetchCurrentUser(context);
        } catch (Exception ex) {
            error = ex;
            return null;
        }
    }

    protected void onPostExecute(User result) {
        if (handler != null) {
            if (result != null) {
                handler.onSuccess(result);
            } else {
                handler.onError(error);
            }
        }
    }

    public interface FetchUserTaskHandler {
        void onSuccess(User user);
        void onError(Exception error);
    }
}
