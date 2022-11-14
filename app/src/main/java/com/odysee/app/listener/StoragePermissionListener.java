package com.odysee.app.listener;

public interface StoragePermissionListener {
    void onStoragePermissionGranted();
    void onStoragePermissionRefused();

    // Android 13+
    void onManageExternalStoragePermissionGranted();
    void onManageExternalStoragePermissionRefused();
}
