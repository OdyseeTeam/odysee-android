package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;
import android.view.View;


import java.util.HashMap;
import java.util.Map;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.utils.Helper;
import com.odysee.app.utils.Lbryio;

public class InviteByEmailTask extends AsyncTask<Void, Void, Boolean> {
    private final String email;
    private final View progressView;
    private final GenericTaskHandler handler;
    private Exception error;

    public InviteByEmailTask(String email, View progressView, GenericTaskHandler handler) {
        this.email = email;
        this.progressView = progressView;
        this.handler = handler;
    }
    protected void onPreExecute() {
        Helper.setViewVisibility(progressView, View.VISIBLE);
        if (handler != null) {
            handler.beforeStart();
        }
    }
    protected Boolean doInBackground(Void... params) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("email", email);
            Lbryio.parseResponse(Lbryio.call("user", "invite", options, Helper.METHOD_POST, null));
        } catch (LbryioRequestException | LbryioResponseException ex) {
            error = ex;
            return false;
        }

        return true;
    }
    protected void onPostExecute(Boolean result) {
        Helper.setViewVisibility(progressView, View.GONE);
        if (handler != null) {
            if (result) {
                handler.onSuccess();
            } else {
                handler.onError(error);
            }
        }
    }
}
