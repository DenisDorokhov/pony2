package net.dorokhov.pony3.core.library.service.scan;

import com.google.common.base.Strings;
import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.core.library.repository.AlbumRepository;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import net.dorokhov.pony3.core.library.repository.SongRepository;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony3.api.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

import static net.dorokhov.pony3.api.library.domain.Artwork.SOURCE_URI_SCHEME_EMBEDDED;

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

    public LibraryImporter(
            GenreRepository genreRepository,
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            SongRepository songRepository,
            LibraryArtworkFinder libraryArtworkFinder,
            LibraryCleaner libraryCleaner,
            LogService logService
    ) {
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
        Song existingSong = songRepository.findByPath(audioNode.getFile().getAbsolutePath());

        ArtworkFiles artworkFiles = findAndSaveArtwork(audioNode, audioData);
        Artwork artwork = artworkFiles != null ? artworkFiles.getArtwork() : null;

        Album overriddenAlbum = null;
        Genre overriddenGenre = null;
        Artwork overriddenArtwork = null;

        boolean shouldSave = false;

        Song songToSave;
        if (existingSong != null) {
            songToSave = existingSong;
        } else {
            songToSave = new Song().setPath(audioNode.getFile().getAbsolutePath());
            shouldSave = true;
        }
        if (existingSong != null) {
            if (!Objects.equals(existingSong.getFileType(), audioData.getFileType()) ||
                    !Objects.equals(existingSong.getSize(), audioData.getSize()) ||
                    !Objects.equals(existingSong.getDuration(), audioData.getDuration()) ||
                    !Objects.equals(existingSong.getBitRate(), audioData.getBitRate()) ||
                    !Objects.equals(existingSong.getBitRateVariable(), audioData.isBitRateVariable()) ||
                    !Objects.equals(existingSong.getDiscNumber(), audioData.getDiscNumber()) ||
                    !Objects.equals(existingSong.getDiscCount(), audioData.getDiscCount()) ||
                    !Objects.equals(existingSong.getTrackNumber(), audioData.getTrackNumber()) ||
                    !Objects.equals(existingSong.getTrackCount(), audioData.getTrackCount()) ||
                    !Objects.equals(existingSong.getName(), audioData.getTitle()) ||
                    !Objects.equals(existingSong.getGenreName(), audioData.getGenre()) ||
                    !Objects.equals(existingSong.getArtistName(), audioData.getArtist()) ||
                    !Objects.equals(existingSong.getAlbumArtistName(), audioData.getAlbumArtist()) ||
                    !Objects.equals(existingSong.getAlbumName(), audioData.getAlbum()) ||
                    !Objects.equals(existingSong.getYear(), audioData.getYear())) {
                shouldSave = true;
            }
            if (!Objects.equals(existingSong.getGenre(), genre)) {
                overriddenGenre = existingSong.getGenre();
                shouldSave = true;
            }
            if (!Objects.equals(existingSong.getAlbum(), album)) {
                overriddenAlbum = existingSong.getAlbum();
                shouldSave = true;
            }
            if (!Objects.equals(existingSong.getArtwork(), artwork)) {
                overriddenArtwork = existingSong.getArtwork();
                shouldSave = true;
            }
        }
        if (shouldSave) {

            songToSave
                    .setFileType(audioData.getFileType())
                    .setSize(audioData.getSize())
                    .setDuration(audioData.getDuration())
                    .setBitRate(audioData.getBitRate())
                    .setBitRateVariable(audioData.isBitRateVariable())
                    .setDiscNumber(audioData.getDiscNumber())
                    .setDiscCount(audioData.getDiscCount())
                    .setTrackNumber(audioData.getTrackNumber())
                    .setTrackCount(audioData.getTrackCount())
                    .setName(audioData.getTitle())
                    .setGenreName(audioData.getGenre())
                    .setArtistName(audioData.getArtist())
                    .setAlbumArtistName(audioData.getAlbumArtist())
                    .setAlbumName(audioData.getAlbum())
                    .setYear(audioData.getYear())
                    .setAlbum(album)
                    .setGenre(genre)
                    .setArtwork(artwork);

            Song savedSong = songRepository.save(songToSave);
            if (existingSong != null) {
                logService.debug(logger, "Updating song '{}': '{}'.", existingSong, savedSong);
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
                albumRepository.save(album
                        .setArtwork(null));
            }
            if (shouldImportAlbumArtwork(album, artwork)) {
                importAlbumArtwork(album, artwork);
            }
            return savedSong;
        }
        return existingSong;
    }

    private boolean shouldImportAlbumArtwork(Album album, Artwork artwork) {
        if (artwork == null) {
            return false;
        }
        if (album.getArtwork() == null) {
            return true;
        }
        // Embedded artwork is always preferred and will override any other artwork type.
        return album.getArtwork() != null &&
                !Objects.equals(album.getArtwork().getSourceUriScheme(), SOURCE_URI_SCHEME_EMBEDDED) &&
                Objects.equals(artwork.getSourceUriScheme(), SOURCE_URI_SCHEME_EMBEDDED);
    }

    @Transactional
    @Nullable
    public Song importArtwork(AudioNode audioNode) {
        Song song = songRepository.findByPath(audioNode.getFile().getAbsolutePath());
        if (song != null) {
            if (song.getArtwork() == null) {
                ArtworkFiles artworkFiles = findAndSaveFileArtwork(audioNode);
                if (artworkFiles != null) {
                    song = songRepository.save(song
                            .setArtwork(artworkFiles.getArtwork()));
                }
            }
            if (shouldImportAlbumArtwork(song.getAlbum(), song.getArtwork())) {
                importAlbumArtwork(song.getAlbum(), song.getArtwork());
            }
        }
        return song;
    }

    private Genre importGenre(ReadableAudioData audioData) {

        String genreName = normalizeTitle(audioData.getGenre());
        Genre existingGenre = genreRepository.findByName(genreName);

        boolean shouldSave = false;

        Genre genreToSave;
        if (existingGenre != null) {
            genreToSave = existingGenre;
        } else {
            genreToSave = new Genre();
            shouldSave = true;
        }
        if (existingGenre != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(existingGenre.getName(), genreName)) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Genre savedGenre = genreRepository.save(genreToSave
                    .setName(genreName));
            if (existingGenre != null) {
                logService.debug(logger, "Updating genre '{}': '{}'.", existingGenre, savedGenre);
            } else {
                logService.debug(logger, "Creating genre '{}'.", savedGenre);
            }
            return savedGenre;
        }
        return existingGenre;
    }

    private Artist importArtist(ReadableAudioData audioData) {

        String artistName = normalizeTitle(audioData.getAlbumArtist());
        if (artistName == null) {
            artistName = normalizeTitle(audioData.getArtist());
        }
        Artist existingArtist = artistRepository.findByName(artistName);

        boolean shouldSave = false;

        Artist artistToSave;
        if (existingArtist != null) {
            artistToSave = existingArtist;
        } else {
            artistToSave = new Artist();
            shouldSave = true;
        }
        if (existingArtist != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(existingArtist.getName(), artistName)) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Artist savedArtist = artistRepository.save(artistToSave
                    .setName(artistName));
            if (existingArtist != null) {
                logService.debug(logger, "Updating artist '{}': '{}'.", existingArtist, savedArtist);
            } else {
                logService.debug(logger, "Creating artist '{}'.", savedArtist);
            }
            return savedArtist;
        }
        return existingArtist;
    }

    private Album importAlbum(ReadableAudioData audioData, Artist artist) {

        String albumName = normalizeTitle(audioData.getAlbum());
        Album existingAlbum = albumRepository.findByArtistIdAndName(artist.getId(), albumName);

        boolean shouldSave = false;

        Album albumToSave;
        if (existingAlbum != null) {
            albumToSave = existingAlbum;
        } else {
            albumToSave = new Album();
            shouldSave = true;
        }
        if (existingAlbum != null) {
            // Compare name to detect case changes ignored by database selection.
            if (!Objects.equals(existingAlbum.getName(), albumName) ||
                    !Objects.equals(existingAlbum.getYear(), audioData.getYear())) {
                shouldSave = true;
            }
        }
        if (shouldSave) {
            Album savedAlbum = albumRepository.save(albumToSave
                    .setArtist(artist)
                    .setName(albumName)
                    .setYear(audioData.getYear()));
            if (existingAlbum != null) {
                logService.debug(logger, "Updating album '{}': '{}'.", existingAlbum, savedAlbum);
            } else {
                logService.debug(logger, "Creating album '{}'.", savedAlbum);
            }
            return savedAlbum;
        }
        return existingAlbum;
    }

    private Album importAlbumArtwork(Album album, Artwork artwork) {
        logService.debug(logger, "Setting artwork for album '{}': '{}'.", album, artwork);
        return albumRepository.save(album
                .setArtwork(artwork));
    }

    @Nullable
    private ArtworkFiles findAndSaveArtwork(AudioNode audioNode, ReadableAudioData audioData) {
        ArtworkFiles artworkFiles = findAndSaveEmbeddedArtwork(audioData);
        if (artworkFiles == null) {
            artworkFiles = findAndSaveFileArtwork(audioNode);
        }
        return artworkFiles;
    }

    @Nullable
    private ArtworkFiles findAndSaveEmbeddedArtwork(ReadableAudioData audioData) {
        try {
            return libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData);
        } catch (IOException e) {
            logService.error(logger, "Could not find and save embedded artwork for data '{}'.",
                    audioData, e);
        }
        return null;
    }

    @Nullable
    private ArtworkFiles findAndSaveFileArtwork(AudioNode audioNode) {
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
