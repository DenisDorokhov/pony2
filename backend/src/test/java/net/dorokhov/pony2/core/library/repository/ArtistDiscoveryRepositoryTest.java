package net.dorokhov.pony2.core.library.repository;

import jakarta.persistence.EntityManagerFactory;
import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryData;
import net.dorokhov.pony2.api.library.domain.ExternalImage;
import net.dorokhov.pony2.web.dto.ArtistDiscoveryDataDto;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static net.dorokhov.pony2.test.ExternalImageFixtures.externalImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtistDiscoveryRepositoryTest extends IntegrationTest {

    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private ArtistDiscoveryRepository indexRepository;
    @Autowired
    private ExternalImageRepository externalImageRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldExposeExternalImageIdsWithoutLoadingExternalImages() {
        ExternalImage bio = externalImageRepository.save(externalImage());
        ExternalImage avatar = externalImageRepository.save(externalImage());
        ArtistDiscovery discovery = saveIndex("Artist", "spotify-artist");
        indexRepository.saveAndFlush(discovery.setBioExternalImage(bio).setAvatarExternalImage(avatar));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            getTransactionTemplate().executeWithoutResult(status -> {
                ArtistDiscovery loaded = indexRepository.findByArtistId(discovery.getArtist().getId()).orElseThrow();
                ArtistDiscoveryDataDto dto = ArtistDiscoveryDataDto.of(new ArtistDiscoveryData(
                        loaded.getCountryCode(), List.of(), loaded.getAvatarExternalImage().getId(), false, loaded.getSpotifyArtistId()));

                assertThat(loaded.getBioExternalImage().getId()).isEqualTo(bio.getId());
                assertThat(dto.getAvatarExternalImageId()).isEqualTo(avatar.getId());
                assertThat(Hibernate.isInitialized(loaded.getBioExternalImage())).isFalse();
                assertThat(Hibernate.isInitialized(loaded.getAvatarExternalImage())).isFalse();
                assertThat(statistics.getPrepareStatementCount()).isOne();
            });
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void shouldLoadArtistLazilyAndMaintainDates() {
        ArtistDiscovery index = saveIndex("Artist", "spotify-artist");
        assertThat(index.getId()).isNotEqualTo(index.getArtist().getId());
        assertThat(index.getCreationDate()).isNotNull();
        assertThat(index.getUpdateDate()).isNull();

        getTransactionTemplate().executeWithoutResult(status -> {
            ArtistDiscovery loaded = indexRepository.findById(index.getId()).orElseThrow();
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
        ArtistDiscovery first = saveIndex("First", "shared-spotify-artist");
        ArtistDiscovery second = saveIndex("Second", "shared-spotify-artist");
        ArtistDiscovery third = saveIndex("Third", "other-spotify-artist");
        saveIndex("Unknown", null);

        assertThat(indexRepository.findByArtistId(first.getArtist().getId())).get()
                .extracting(ArtistDiscovery::getId).isEqualTo(first.getId());
        assertThat(indexRepository.findAllBySpotifyArtistId("shared-spotify-artist"))
                .extracting(ArtistDiscovery::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(indexRepository.findAllBySpotifyArtistIdIn(List.of("shared-spotify-artist", "other-spotify-artist")))
                .extracting(ArtistDiscovery::getId).containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId());
        assertThat(indexRepository.findAllBySpotifyArtistId("missing")).isEmpty();
    }

    @Test
    void shouldAllowOnlyOneIndexPerArtist() {
        ArtistDiscovery index = saveIndex("Artist", "spotify-artist");

        assertThatThrownBy(() -> indexRepository.saveAndFlush(new ArtistDiscovery()
                .setArtist(index.getArtist())
                .setSpotifyArtistId("other-spotify-artist")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(indexRepository.count()).isOne();
    }

    @Test
    void shouldPreventDeletingArtistBeforeIndex() {
        ArtistDiscovery index = saveIndex("Artist", "spotify-artist");

        assertThatThrownBy(() -> artistRepository.deleteById(index.getArtist().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(indexRepository.count()).isOne();
        assertThat(artistRepository.existsById(index.getArtist().getId())).isTrue();
    }

    private ArtistDiscovery saveIndex(String name, String spotifyArtistId) {
        Artist artist = artistRepository.save(new Artist().setName(name));
        return indexRepository.save(new ArtistDiscovery().setArtist(artist).setSpotifyArtistId(spotifyArtistId));
    }
}
