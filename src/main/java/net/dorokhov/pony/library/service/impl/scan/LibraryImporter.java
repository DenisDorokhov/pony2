package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.base.Strings;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

@Component
public class LibraryImporter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final LibraryArtworkFinder libraryArtworkFinder;
    private final LibraryCleaner libraryCleaner;
    private final LogService logService;

    public LibraryImporter(GenreRepository genreRepository,
                           ArtistRepository artistRepository,
                           AlbumRepository albumRepository,
                           SongRepository songRepository,
                           LibraryArtworkFinder libraryArtworkFinder,
                           LibraryCleaner libraryCleaner,
                           LogService logService) {
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.libraryArtworkFinder = libraryArtworkFinder;
        this.libraryCleaner = libraryCleaner;
        this.logService = logService;
    }

    @Transactional
    public Song importAudioData(AudioNode audioNode, ReadableAudioData audioData) {

        Genre genre = importGenre(audioData);
        Album album = importAlbum(audioData, importArtist(audioData));
        Artwork artwork = findAndSaveArtwork(audioNode, audioData);
        Song song = songRepository.findByPath(audioNode.getFile().getAbsolutePath());

        Album overriddenAlbum = null;
        Genre overriddenGenre = null;
        Artwork overriddenArtwork = null;

        boolean shouldSave = false;

        Song.Builder builder;
        if (song != null) {
            builder = Song.builder(song);
        } else {
            builder = Song.builder().path(audioNode.getFile().getAbsolutePath());
            shouldSave = true;
        }
        if (song != null) {
            if (!Objects.equals(song.getFileType(), audioData.getFileType()) ||
                    !Objects.equals(song.getSize(), audioData.getSize()) ||
                    !Objects.equals(song.getDuration(), audioData.getDuration()) ||
                    !Objects.equals(song.getBitRate(), audioData.getBitRate()) ||
                    !Objects.equals(song.isBitRateVariable(), audioData.isBitRateVariable()) ||
                    !Objects.equals(song.getDiscNumber(), audioData.getDiscNumber()) ||
                    !Objects.equals(song.getDiscCount(), audioData.getDiscCount()) ||
                    !Objects.equals(song.getTrackNumber(), audioData.getTrackNumber()) ||
                    !Objects.equals(song.getTrackCount(), audioData.getTrackCount()) ||
                    !Objects.equals(song.getName(), audioData.getTitle()) ||
                    !Objects.equals(song.getGenreName(), audioData.getGenre()) ||
                    !Objects.equals(song.getArtistName(), audioData.getArtist()) ||
                    !Objects.equals(song.getAlbumArtistName(), audioData.getAlbumArtist()) ||
                    !Objects.equals(song.getAlbumName(), audioData.getAlbum()) ||
                    !Objects.equals(song.getYear(), audioData.getYear())) {
                shouldSave = true;
            }
            if (!Objects.equals(song.getGenre(), genre)) {
                overriddenGenre = song.getGenre();
                shouldSave = true;
            }
            if (!Objects.equals(song.getAlbum(), album)) {
                overriddenAlbum = song.getAlbum();
                shouldSave = true;
            }
            if (!Objects.equals(song.getArtwork(), artwork)) {
                overriddenArtwork = song.getArtwork();
                shouldSave = true;
            }
        }
        if (shouldSave) {

            builder.fileType(audioData.getFileType())
                    .size(audioData.getSize())
                    .duration(audioData.getDuration())
                    .bitRate(audioData.getBitRate())
                    .bitRateVariable(audioData.isBitRateVariable())
                    .discNumber(audioData.getDiscNumber())
                    .discCount(audioData.getDiscCount())
                    .trackNumber(audioData.getTrackNumber())
                    .trackCount(audioData.getTrackCount())
                    .name(audioData.getTitle())
                    .genreName(audioData.getGenre())
                    .artistName(audioData.getArtist())
                    .albumArtistName(audioData.getAlbumArtist())
                    .albumName(audioData.getAlbum())
                    .year(audioData.getYear())
                    .album(album)
                    .genre(genre)
                    .artwork(artwork);

            Song savedSong = songRepository.save(builder.build());
            if (song != null) {
                logService.debug(logger, "Updating song '{}': '{}'.", song, savedSong);
            } else {
                logService.debug(logger, "Creating song '{}'.", savedSong);
            }
            if (overriddenAlbum != null) {
                libraryCleaner.deleteAlbumIfUnused(overriddenAlbum);
                libraryCleaner.deleteArtistIfUnused(overriddenAlbum.getArtist());
            }
            if (overriddenGenre != null) {
                libraryCleaner.deleteGenreIfUnused(overriddenGenre);
            }
            if (overriddenArtwork != null &&
                    libraryCleaner.deleteArtworkIfUnused(overriddenArtwork) &&
                    Objects.equals(album.getArtwork(), overriddenArtwork)) {
                albumRepository.save(Album.builder(album)
                        .artwork(null)
                        .build());
            }
            if (artwork != null && album.getArtwork() == null) {
                importAlbumArtwork(album, artwork);
            }
            return savedSong;
        }
        return song;
    }

    @Transactional
    @Nullable
    public Song importArtwork(AudioNode audioNode) {
        Song song = songRepository.findByPath(audioNode.getFile().getAbsolutePath());
        if (song != null) {
            if (song.getArtwork() == null) {
                Artwork artwork = findAndSaveFileArtwork(audioNode);
                if (artwork != null) {
                    song = songRepository.save(Song.builder(song)
                            .artwork(artwork)
                            .build());
                }
            }
            if (song.getArtwork() != null && song.getAlbum().getArtwork() == null) {
                importAlbumArtwork(song.getAlbum(), song.getArtwork());
            }
        }
        return song;
    }

    private Genre importGenre(ReadableAudioData audioData) {

        String genreName = normalizeTitle(audioData.getGenre());
        Genre genre = genreRepository.findByName(genreName);

        boolean shouldSave = false;

        Genre.Builder builder;
        if (genre != null) {
            builder = Genre.builder(genre);
        } else {
            builder = Genre.builder();
            shouldSave = true;
        }
        if (genre != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(genre.getName(), genreName)) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Genre savedGenre = genreRepository.save(builder
                    .name(genreName)
                    .build());
            if (genre != null) {
                logService.debug(logger, "Updating genre '{}': '{}'.", genre, savedGenre);
            } else {
                logService.debug(logger, "Creating genre '{}'.", savedGenre);
            }
            return savedGenre;
        }
        return genre;
    }

    private Artist importArtist(ReadableAudioData audioData) {

        String artistName = normalizeTitle(audioData.getAlbumArtist());
        if (artistName == null) {
            artistName = normalizeTitle(audioData.getArtist());
        }
        Artist artist = artistRepository.findByName(artistName);

        boolean shouldSave = false;

        Artist.Builder builder;
        if (artist != null) {
            builder = Artist.builder(artist);
        } else {
            builder = Artist.builder();
            shouldSave = true;
        }
        if (artist != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(artist.getName(), artistName)) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Artist savedArtist = artistRepository.save(builder
                    .name(artistName)
                    .build());
            if (artist != null) {
                logService.debug(logger, "Updating artist '{}': '{}'.", artist, savedArtist);
            } else {
                logService.debug(logger, "Creating artist '{}'.", savedArtist);
            }
            return savedArtist;
        }
        return artist;
    }

    private Album importAlbum(ReadableAudioData audioData, Artist artist) {

        String albumName = normalizeTitle(audioData.getAlbum());
        Album album = albumRepository.findByArtistIdAndName(artist.getId(), albumName);

        boolean shouldSave = false;

        Album.Builder builder;
        if (album != null) {
            builder = Album.builder(album);
        } else {
            builder = Album.builder();
            shouldSave = true;
        }
        if (album != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(album.getName(), albumName) ||
                    !Objects.equals(album.getYear(), audioData.getYear())) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Album savedAlbum = albumRepository.save(builder
                    .artist(artist)
                    .name(albumName)
                    .year(audioData.getYear())
                    .build());
            if (album != null) {
                logService.debug(logger, "Updating artist '{}': '{}'.", album, savedAlbum);
            } else {
                logService.debug(logger, "Creating artist '{}'.", savedAlbum);
            }
            return savedAlbum;
        }
        return album;
    }

    private Album importAlbumArtwork(Album album, Artwork artwork) {
        logService.debug(logger, "Setting artwork for album '{}': '{}'.", album, artwork);
        return albumRepository.save(Album.builder(album)
                .artwork(artwork)
                .build());
    }

    @Nullable
    private Artwork findAndSaveArtwork(AudioNode audioNode, ReadableAudioData audioData) {
        Artwork artwork = findAndSaveEmbeddedArtwork(audioData);
        if (artwork == null) {
            artwork = findAndSaveFileArtwork(audioNode);
        }
        return artwork;
    }

    @Nullable
    private Artwork findAndSaveEmbeddedArtwork(ReadableAudioData audioData) {
        try {
            return libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData);
        } catch (IOException e) {
            logService.error(logger, "Could not find and save embedded artwork for data '{}'.",
                    audioData, e);
        }
        return null;
    }

    @Nullable
    private Artwork findAndSaveFileArtwork(AudioNode audioNode) {
        try {
            return libraryArtworkFinder.findAndSaveFileArtwork(audioNode);
        } catch (IOException e) {
            logService.error(logger, "Could not find and save file artwork for file '{}'.",
                    audioNode.getFile().getAbsolutePath(), e);
        }
        return null;
    }

    @Nullable
    private String normalizeTitle(@Nullable String title) {
        if (title != null) {
            return Strings.emptyToNull(title.trim().replaceAll("\\s+", " "));
        } else {
            return null;
        }
    }
}
