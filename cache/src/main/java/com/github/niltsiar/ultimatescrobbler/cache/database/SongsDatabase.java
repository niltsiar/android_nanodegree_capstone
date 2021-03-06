package com.github.niltsiar.ultimatescrobbler.cache.database;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

@Database(version = SongsDatabase.VERSION,
          packageName = "com.github.niltsiar.ultimatescrobbler.cache.provider")
public class SongsDatabase {

    private SongsDatabase() {
        //Avoid instances
    }

    public static final int VERSION = 1;

    @Table(PlayedSongColumns.class)
    public static final String PLAYED_SONGS = "played_songs";

    @Table(InfoSongColumns.class)
    public static final String INFO_SONG = "info_song";

    @Table(PlayedSongColumns.class)
    public static final String CURRENT_SONG = "current_song";
}
