package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ArtistDiscoveryIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistDiscoveryIndexRepository extends JpaRepository<ArtistDiscoveryIndex, String> {

    Optional<ArtistDiscoveryIndex> findByArtistId(String artistId);

    List<ArtistDiscoveryIndex> findAllBySpotifyArtistId(String spotifyArtistId);

    List<ArtistDiscoveryIndex> findAllBySpotifyArtistIdIn(List<String> spotifyArtistIds);

    void deleteByArtistId(String artistId);
}
