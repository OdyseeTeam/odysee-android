package com.odysee.app.utils;

import android.app.Activity;

import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.odysee.app.R;

public class CastHelper {
    private final Activity context;
    private final CastContext castContext;
    private final CastStateListener castStateListener;
    private IntroductoryOverlay introductoryOverlay;

    public CastHelper(Activity context, MediaRouteButton introductoryOverlayButton, Listener listener) {
        this.context = context;

        castContext = CastContext.getSharedInstance(context);

        castStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int state) {
                boolean isVisible = state != CastState.NO_DEVICES_AVAILABLE;
                listener.updateMediaRouteButtonVisibility(isVisible);
                if (isVisible) {
                    if (introductoryOverlay != null) {
                        introductoryOverlay.remove();
                    }
                    introductoryOverlay = new IntroductoryOverlay.Builder(context, introductoryOverlayButton)
                            .setTitleText(R.string.cast_introductory_overlay_text)
                            .setSingleTime()
                            .setOnOverlayDismissedListener(new IntroductoryOverlay.OnOverlayDismissedListener() {
                                @Override
                                public void onOverlayDismissed() {
                                    introductoryOverlay = null;
                                }
                            })
                            .build();
                    introductoryOverlay.show();
                }
            }
        };
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
