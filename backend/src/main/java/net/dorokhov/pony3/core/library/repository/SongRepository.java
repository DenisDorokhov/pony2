package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, String> {

    long countByGenreId(String genreId);

    long countByAlbumId(String albumId);

    long countByAlbumArtistId(String artistId);

    long countByArtworkId(@Nullable String artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByGenreIdAndArtworkNotNull(String genreId);

    @Query("SELECT COALESCE(SUM(s.size), 0) FROM Song s")
    long sumSize();

    @Query("SELECT COALESCE(SUM(s.duration), 0) FROM Song s")
    long sumDuration();

    Song findByPath(String path);

    List<Song> findByAlbumId(String albumId, Sort sort);

    List<Song> findByAlbumId(String albumId, Pageable pageable);

    List<Song> findByAlbumArtistId(String artistId, Sort sort);

    List<Song> findByAlbumArtistId(String artistId, Pageable pageable);

    Page<Song> findByGenreId(String genreId, Pageable pageable);

    Page<Song> findByGenreIdAndArtworkNotNull(String genreId, Pageable pageable);

    Song findFirstByAlbumIdAndArtworkNotNull(String albumId);

    @Modifying
    @Query("UPDATE Song s SET s.artwork = NULL WHERE s.artwork.id = ?1")
    void clearArtworkByArtworkId(String artworkId);
}
