package net.dorokhov.pony2.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.dorokhov.pony2.api.library.domain.Playlist;

import java.util.List;

public class PlaylistUpdateCommandDto {

    public static class PlaylistSongId {

        private String id;
        @NotNull
        private String songId;

        public String getId() {
            return id;
        }

        public PlaylistSongId setId(String id) {
            this.id = id;
            return this;
        }

        public String getSongId() {
            return songId;
        }

        public PlaylistSongId setSongId(String songId) {
            this.songId = songId;
            return this;
        }
    }

    @NotNull
    private String id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Playlist.Type type;

    @NotNull
    @Valid
    private List<PlaylistSongId> songIds;

    public String getId() {
        return id;
    }

    public PlaylistUpdateCommandDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PlaylistUpdateCommandDto setName(String name) {
        this.name = name;
        return this;
    }

    public Playlist.Type getType() {
        return type;
    }

    public PlaylistUpdateCommandDto setType(Playlist.Type type) {
        this.type = type;
        return this;
    }

    public List<PlaylistSongId> getSongIds() {
        return songIds;
    }

    public PlaylistUpdateCommandDto setSongIds(List<PlaylistSongId> songIds) {
        this.songIds = songIds;
        return this;
    }
}
