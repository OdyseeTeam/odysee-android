package com.odysee.app.utils;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.cast.CastPlayer;
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.cast.framework.CastContext;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.model.Claim;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private final ExoPlayer localPlayer;
    private final CastPlayer castPlayer;

    @Setter
    private Listener listener;
    @Getter
    private Player currentPlayer;

    public PlayerManager(MainActivity activity) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();

        localPlayer = new ExoPlayer.Builder(activity).build();
        localPlayer.setWakeMode(C.WAKE_MODE_NETWORK);

        localPlayer.setAudioAttributes(audioAttributes, true);

        CastContext castContext = CastContext.getSharedInstance(activity);
        castPlayer = new CastPlayer(castContext);
        castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                setCurrentPlayer(castPlayer);
            }

            @Override
            public void onCastSessionUnavailable() {
                setCurrentPlayer(localPlayer);
            }
        });

        setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : localPlayer);
    }

    public void initializeCurrentPlayer(String sourceUrl, long positionMs, Claim claim, Context context) {
        if (currentPlayer == localPlayer) {
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            if (context != null) {
                dataSourceFactory.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)));
            }

            Map<String, String> requestProperties = new HashMap<>(1);
            requestProperties.put("Referer", "https://odysee.com");
            dataSourceFactory.setDefaultRequestProperties(requestProperties);
            // NOTE: Odysee Android is using default Google Cast receiver which doesn't allow to customize rquests headers. A custom web receiver should be implemented.

            CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory();
            cacheDataSourceFactory.setUpstreamDataSourceFactory(dataSourceFactory);
            cacheDataSourceFactory.setCache(MainActivity.playerCache);

            MediaSource mediaSource;
            if (MainActivity.videoIsTranscoded) {
                mediaSource = new HlsMediaSource.Factory(!claim.isLive() ? cacheDataSourceFactory : dataSourceFactory)
                        .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                        .createMediaSource(MediaItem.fromUri(sourceUrl));
            } else {
                mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory, new DefaultExtractorsFactory())
                        .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                        .createMediaSource(MediaItem.fromUri(sourceUrl));
            }

            localPlayer.setMediaSource(mediaSource, positionMs);
            localPlayer.prepare();
        } else if (currentPlayer == castPlayer) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(claim.getTitle())
                    .setArtworkUri(Uri.parse(claim.getThumbnailUrl()))
                    .build();
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(sourceUrl)
                    .setMediaMetadata(metadata)
                    .setMimeType(MainActivity.videoIsTranscoded ? "application/x-mpegurl" : MimeTypes.VIDEO_MP4)
                    .build();

            castPlayer.setMediaItem(mediaItem, positionMs);
            castPlayer.prepare();
        }
    }

    public void stopPlayers() {
        localPlayer.stop();
        localPlayer.release();
        castPlayer.setSessionAvailabilityListener(null);
        castPlayer.release();
    }

    private void setCurrentPlayer(Player currentPlayer) {
        if (this.currentPlayer == currentPlayer) {
            return;
        }

        // Player state management.
        long playbackPositionMs = C.TIME_UNSET;

        Player previousPlayer = this.currentPlayer;
        if (previousPlayer != null) {
            // Save state from the previous player.
            int playbackState = previousPlayer.getPlaybackState();
            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = previousPlayer.getCurrentPosition();
            }
            previousPlayer.stop();
            previousPlayer.clearMediaItems();
        }

        this.currentPlayer = currentPlayer;

        if (listener != null) {
            listener.onPlayerChanged(playbackPositionMs);
        }

        currentPlayer.setPlayWhenReady(true);
    }

    public interface Listener {
        /**
         * Called when player is changed from local to cast.
         *
         * {@code currentPlayer} should be initialized with media
         * and used in a {@link com.google.android.exoplayer2.ui.PlayerView}.
         *
         * @param previousPlaybackMs The playback position of the previous player.
         */
        void onPlayerChanged(long previousPlaybackMs);
    }

    private static class StreamLoadErrorPolicy extends DefaultLoadErrorHandlingPolicy {
        @Override
        public int getMinimumLoadableRetryCount(int dataType) {
            return Integer.MAX_VALUE;
        }
    }
}
