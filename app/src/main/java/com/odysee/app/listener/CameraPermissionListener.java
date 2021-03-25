package com.odysee.app.listener;

public interface CameraPermissionListener {
    void onCameraPermissionGranted();
    void onCameraPermissionRefused();
    void onRecordAudioPermissionGranted();
    void onRecordAudioPermissionRefused();
}
