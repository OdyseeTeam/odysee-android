package com.odysee.app.supplier;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.odysee.app.callable.CommentEnabled;

import java.util.function.Supplier;

@RequiresApi(api = Build.VERSION_CODES.N)
public final class CommentEnabledSupplier implements Supplier<Boolean> {
    private final CommentEnabled callable;

    public CommentEnabledSupplier(String channelId, String channelName) {
        this.callable = new CommentEnabled(channelId, channelName);
    }

    @Override
    public Boolean get() {
        return callable.call();
    }
}
