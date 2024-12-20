package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import net.dorokhov.pony2.api.user.domain.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlist")
public class Playlist extends BaseEntity<Playlist> {

    public enum Type {
        NORMAL,
        LIKE,
        HISTORY,
    }

    @Column(name = "name")
    private String name;

    @Column(name = "type", nullable = false)
    private Type type;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "playlist",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sort")
    private List<PlaylistSong> songs = new ArrayList<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pony_user_id", nullable = false)
    @NotNull
    private User user;

    public String getName() {
        return name;
    }

    public Playlist setName(String name) {
        this.name = name;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Playlist setType(Type type) {
        this.type = type;
        return this;
    }

    public List<PlaylistSong> getSongs() {
        return songs;
    }

    public Playlist setSongs(List<PlaylistSong> songs) {
        this.songs = songs;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Playlist setUser(User user) {
        this.user = user;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("user", user)
                .toString();
    }
}
