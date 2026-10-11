package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryIndex;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtistDiscoveryIndexRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private ArtistDiscoveryIndexRepository indexRepository;

    @Test
    void shouldLoadArtistLazilyAndMaintainDates() {
        ArtistDiscoveryIndex index = saveIndex("Artist", "spotify-artist");
        assertThat(index.getId()).isNotEqualTo(index.getArtist().getId());
        assertThat(index.getCreationDate()).isNotNull();
        assertThat(index.getUpdateDate()).isNull();

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscoveryIndex loaded = indexRepository.findById(index.getId()).orElseThrow();
            assertThat(Hibernate.isInitialized(loaded.getArtist())).isFalse();
            assertThat(loaded.getArtist().getName()).isEqualTo("Artist");
            assertThat(Hibernate.isInitialized(loaded.getArtist())).isTrue();
        });

        indexRepository.save(index.setSpotifyArtistId("updated-spotify-artist"));

        assertThat(indexRepository.findById(index.getId())).get().satisfies(updated -> {
            assertThat(updated.getCreationDate()).isEqualTo(index.getCreationDate());
            assertThat(updated.getUpdateDate()).isNotNull();
            assertThat(updated.getSpotifyArtistId()).isEqualTo("updated-spotify-artist");
        });
    }

    @Test
    void shouldFindByArtistAndSpotifyArtistIds() {
        ArtistDiscoveryIndex first = saveIndex("First", "shared-spotify-artist");
        ArtistDiscoveryIndex second = saveIndex("Second", "shared-spotify-artist");
        ArtistDiscoveryIndex third = saveIndex("Third", "other-spotify-artist");
        saveIndex("Unknown", null);

        assertThat(indexRepository.findByArtistId(first.getArtist().getId())).get()
                .extracting(ArtistDiscoveryIndex::getId).isEqualTo(first.getId());
        assertThat(indexRepository.findAllBySpotifyArtistId("shared-spotify-artist"))
                .extracting(ArtistDiscoveryIndex::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(indexRepository.findAllBySpotifyArtistIdIn(List.of("shared-spotify-artist", "other-spotify-artist")))
                .extracting(ArtistDiscoveryIndex::getId).containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId());
        assertThat(indexRepository.findAllBySpotifyArtistId("missing")).isEmpty();
    }

    @Test
    void shouldAllowOnlyOneIndexPerArtist() {
        ArtistDiscoveryIndex index = saveIndex("Artist", "spotify-artist");

        assertThatThrownBy(() -> indexRepository.saveAndFlush(new ArtistDiscoveryIndex()
                .setArtist(index.getArtist())
                .setSpotifyArtistId("other-spotify-artist")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(indexRepository.count()).isOne();
    }

    @Test
    void shouldPreventDeletingArtistBeforeIndex() {
        ArtistDiscoveryIndex index = saveIndex("Artist", "spotify-artist");

        assertThatThrownBy(() -> artistRepository.deleteById(index.getArtist().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(indexRepository.count()).isOne();
        assertThat(artistRepository.existsById(index.getArtist().getId())).isTrue();
    }

    private ArtistDiscoveryIndex saveIndex(String name, String spotifyArtistId) {
        Artist artist = artistRepository.save(new Artist().setName(name));
        return indexRepository.save(new ArtistDiscoveryIndex().setArtist(artist).setSpotifyArtistId(spotifyArtistId));
    }
}
