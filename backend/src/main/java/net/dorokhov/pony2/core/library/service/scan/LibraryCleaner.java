package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.Artist;
import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.Genre;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
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
    private final ArtistGenreRepository artistGenreRepository;
    private final ArtworkStorage artworkStorage;

    public LibraryCleaner(
            SongRepository songRepository,
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            GenreRepository genreRepository,
            ArtistGenreRepository artistGenreRepository,
            ArtworkStorage artworkStorage
    ) {
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.artistGenreRepository = artistGenreRepository;
        this.artworkStorage = artworkStorage;
    }

    @Transactional
    public boolean deleteArtistIfUnused(Artist artist) {
        if (albumRepository.countByArtistId(artist.getId()) == 0) {
            artistGenreRepository.deleteByArtistId(artist.getId());
            artistRepository.deleteById(artist.getId());
            logger.debug("Deleting artist '{}'.", artist);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteAlbumIfUnused(Album album) {
        if (songRepository.countByAlbumId(album.getId()) == 0) {
            albumRepository.deleteById(album.getId());
            logger.debug("Deleting album '{}'.", album);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteGenreIfUnused(Genre genre) {
        if (songRepository.countByGenreId(genre.getId()) == 0) {
            artistGenreRepository.deleteByGenreId(genre.getId());
            genreRepository.deleteById(genre.getId());
            logger.debug("Deleting genre '{}'.", genre);
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
            logger.debug("Artwork '{}' has been deleted.", artwork);
            return true;
        }
        return false;
    }
}
