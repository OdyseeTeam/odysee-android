package com.odysee.app.checkers;

import com.odysee.app.supplier.CommentEnabledSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CommentEnabledCheck {

    public void checkCommentStatus(String channelId, String channelName, CommentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("CommentStatus interface cannot be null");
        }
        Supplier<Boolean> task = new CommentEnabledSupplier(channelId, channelName);
        CompletableFuture<Boolean> cf = CompletableFuture.supplyAsync(task);
        cf.thenAcceptAsync(status::onStatus);
    }

    public interface CommentStatus {
        void onStatus(boolean isEnabled);
    }
}
