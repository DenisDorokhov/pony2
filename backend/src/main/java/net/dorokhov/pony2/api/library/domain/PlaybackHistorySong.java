package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.user.domain.User;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.UUID;

@Entity
@Table(name = "playback_history_song")
public class PlaybackHistorySong {

    @Id
    @GeneratedValue(strategy = UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime creationDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pony_user_id", nullable = false)
    @NotNull
    private User user;

    public String getId() {
        return id;
    }

    public PlaybackHistorySong setId(String id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public PlaybackHistorySong setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Song getSong() {
        return song;
    }

    public PlaybackHistorySong setSong(Song song) {
        this.song = song;
        return this;
    }

    public User getUser() {
        return user;
    }

    public PlaybackHistorySong setUser(User user) {
        this.user = user;
        return this;
    }

    @PrePersist
    public void prePersist() {
        creationDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("song", song)
                .add("user", user)
                .toString();
    }
}
