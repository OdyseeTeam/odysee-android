package com.odysee.app.listener;

import android.net.Uri;

public interface FilePickerListener {
    void onFilePicked(String filePath, Uri intentData);
    void onFilePickerCancelled();
}
