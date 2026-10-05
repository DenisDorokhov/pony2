package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.AlbumDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumDiscoveryRepository extends JpaRepository<AlbumDiscovery, String> {

    void deleteByAlbumId(String albumId);
}
