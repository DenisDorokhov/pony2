package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LibraryCleaner {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final ArtworkStorage artworkStorage;
    private final LogService logService;

    public LibraryCleaner(SongRepository songRepository,
                          AlbumRepository albumRepository,
                          ArtistRepository artistRepository,
                          GenreRepository genreRepository,
                          ArtworkStorage artworkStorage,
                          LogService logService) {
        
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.artworkStorage = artworkStorage;
        this.logService = logService;
    }

    @Transactional
    public boolean deleteArtistIfUnused(Artist artist) {
        if (albumRepository.countByArtistId(artist.getId()) == 0) {
            artistRepository.delete(artist.getId());
            logService.debug(logger, "Artist '{}' has been deleted.", artist);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteAlbumIfUnused(Album album) {
        if (songRepository.countByAlbumId(album.getId()) == 0) {
            albumRepository.delete(album.getId());
            logService.debug(logger, "Album '{}' has been deleted.", album);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteGenreIfUnused(Genre genre) {
        if (songRepository.countByGenreId(genre.getId()) == 0) {
            genreRepository.delete(genre.getId());
            logService.debug(logger, "Genre '{}' has been deleted.", genre);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteArtworkIfUnused(Artwork artwork) {
        if (songRepository.countByArtworkId(artwork.getId()) == 0) {
            songRepository.clearArtworkByArtworkId(artwork.getId());
            albumRepository.clearArtworkByArtworkId(artwork.getId());
            artistRepository.clearArtworkByArtworkId(artwork.getId());
            genreRepository.clearArtworkByArtworkId(artwork.getId());
            artworkStorage.delete(artwork.getId());
            logService.debug(logger, "Artwork '{}' has been deleted.", artwork);
            return true;
        }
        return false;
    }
}
