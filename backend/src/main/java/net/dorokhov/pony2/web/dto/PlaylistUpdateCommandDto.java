package net.dorokhov.pony2.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.dorokhov.pony2.api.library.domain.PlaylistUpdateCommand;

import java.util.List;

public class PlaylistUpdateCommandDto {

    public static class SongId {

        private String id;
        @NotNull
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

        public PlaylistUpdateCommand.SongId convert() {
            return new PlaylistUpdateCommand.SongId()
                    .setId(id)
                    .setSongId(songId);
        }
    }

    @NotNull
    private String id;

    @Size(min = 1, max = 255)
    private String overrideName;

    @Valid
    private List<SongId> overriddenSongIds;

    public String getId() {
        return id;
    }

    public PlaylistUpdateCommandDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getOverrideName() {
        return overrideName;
    }

    public PlaylistUpdateCommandDto setOverrideName(String overrideName) {
        this.overrideName = overrideName;
        return this;
    }

    @Nullable
    public List<SongId> getOverriddenSongIds() {
        return overriddenSongIds;
    }

    public PlaylistUpdateCommandDto setOverriddenSongIds(List<SongId> overriddenSongIds) {
        this.overriddenSongIds = overriddenSongIds;
        return this;
    }

    public PlaylistUpdateCommand convert() {
        return new PlaylistUpdateCommand()
                .setId(id)
                .setOverrideName(overrideName)
                .setOverriddenSongIds(overriddenSongIds != null ? overriddenSongIds.stream()
                        .map(SongId::convert)
                        .toList() : null);
    }
}
