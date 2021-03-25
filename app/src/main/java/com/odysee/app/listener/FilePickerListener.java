package com.odysee.app.listener;

public interface FilePickerListener {
    void onFilePicked(String filePath);
    void onFilePickerCancelled();
}
