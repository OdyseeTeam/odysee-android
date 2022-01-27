package com.odysee.app.xyz;

import android.os.Build;

import com.odysee.app.callable.CommentEnabled;
import com.odysee.app.supplier.CommentEnabledSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CommentEnabledCheck {

    public void checkCommentStatus(String channelId, String channelName, CommentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("CommentStatus interface cannot be null");
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Supplier<Boolean> task = new CommentEnabledSupplier(channelId, channelName);
            CompletableFuture<Boolean> cf = CompletableFuture.supplyAsync(task);
            cf.thenAcceptAsync(status::onStatus);
        } else {
            new Thread(() -> {
                ExecutorService service = Executors.newSingleThreadExecutor();
                Future<Boolean> future = service.submit(new CommentEnabled(channelId, channelName));
                try {
                    status.onStatus(future.get());
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public interface CommentStatus {
        void onStatus(boolean isEnabled);
    }
}
