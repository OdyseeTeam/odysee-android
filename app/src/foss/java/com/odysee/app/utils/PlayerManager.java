package com.odysee.app.utils;

import android.content.Context;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.Util;
import com.odysee.app.MainActivity;
import com.odysee.app.R;
import com.odysee.app.model.Claim;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    @Getter
    private final ExoPlayer currentPlayer;

    @Setter
    private Listener listener;

    public PlayerManager(MainActivity activity) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();

        currentPlayer = new ExoPlayer.Builder(activity).build();
        currentPlayer.setWakeMode(C.WAKE_MODE_NETWORK);

        currentPlayer.setAudioAttributes(audioAttributes, true);
    }

    public void initializeCurrentPlayer(String sourceUrl, long positionMs, Claim claim, Context context) {
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        if (context != null) {
            dataSourceFactory.setUserAgent(Util.getUserAgent(context, context.getString(R.string.app_name)));
        }

        Map<String, String> requestProperties = new HashMap<>(1);
        requestProperties.put("Referer", "https://odysee.com/");
        dataSourceFactory.setDefaultRequestProperties(requestProperties);

        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory();
        cacheDataSourceFactory.setUpstreamDataSourceFactory(dataSourceFactory);
        cacheDataSourceFactory.setCache(MainActivity.playerCache);

        MediaSource mediaSource;
        if (MainActivity.videoIsTranscoded) {
            mediaSource = new HlsMediaSource.Factory(cacheDataSourceFactory)
                    .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(sourceUrl));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory, new DefaultExtractorsFactory())
                    .setLoadErrorHandlingPolicy(new StreamLoadErrorPolicy())
                    .createMediaSource(MediaItem.fromUri(sourceUrl));
        }

        currentPlayer.setMediaSource(mediaSource, positionMs);
        currentPlayer.prepare();
    }

    public void stopPlayers() {
        currentPlayer.stop();
        currentPlayer.release();
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
