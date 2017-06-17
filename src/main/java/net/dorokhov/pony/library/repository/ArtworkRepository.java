package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.library.domain.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    long countByDateGreaterThan(LocalDateTime date);

    @Query("SELECT SUM(f.largeImageSize) FROM Artwork f")
    Long sumLargeImageSize();
    
    @Query("SELECT SUM(f.smallImageSize) FROM Artwork f")
    Long sumSmallImageSize();

    Artwork findByChecksum(String checksum);
}
