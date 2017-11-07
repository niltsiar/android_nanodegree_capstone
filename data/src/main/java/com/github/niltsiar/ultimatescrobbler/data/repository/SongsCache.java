package com.github.niltsiar.ultimatescrobbler.data.repository;

import com.github.niltsiar.ultimatescrobbler.data.model.PlayedSongEntity;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface SongsCache {

    Completable savePlayedSong(PlayedSongEntity playedSongEntity);

    Single<Long> countStoredPlayedSongs();
}
