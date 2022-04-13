package com.odysee.app.utils;

public class FirebaseMessagingToken {
    public static void getFirebaseMessagingToken(GetTokenListener listener) {
        listener.onComplete(null);
    }

    public interface GetTokenListener {
        void onComplete(String token);
    }
}
