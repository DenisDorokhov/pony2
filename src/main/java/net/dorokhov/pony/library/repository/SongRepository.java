package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.library.domain.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

public interface SongRepository extends PagingAndSortingRepository<Song, Long> {

    long countByGenreId(Long genreId);

    long countByAlbumId(Long albumId);

    long countByAlbumArtistId(Long artistId);

    long countByArtworkId(@Nullable Long artworkId);

    long countByCreationDateGreaterThan(LocalDateTime date);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime creationDate, LocalDateTime updateDate);

    long countByGenreIdAndArtworkNotNull(Long genreId);

    long countByAlbumIdAndArtworkNotNull(Long genreId);

    @Query("SELECT SUM(s.size) FROM Song s")
    Long sumSize();

    Song findByPath(String path);

    List<Song> findByAlbumId(Long albumId, Sort sort);

    List<Song> findByAlbumArtistId(Long artistId, Sort sort);

    Page<Song> findByAlbumArtistId(Long artistId, Pageable pageable);

    Page<Song> findByGenreIdAndArtworkNotNull(Long genreId, Pageable pageable);

    Page<Song> findByAlbumIdAndArtworkNotNull(Long genreId, Pageable pageable);

    @Modifying
    @Query("UPDATE Song s SET s.artwork = NULL WHERE s.artwork.id = ?1")
    void clearArtworkByArtworkId(Long artworkId);
}
