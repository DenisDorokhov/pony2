package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ArtistDiscovery;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArtistDiscoveryRepository extends JpaRepository<ArtistDiscovery, String> {

    Optional<ArtistDiscovery> findFirstByArtistIdOrderByCreationDateDesc(String artistId);

    @Query("""
            SELECT d FROM ArtistDiscovery d
            WHERE d.artist.id = :artistId AND d.creationDate <= :maximumCreationDate
              AND d.job.status IN (COMPLETE, MODERATE)
            ORDER BY d.creationDate DESC, d.id DESC
            """)
    Optional<ArtistDiscovery> findLatestForEvaluation(
            String artistId,
            LocalDateTime maximumCreationDate,
            Limit limit
    );

    @Query("""
            SELECT t.id FROM ArtistDiscovery d JOIN d.tasks t
            WHERE d.id = :discoveryId AND t.id > :afterTaskId AND t.creationDate <= :maximumCreationDate
            ORDER BY t.id
            """)
    List<String> findTaskIdsForEvaluation(
            String discoveryId,
            String afterTaskId,
            LocalDateTime maximumCreationDate,
            Pageable pageable
    );

    void deleteByArtistId(String artistId);
}
