package com.odysee.app.listener;

import android.view.View;

public interface YouTubeSyncListener {
    void onSkipPressed();
    void onNewSyncPressed();
    void onClaimNowPressed(String channelName, View skip, View inputSource, View eventSource, View progress);
    void onDonePressed();
}
