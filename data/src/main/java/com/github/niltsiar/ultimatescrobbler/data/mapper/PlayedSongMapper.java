package com.github.niltsiar.ultimatescrobbler.data.mapper;

import com.github.niltsiar.ultimatescrobbler.data.model.PlayedSongEntity;
import com.github.niltsiar.ultimatescrobbler.domain.model.PlayedSong;
import javax.inject.Inject;

public class PlayedSongMapper implements Mapper<PlayedSongEntity, PlayedSong> {

    @Inject
    public PlayedSongMapper() {
    }

    @Override
    public PlayedSong mapFromEntity(PlayedSongEntity type) {
        return PlayedSong.builder()
                         .setId(type.getId())
                         .setTrackName(type.getTrackName())
                         .setArtistName(type.getArtistName())
                         .setAlbumName(type.getAlbumName())
                         .setLength(type.getDuration())
                         .setTimestamp(type.getTimestamp())
                         .build();
    }

    @Override
    public PlayedSongEntity mapToEntity(PlayedSong type) {
        return PlayedSongEntity.builder()
                               .setId(type.getId())
                               .setTrackName(type.getTrackName())
                               .setArtistName(type.getArtistName())
                               .setAlbumName(type.getAlbumName())
                               .setDuration(type.getLength())
                               .setTimestamp(type.getTimestamp())
                               .build();
    }
}
