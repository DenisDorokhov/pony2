package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.Playlist;

public class PlaylistDto extends BaseDto<PlaylistDto> {

    private String name;
    private Playlist.Type type;

    public String getName() {
        return name;
    }

    public PlaylistDto setName(String name) {
        this.name = name;
        return this;
    }

    public Playlist.Type getType() {
        return type;
    }

    public PlaylistDto setType(Playlist.Type type) {
        this.type = type;
        return this;
    }

    public static PlaylistDto of(Playlist playlist) {
        return new PlaylistDto()
                .setId(playlist.getId())
                .setCreationDate(playlist.getCreationDate())
                .setUpdateDate(playlist.getUpdateDate())
                .setName(playlist.getName())
                .setType(playlist.getType());
    }
}
