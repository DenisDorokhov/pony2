package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "playlist_song")
public class PlaylistSong {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime creationDate;

    @Column(name = "sort", nullable = false)
    private int sort;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    @NotNull
    private Playlist playlist;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    public String getId() {
        return id;
    }

    public PlaylistSong setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public PlaylistSong setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public int getSort() {
        return sort;
    }

    public PlaylistSong setSort(int sort) {
        this.sort = sort;
        return this;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public PlaylistSong setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        return this;
    }

    public Song getSong() {
        return song;
    }

    public PlaylistSong setSong(Song song) {
        this.song = song;
        return this;
    }

    @PrePersist
    public void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("sort", sort)
                .add("playlist", playlist)
                .add("song", song)
                .toString();
    }
}
