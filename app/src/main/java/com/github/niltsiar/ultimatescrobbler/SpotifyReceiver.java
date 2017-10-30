package com.github.niltsiar.ultimatescrobbler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.github.niltsiar.ultimatescrobbler.domain.model.PlayedSong;
import com.jakewharton.rxrelay2.PublishRelay;
import io.reactivex.Observable;
import timber.log.Timber;

public class SpotifyReceiver extends BroadcastReceiver {

    private static IntentFilter spotifyIntents;
    private PublishRelay<PlayedSong> playedSongs;

    private final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    public static IntentFilter getSpotifyIntents() {
        if (null == spotifyIntents) {
            spotifyIntents = new IntentFilter();
            spotifyIntents.addAction(BroadcastTypes.PLAYBACK_STATE_CHANGED);
            spotifyIntents.addAction(BroadcastTypes.METADATA_CHANGED);
        }
        return spotifyIntents;
    }

    public SpotifyReceiver() {
        playedSongs = PublishRelay.create();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        long timeSent = intent.getLongExtra("timeSent", 0);

        Timber.i("Action: %s", action);
        Timber.i("Extras: %s", intent.getExtras()
                                     .toString());

        if (action.equals(BroadcastTypes.METADATA_CHANGED)) {
            String trackId = intent.getStringExtra("id");
            String artistName = intent.getStringExtra("artist");
            String albumName = intent.getStringExtra("album");
            String trackName = intent.getStringExtra("track");
            String length = Integer.toString(intent.getIntExtra("length", 0));
            PlayedSong playedSong = PlayedSong.builder()
                                              .setId(trackId)
                                              .setArtistName(artistName)
                                              .setAlbumName(albumName)
                                              .setTrackName(trackName)
                                              .setLength(Integer.valueOf(length))
                                              .build();

            playedSongs.accept(playedSong);
        } else if (action.equals(BroadcastTypes.PLAYBACK_STATE_CHANGED)) {
            boolean playing = intent.getBooleanExtra("playing", false);
            int positionInMs = intent.getIntExtra("playbackPosition", 0);
        }
    }

    public Observable<PlayedSong> getPlayedSongs() {
        return playedSongs;
    }
}
