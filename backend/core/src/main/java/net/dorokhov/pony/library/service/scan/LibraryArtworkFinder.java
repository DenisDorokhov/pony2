package net.dorokhov.pony.library.service.scan;

import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.artwork.ArtworkFileFinder;
import net.dorokhov.pony.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony.library.service.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony.library.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;

@Component
public class LibraryArtworkFinder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final ArtworkFileFinder artworkFileFinder;
    private final ArtworkStorage artworkStorage;
    private final LogService logService;

    LibraryArtworkFinder(GenreRepository genreRepository,
                         ArtistRepository artistRepository,
                         AlbumRepository albumRepository,
                         SongRepository songRepository,
                         ArtworkFileFinder artworkFileFinder,
                         ArtworkStorage artworkStorage,
                         LogService logService) {

        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.artworkFileFinder = artworkFileFinder;
        this.artworkStorage = artworkStorage;
        this.logService = logService;
    }

    @Transactional
    @Nullable
    public ArtworkFiles findAndSaveFileArtwork(AudioNode audioNode) throws IOException {
        ImageNode artwork = artworkFileFinder.findArtwork(audioNode);
        if (artwork != null) {
            return artworkStorage.getOrSave(new ImageNodeArtworkStorageCommand(artwork.getFile().toURI(), artwork));
        } else {
            return null;
        }
    }

    @Transactional
    @Nullable
    public ArtworkFiles findAndSaveEmbeddedArtwork(ReadableAudioData audioData) throws IOException {
        ReadableAudioData.EmbeddedArtwork artwork = audioData.getEmbeddedArtwork();
        if (artwork != null) {
            return artworkStorage.getOrSave(new ByteSourceArtworkStorageCommand(
                    UriComponentsBuilder.fromPath(audioData.getFile().getAbsolutePath())
                            .scheme(Artwork.SOURCE_URI_SCHEME_EMBEDDED)
                            .build()
                            .toUri(), 
                    artwork.getBinaryData()));
        } else {
            return null;
        }
    }

    @Transactional
    public Genre findAndSaveGenreArtwork(Genre genre) {
        long genreSongCount = songRepository.countByGenreIdAndArtworkNotNull(genre.getId());
        if (genreSongCount > 0) {
            int songIndex = (int) Math.floor(genreSongCount / 2.0);
            Page<Song> middleSongPage = songRepository.findByGenreIdAndArtworkNotNull(genre.getId(),
                    new PageRequest(songIndex, 1, Sort.Direction.ASC, "year"));
            if (middleSongPage.hasContent()) {
                Song middleSong = middleSongPage.getContent().get(0);
                Genre savedGenre = genreRepository.save(Genre.builder(genre)
                        .artwork(middleSong.getArtwork())
                        .build());
                logService.debug(logger, "Setting artwork for genre '{}': '{}'.", genre, middleSong.getArtwork());
                return savedGenre;
            }
        }
        return genre;
    }

    @Transactional
    public Album findAndSaveAlbumArtwork(Album album) {
        Song song = songRepository.findFirstByAlbumIdAndArtworkNotNull(album.getId());
        if (song != null) {
            Album savedAlbum = albumRepository.save(Album.builder(album)
                    .artwork(song.getArtwork())
                    .build());
            logService.debug(logger, "Setting artwork for album '{}': '{}'.", album, song.getArtwork());
            return savedAlbum;
        }
        return album;
    }

    @Transactional
    public Artist findAndSaveArtistArtwork(Artist artist) {
        long artistAlbumCount = albumRepository.countByArtistIdAndArtworkNotNull(artist.getId());
        if (artistAlbumCount > 0) {
            int albumIndex = (int) Math.floor(artistAlbumCount / 2.0);
            Page<Album> middleAlbum = albumRepository.findByArtistIdAndArtworkNotNull(artist.getId(),
                    new PageRequest(albumIndex, 1, Sort.Direction.ASC, "year"));
            if (middleAlbum.hasContent()) {
                Artist savedArtist = artistRepository.save(Artist.builder(artist)
                        .artwork(middleAlbum.getContent().get(0).getArtwork())
                        .build());
                logService.debug(logger, "Setting artwork for artist '{}': '{}'.", artist, middleAlbum.getContent().get(0).getArtwork());
                return savedArtist;
            }
        }
        return artist;
    }
}
