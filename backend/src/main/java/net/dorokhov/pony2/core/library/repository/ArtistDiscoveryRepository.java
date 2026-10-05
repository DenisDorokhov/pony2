package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistDiscoveryRepository extends JpaRepository<ArtistDiscovery, String> {

    Optional<ArtistDiscovery> findFirstByArtistIdOrderByCreationDateDesc(String artistId);

    void deleteByArtistId(String artistId);
}
