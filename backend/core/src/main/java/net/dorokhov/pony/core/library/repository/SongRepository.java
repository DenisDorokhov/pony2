package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {

    long countByGenreId(Long genreId);

    long countByAlbumId(Long albumId);

    long countByAlbumArtistId(Long artistId);

    long countByArtworkId(@Nullable Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByGenreIdAndArtworkNotNull(Long genreId);

    @Query("SELECT COALESCE(SUM(s.size), 0) FROM Song s")
    long sumSize();

    Song findByPath(String path);

    List<Song> findByAlbumId(Long albumId, Sort sort);

    List<Song> findByAlbumId(Long albumId, Pageable pageable);

    List<Song> findByAlbumArtistId(Long artistId, Sort sort);

    List<Song> findByAlbumArtistId(Long artistId, Pageable pageable);

    Page<Song> findByGenreId(Long genreId, Pageable pageable);

    Page<Song> findByGenreIdAndArtworkNotNull(Long genreId, Pageable pageable);

    Song findFirstByAlbumIdAndArtworkNotNull(Long albumId);

    @Modifying
    @Query("UPDATE Song s SET s.artwork = NULL WHERE s.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
