package com.odysee.app.utils;

import android.app.Activity;

import androidx.mediarouter.app.MediaRouteButton;

public class CastHelper {
    public CastHelper(Activity context, MediaRouteButton introductoryOverlayButton, Listener listener) { }

    public void setUpCastButton(MediaRouteButton mediaRouteButton) { }

    public void addCastStateListener() { }

    public void removeCastStateListener() { }

    public interface Listener {
        void updateMediaRouteButtonVisibility(boolean isVisible);
    }
}
