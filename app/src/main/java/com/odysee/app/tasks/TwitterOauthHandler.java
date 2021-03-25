package com.odysee.app.tasks;

import com.odysee.app.model.TwitterOauth;

public interface TwitterOauthHandler {
    void onSuccess(TwitterOauth twitterOauth);
    void onError(Exception error);
}
