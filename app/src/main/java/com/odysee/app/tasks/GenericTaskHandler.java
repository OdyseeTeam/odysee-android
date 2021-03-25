package com.odysee.app.tasks;

public interface GenericTaskHandler {
    void beforeStart();
    void onSuccess();
    void onError(Exception error);
}
