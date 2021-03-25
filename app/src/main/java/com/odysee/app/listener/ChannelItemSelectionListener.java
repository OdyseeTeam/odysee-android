package com.odysee.app.listener;

import com.odysee.app.model.Claim;

public interface ChannelItemSelectionListener {
    void onChannelItemSelected(Claim claim);
    void onChannelItemDeselected(Claim claim);
    void onChannelSelectionCleared();
}
