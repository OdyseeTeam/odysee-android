package com.odysee.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.odysee.app.auth.OdyseeAccountAuthenticator;

public class OdyseeAccountAuthenticatorService extends Service {
    private OdyseeAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        authenticator = new OdyseeAccountAuthenticator(this);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}
