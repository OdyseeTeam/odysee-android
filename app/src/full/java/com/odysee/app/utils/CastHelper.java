package com.odysee.app.utils;

import android.app.Activity;

import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;

public class CastHelper {
    private final Activity context;
    private final CastContext castContext;
    private final CastStateListener castStateListener;

    public CastHelper(Activity context, Listener listener) {
        this.context = context;

        castContext = CastContext.getSharedInstance(context);

        castStateListener = state -> listener.updateMediaRouteButtonVisibility(state != CastState.NO_DEVICES_AVAILABLE);
    }

    public void setUpCastButton(MediaRouteButton mediaRouteButton) {
        CastButtonFactory.setUpMediaRouteButton(context, mediaRouteButton);
    }

    public void addCastStateListener() {
        castContext.addCastStateListener(castStateListener);
    }

    public void removeCastStateListener() {
        castContext.removeCastStateListener(castStateListener);
    }

    public interface Listener {
        void updateMediaRouteButtonVisibility(boolean isVisible);
    }
}
