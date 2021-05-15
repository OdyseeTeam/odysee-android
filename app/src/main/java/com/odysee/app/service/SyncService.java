package com.odysee.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.odysee.app.adapter.SyncAdapter;

public class SyncService extends Service {
    private static final Object _sync_adapter_lock = new Object();
    private static SyncAdapter _sync_adapter = null;

    @Override
    public void onCreate() {
        synchronized (_sync_adapter_lock) {
            if (_sync_adapter == null)
                _sync_adapter = new SyncAdapter(getApplicationContext(), false);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return _sync_adapter.getSyncAdapterBinder();
    }
}
