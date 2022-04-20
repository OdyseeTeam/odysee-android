package com.odysee.app.utils;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.odysee.app.MainActivity;
import com.odysee.app.tasks.RewardVerifiedHandler;

import java.util.ArrayList;
import java.util.List;

public class VerificationSkipQueue {
    private BillingClient billingClient;
    private final Context context;
    private final PurchasesUpdatedListener purchasesUpdatedListener;

    public VerificationSkipQueue(Context context, ShowInProgressListener listener, RewardVerifiedHandler handler) {
        this.context = context;
        purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                int responseCode = billingResult.getResponseCode();
                if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
                {
                    for (Purchase purchase : purchases) {
                        if (MainActivity.SKU_SKIP.equalsIgnoreCase(purchase.getSku())) {
                            listener.maybeShowRequestInProgress();

                            HandleBillingPurchase.handleBillingPurchase(purchase,
                                    billingClient, context, null, handler);
                        }
                    }
                }
            }
        };
    }

    public void createBillingClientAndEstablishConnection() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        establishBillingClientConnection();
    }

    public void onSkipQueueAction(Activity activity) {
        if (billingClient != null) {
            List<String> skuList = new ArrayList<>();
            skuList.add(MainActivity.SKU_SKIP);

            SkuDetailsParams detailsParams = SkuDetailsParams.newBuilder().
                    setType(BillingClient.SkuType.INAPP).
                    setSkusList(skuList).build();
            billingClient.querySkuDetailsAsync(detailsParams, new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
                    if (list != null && list.size() > 0) {
                        // we only queried one product, so it should be the first item in the list
                        SkuDetails skuDetails = list.get(0);

                        // launch the billing flow for skip queue
                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().
                                setSkuDetails(skuDetails).build();
                        billingClient.launchBillingFlow(activity, billingFlowParams);
                    }
                }
            });
        }
    }

    private void establishBillingClientConnection() {
        if (billingClient != null) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // no need to do anything here. purchases are always checked server-side
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    establishBillingClientConnection();
                }
            });
        }
    }

    public interface ShowInProgressListener {
        void maybeShowRequestInProgress();
    }
}
