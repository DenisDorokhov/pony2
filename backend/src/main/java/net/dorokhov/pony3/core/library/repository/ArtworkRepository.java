package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface ArtworkRepository extends JpaRepository<Artwork, String> {

    long countByDateGreaterThan(LocalDateTime date);

    @Query("SELECT COALESCE(SUM(f.largeImageSize), 0) FROM Artwork f")
    long sumLargeImageSize();
    
    @Query("SELECT COALESCE(SUM(f.smallImageSize), 0) FROM Artwork f")
    long sumSmallImageSize();

    Artwork findByChecksumAndSourceUriScheme(String checksum, String sourceUriScheme);
}
