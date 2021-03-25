package com.odysee.app.listener;

public interface DownloadActionListener {
    void onDownloadAction(String downloadAction, String uri, String outpoint, String fileInfoJson, double progress);
}
