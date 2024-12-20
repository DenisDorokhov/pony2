package net.dorokhov.pony2.api.library.domain;

import java.util.ArrayList;
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
    private String name;
    private List<SongId> songIds;

    public String getId() {
        return id;
    }

    public PlaylistUpdateCommand setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PlaylistUpdateCommand setName(String name) {
        this.name = name;
        return this;
    }

    public List<SongId> getSongIds() {
        if (songIds == null) {
            songIds = new ArrayList<>();
        }
        return songIds;
    }

    public PlaylistUpdateCommand setSongIds(List<SongId> songIds) {
        this.songIds = songIds;
        return this;
    }
}
