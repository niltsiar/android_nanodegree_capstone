package com.github.niltsiar.ultimatescrobbler.data.model;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class PlayedSongEntity {

    public abstract String getId();

    public abstract String getTrackName();

    public abstract String getArtistName();

    @Nullable
    public abstract String getAlbumName();

    public abstract int getDuration();

    public static Builder builder() {
        return new AutoValue_PlayedSongEntity.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(String newId);

        public abstract Builder setTrackName(String newTrackName);

        public abstract Builder setArtistName(String newArtistName);

        public abstract Builder setAlbumName(String newAlbumName);

        public abstract Builder setDuration(int newDuration);

        public abstract PlayedSongEntity build();
    }
}
