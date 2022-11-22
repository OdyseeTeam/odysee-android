package com.odysee.app.supplier;

import com.odysee.app.callable.CommentEnabled;

import java.util.function.Supplier;

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
