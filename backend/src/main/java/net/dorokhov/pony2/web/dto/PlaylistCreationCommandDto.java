package net.dorokhov.pony2.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.dorokhov.pony2.api.library.domain.Playlist;

import java.util.List;

public class PlaylistCreationCommandDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Playlist.Type type;

    @NotNull
    private List<String> songIds;

    public String getName() {
        return name;
    }

    public PlaylistCreationCommandDto setName(String name) {
        this.name = name;
        return this;
    }

    public Playlist.Type getType() {
        return type;
    }

    public PlaylistCreationCommandDto setType(Playlist.Type type) {
        this.type = type;
        return this;
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public PlaylistCreationCommandDto setSongIds(List<String> songIds) {
        this.songIds = songIds;
        return this;
    }
}
