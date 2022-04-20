package com.odysee.app.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class FirebaseMessagingToken {
    public static void getFirebaseMessagingToken(GetTokenListener listener) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        listener.onComplete(null);
                        return;
                    }
                    listener.onComplete(task.getResult());
                }
            });
        } catch (IllegalStateException ex) {
            // pass
        }
    }

    public interface GetTokenListener {
        void onComplete(String token);
    }
}
