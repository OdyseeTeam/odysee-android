package com.odysee.app.tasks.lbryinc;

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import com.odysee.app.exceptions.LbryioRequestException;
import com.odysee.app.exceptions.LbryioResponseException;
import com.odysee.app.model.Claim;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.utils.Lbryio;

public class LogFileViewTask extends AsyncTask<Void, Void, Boolean> {
    private final String uri;
    private final Claim claim;
    private Exception error;
    private final GenericTaskHandler handler;
    private final long timeToStart;

    public LogFileViewTask(String uri, Claim claim, long timeToStart, GenericTaskHandler handler) {
        this.uri = uri;
        this.claim = claim;
        this.timeToStart = timeToStart;
        this.handler = handler;
    }
    protected Boolean doInBackground(Void... params) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("uri", uri);
            options.put("claim_id", claim.getClaimId());
            options.put("outpoint", String.format("%s:%d", claim.getTxid(), claim.getNout()));
            if (timeToStart > 0) {
                options.put("time_to_start", String.valueOf(timeToStart));
            }
            Lbryio.call("file", "view", options,  null).close();
        } catch (LbryioRequestException | LbryioResponseException ex) {
            error = ex;
            return false;
        }
        return true;
    }
    protected void onPostExecute(Boolean result) {
        if (handler != null) {
            if (result) {
                handler.onSuccess();
            } else {
                handler.onError(error);
            }
        }
    }
}
