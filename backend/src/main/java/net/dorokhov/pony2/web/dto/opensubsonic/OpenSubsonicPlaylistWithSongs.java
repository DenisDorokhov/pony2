package net.dorokhov.pony2.web.dto.opensubsonic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenSubsonicPlaylistWithSongs {

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
    private List<OpenSubsonicChild> entry;

    public String getId() {
        return id;
    }

    public OpenSubsonicPlaylistWithSongs setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public OpenSubsonicPlaylistWithSongs setName(String name) {
        this.name = name;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OpenSubsonicPlaylistWithSongs setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public OpenSubsonicPlaylistWithSongs setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public OpenSubsonicPlaylistWithSongs setPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public int getSongCount() {
        return songCount;
    }

    public OpenSubsonicPlaylistWithSongs setSongCount(int songCount) {
        this.songCount = songCount;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public OpenSubsonicPlaylistWithSongs setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicPlaylistWithSongs setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getChanged() {
        return changed;
    }

    public OpenSubsonicPlaylistWithSongs setChanged(String changed) {
        this.changed = changed;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicPlaylistWithSongs setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public List<String> getAllowedUser() {
        return allowedUser;
    }

    public OpenSubsonicPlaylistWithSongs setAllowedUser(List<String> allowedUser) {
        this.allowedUser = allowedUser;
        return this;
    }

    public List<OpenSubsonicChild> getEntry() {
        return entry;
    }

    public OpenSubsonicPlaylistWithSongs setEntry(List<OpenSubsonicChild> entry) {
        this.entry = entry;
        return this;
    }
}
