package net.dorokhov.pony2.api.library.domain;

import jakarta.annotation.Nullable;

import java.util.List;

public class PlaylistUpdateCommand {

    public static class SongId {

        private String id;
        private String songId;

        public String getId() {
            return id;
        }

        public SongId setId(String id) {
            this.id = id;
            return this;
        }

        public String getSongId() {
            return songId;
        }

        public SongId setSongId(String songId) {
            this.songId = songId;
            return this;
        }
    }

    private String id;
    private String overrideName;
    private List<SongId> overriddenSongIds;

    public String getId() {
        return id;
    }

    public PlaylistUpdateCommand setId(String id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getOverrideName() {
        return overrideName;
    }

    public PlaylistUpdateCommand setOverrideName(String overrideName) {
        this.overrideName = overrideName;
        return this;
    }

    @Nullable
    public List<SongId> getOverriddenSongIds() {
        return overriddenSongIds;
    }

    public PlaylistUpdateCommand setOverriddenSongIds(List<SongId> overriddenSongIds) {
        this.overriddenSongIds = overriddenSongIds;
        return this;
    }
}
