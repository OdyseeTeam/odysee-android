package com.odysee.app.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.odysee.app.MainActivity;
import com.odysee.app.tasks.RewardVerifiedHandler;
import com.odysee.app.tasks.lbryinc.AndroidPurchaseTask;

public class HandleBillingPurchase {
    public static void handleBillingPurchase(
            Purchase purchase,
            BillingClient billingClient,
            Context context,
            View progressView,
            RewardVerifiedHandler handler) {
        String sku = purchase.getSku();
        if (MainActivity.SKU_SKIP.equalsIgnoreCase(sku)) {
            // send purchase token for verification
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                /*&& isSignatureValid(purchase)*/) {
                // consume the purchase
                String purchaseToken = purchase.getPurchaseToken();
                ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build();
                billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {

                    }
                });

                // send the purchase token to the backend to complete verification
                AndroidPurchaseTask task = new AndroidPurchaseTask(purchaseToken, progressView, context, handler);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }
}
