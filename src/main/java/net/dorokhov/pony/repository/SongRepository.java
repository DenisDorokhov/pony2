package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SongRepository extends PagingAndSortingRepository<Song, Long> {

    long countByGenreId(Long aGenreId);

    long countByAlbumId(Long aAlbumId);

    long countByAlbumArtistId(Long aArtistId);

    long countByArtworkId(Long aStoredFileId);

    long countByCreationDateGreaterThan(LocalDateTime aDate);

    long countByCreationDateLessThanAndUpdateDateGreaterThan(LocalDateTime aCreationDate, LocalDateTime aUpdateDate);

    long countByGenreIdAndArtworkNotNull(Long aGenreId);

    long countByAlbumIdAndArtworkNotNull(Long aGenreId);

    @Query("SELECT SUM(s.size) FROM Song s")
    Long sumSize();

    Song findByPath(String aPath);

    List<Song> findByAlbumId(Long aAlbumId, Sort aSort);

    List<Song> findByAlbumArtistId(Long aArtistId, Sort aSort);

    Page<Song> findByAlbumArtistId(Long aArtistId, Pageable aPageable);

    Page<Song> findByGenreIdAndArtworkNotNull(Long aGenreId, Pageable aPageable);

    Page<Song> findByAlbumIdAndArtworkNotNull(Long aGenreId, Pageable aPageable);

    @Modifying
    @Query("UPDATE Song s SET s.artwork = NULL WHERE s.artwork.id = ?1")
    void clearArtworkByArtworkId(Long aStoredFileId);
}
