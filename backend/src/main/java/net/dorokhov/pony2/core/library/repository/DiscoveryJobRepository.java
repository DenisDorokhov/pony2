package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface DiscoveryJobRepository extends JpaRepository<DiscoveryJob, String> {

    List<DiscoveryJob> findByStatusIn(Collection<DiscoveryJob.Status> statuses);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryJob j SET j.genre = NULL WHERE j.genre.id = :genreId")
    void clearGenreByGenreId(String genreId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryJob j SET j.artist = NULL WHERE j.artist.id = :artistId")
    void clearArtistByArtistId(String artistId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryJob j SET j.album = NULL WHERE j.album.id = :albumId")
    void clearAlbumByAlbumId(String albumId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE DiscoveryJob j SET j.song = NULL WHERE j.song.id = :songId")
    void clearSongBySongId(String songId);

    @Query("SELECT j.id FROM DiscoveryJob j ORDER BY j.id")
    List<String> findIdsForCleanup(Pageable pageable);
}
