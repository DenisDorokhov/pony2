package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumDiscoveryRepository extends JpaRepository<AlbumDiscovery, String> {

    Optional<AlbumDiscovery> findFirstByAlbumIdOrderByCreationDateDesc(String albumId);

    void deleteByAlbumId(String albumId);
}
