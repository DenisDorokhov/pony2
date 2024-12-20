package net.dorokhov.pony2.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.dorokhov.pony2.api.library.domain.PlaylistUpdateCommand;

import java.util.ArrayList;
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

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @Valid
    private List<SongId> songIds;

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

    public List<SongId> getSongIds() {
        return songIds;
    }

    public PlaylistUpdateCommandDto setSongIds(List<SongId> songIds) {
        this.songIds = songIds;
        return this;
    }

    public PlaylistUpdateCommand convert() {
        return new PlaylistUpdateCommand()
                .setId(id)
                .setName(name)
                .setSongIds(songIds != null ? songIds.stream()
                        .map(SongId::convert)
                        .toList() : new ArrayList<>());
    }
}
