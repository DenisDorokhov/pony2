package net.dorokhov.pony2.api.library.domain;

import com.google.common.base.MoreObjects;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import net.dorokhov.pony2.api.common.BaseEntity;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "album_discovery")
@DynamicUpdate // Update only changed columns to support concurrent discovery updates.
public class AlbumDiscovery extends BaseEntity<AlbumDiscovery> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false, unique = true)
    @NotNull
    private Album album;

    @Column(name = "music_brainz_release_group_id")
    private String musicBrainzReleaseGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_release_type")
    private ReleaseType overrideReleaseType;

    public Album getAlbum() {
        return album;
    }

    public AlbumDiscovery setAlbum(Album album) {
        this.album = album;
        return this;
    }

    public @Nullable String getMusicBrainzReleaseGroupId() {
        return musicBrainzReleaseGroupId;
    }

    public AlbumDiscovery setMusicBrainzReleaseGroupId(@Nullable String musicBrainzReleaseGroupId) {
        this.musicBrainzReleaseGroupId = musicBrainzReleaseGroupId;
        return this;
    }

    public @Nullable ReleaseType getOverrideReleaseType() {
        return overrideReleaseType;
    }

    public AlbumDiscovery setOverrideReleaseType(@Nullable ReleaseType overrideReleaseType) {
        this.overrideReleaseType = overrideReleaseType;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("creationDate", creationDate)
                .add("updateDate", updateDate)
                .add("album", album != null ? album.getId() : "null")
                .add("musicBrainzReleaseGroupId", musicBrainzReleaseGroupId)
                .add("overrideReleaseType", overrideReleaseType)
                .toString();
    }
}
