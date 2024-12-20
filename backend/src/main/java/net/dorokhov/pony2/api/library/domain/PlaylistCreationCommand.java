package net.dorokhov.pony2.api.library.domain;

import java.util.ArrayList;
import java.util.List;

public class PlaylistCreationCommand {

    String name;
    String userId;
    List<String> songIds;

    public String getName() {
        return name;
    }

    public PlaylistCreationCommand setName(String name) {
        this.name = name;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public PlaylistCreationCommand setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getSongIds() {
        if (songIds == null) {
            songIds = new ArrayList<>();
        }
        return songIds;
    }

    public PlaylistCreationCommand setSongIds(List<String> songIds) {
        this.songIds = songIds;
        return this;
    }
}
