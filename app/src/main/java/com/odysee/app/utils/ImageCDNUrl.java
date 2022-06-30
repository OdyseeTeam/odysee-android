package com.odysee.app.utils;

import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public final class ImageCDNUrl {
    private String appendedPath = "";

    public ImageCDNUrl(int width, int height, int quality, @Nullable String format, String thumbnailUrl) {
        appendedPath = "s:".concat(String.valueOf(width)).concat(":").concat(String.valueOf(height)).concat("/");

        appendedPath = appendedPath.concat("quality:").concat(String.valueOf(quality)).concat("/");

        appendedPath = appendedPath.concat("plain/").concat(thumbnailUrl);

        if (format != null) {
            appendedPath = appendedPath.concat("@").concat(format);
        }
    }

    @NotNull
    @Override
    public String toString() {
        String url = "https://thumbnails.odycdn.com/optimize/";
        return url.concat(appendedPath);
    }
}
