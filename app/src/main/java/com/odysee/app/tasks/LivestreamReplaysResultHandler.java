package com.odysee.app.tasks;

import com.odysee.app.model.LivestreamReplay;

import java.util.List;

public interface LivestreamReplaysResultHandler {
    void onSuccess(List<LivestreamReplay> replays);
    void onError(Exception error);
}
