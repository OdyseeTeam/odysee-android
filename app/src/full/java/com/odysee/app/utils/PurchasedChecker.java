package com.odysee.app.utils;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.model.lbryinc.RewardVerified;
import com.odysee.app.tasks.GenericTaskHandler;
import com.odysee.app.tasks.RewardVerifiedHandler;

import java.util.List;

public class PurchasedChecker {
    private BillingClient billingClient;
    private final Context context;
    private final Activity activity;

    public PurchasedChecker(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public void createBillingClientAndEstablishConnection() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                        int responseCode = billingResult.getResponseCode();
                        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
                        {
                            for (Purchase purchase : purchases) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                })
                .enablePendingPurchases()
                .build();
        establishBillingClientConnection();
    }

    public void checkPurchases() {
        if (billingClient != null) {
            Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getPurchasesList() != null) {
                for (Purchase purchase : result.getPurchasesList()) {
                    handlePurchase(purchase);
                }
            }
        }
    }

    public void checkPurchases(GenericTaskHandler handler) {
        if (billingClient != null) {
            Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getPurchasesList() != null) {
                for (Purchase purchase : result.getPurchasesList()) {
                    handlePurchase(purchase, handler);
                    return;
                }
            }
        }

        handler.onError(new Exception(activity.getString(R.string.skip_queue_purchase_not_found)));
    }

    private void handlePurchase(Purchase purchase) {
        HandleBillingPurchase.handleBillingPurchase(purchase, billingClient,
                activity, null, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }
            }

            @Override
            public void onError(Exception error) {
                // pass
            }
        });
    }

    private void handlePurchase(Purchase purchase, GenericTaskHandler handler) {
        HandleBillingPurchase.handleBillingPurchase(purchase, billingClient,
                activity, null, new RewardVerifiedHandler() {
            @Override
            public void onSuccess(RewardVerified rewardVerified) {
                if (Lbryio.currentUser != null) {
                    Lbryio.currentUser.setRewardApproved(rewardVerified.isRewardApproved());
                }

                if (handler != null) {
                    handler.onSuccess();
                }
            }

            @Override
            public void onError(Exception error) {
                if (handler != null) {
                    handler.onError(error);
                }
            }
        });
    }

    private void establishBillingClientConnection() {
        if (billingClient != null) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // no need to do anything here. purchases are always checked server-side
                        checkPurchases();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    establishBillingClientConnection();
                }
            });
        }
    }
}
