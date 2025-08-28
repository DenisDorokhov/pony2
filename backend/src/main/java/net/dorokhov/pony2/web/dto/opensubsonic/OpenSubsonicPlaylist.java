package net.dorokhov.pony2.web.dto.opensubsonic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenSubsonicPlaylist {

    private String id;
    private String name;
    private String comment;
    private String owner;
    @JsonProperty("public")
    private Boolean isPublic;
    private int songCount;
    private int duration;
    private String created;
    private String changed;
    private String coverArt;
    private List<String> allowedUser;

    public String getId() {
        return id;
    }

    public OpenSubsonicPlaylist setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicPlaylist setName(String name) {
        this.name = name;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OpenSubsonicPlaylist setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public OpenSubsonicPlaylist setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public OpenSubsonicPlaylist setPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public int getSongCount() {
        return songCount;
    }

    public OpenSubsonicPlaylist setSongCount(int songCount) {
        this.songCount = songCount;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public OpenSubsonicPlaylist setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicPlaylist setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getChanged() {
        return changed;
    }

    public OpenSubsonicPlaylist setChanged(String changed) {
        this.changed = changed;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicPlaylist setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public List<String> getAllowedUser() {
        return allowedUser;
    }

    public OpenSubsonicPlaylist setAllowedUser(List<String> allowedUser) {
        this.allowedUser = allowedUser;
        return this;
    }
}
