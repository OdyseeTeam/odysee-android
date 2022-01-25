package com.odysee.app.listener;

public interface VerificationListener {
    void onEmailAdded(String email);
    void onEmailEdit();
    void onEmailVerified();
    void onPhoneAdded(String countryCode, String phoneNumber);
    void onPhoneVerified();
    void onManualVerifyContinue();
    void onSkipQueueAction();
    void onTwitterVerified();
    void onManualProgress(boolean progress);
}
