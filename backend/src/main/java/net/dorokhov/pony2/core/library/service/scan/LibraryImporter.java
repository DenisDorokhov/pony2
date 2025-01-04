package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.dorokhov.pony2.api.library.domain.Artwork.SOURCE_URI_SCHEME_EMBEDDED;

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

        List<String> saveReasons = new ArrayList<>();

        Song songToSave;
        if (existingSong != null) {
            songToSave = existingSong;
        } else {
            songToSave = new Song().setPath(audioNode.getFile().getAbsolutePath());
            saveReasons.add("Song does not exist.");
        }
        if (existingSong != null) {
            if (!Objects.equals(existingSong.getFileType(), audioData.getFileType())) {
                saveReasons.add("File type changed from '%s' to '%s'.".formatted(existingSong.getFileType(), audioData.getFileType()));
            }
            if (!Objects.equals(existingSong.getSize(), audioData.getSize())) {
                saveReasons.add("File size changed from '%s' to '%s'.".formatted(existingSong.getSize(), audioData.getSize()));
            }
            if (!Objects.equals(existingSong.getDuration(), audioData.getDuration())) {
                saveReasons.add("Duration changed from '%s' to '%s'.".formatted(existingSong.getDuration(), audioData.getDuration()));
            }
            if (!Objects.equals(existingSong.getBitRate(), audioData.getBitRate())) {
                saveReasons.add("Bit rate changed from '%s' to '%s'.".formatted(existingSong.getBitRate(), audioData.getBitRate()));
            }
            if (!Objects.equals(existingSong.getBitRateVariable(), audioData.isBitRateVariable())) {
                saveReasons.add("Bit rate variable changed from '%s' to '%s'.".formatted(existingSong.getBitRateVariable(), audioData.isBitRateVariable()));
            }
            if (!Objects.equals(existingSong.getDiscNumber(), audioData.getDiscNumber())) {
                saveReasons.add("Disc number changed from '%s' to '%s'.".formatted(existingSong.getDiscNumber(), audioData.getDiscNumber()));
            }
            if (!Objects.equals(existingSong.getDiscCount(), audioData.getDiscCount())) {
                saveReasons.add("Disc count changed from '%s' to '%s'.".formatted(existingSong.getDiscCount(), audioData.getDiscCount()));
            }
            if (!Objects.equals(existingSong.getTrackNumber(), audioData.getTrackNumber())) {
                saveReasons.add("Track number changed from '%s' to '%s'.".formatted(existingSong.getTrackNumber(), audioData.getTrackNumber()));
            }
            if (!Objects.equals(existingSong.getTrackCount(), audioData.getTrackCount())) {
                saveReasons.add("Track count changed from '%s' to '%s'.".formatted(existingSong.getTrackCount(), audioData.getTrackCount()));
            }
            if (!Objects.equals(existingSong.getName(), audioData.getTitle())) {
                saveReasons.add("Name changed from '%s' to '%s'.".formatted(existingSong.getName(), audioData.getTitle()));
            }
            if (!Objects.equals(existingSong.getGenreName(), audioData.getGenre())) {
                saveReasons.add("Genre name changed from '%s' to '%s'.".formatted(existingSong.getGenreName(), audioData.getGenre()));
            }
            if (!Objects.equals(existingSong.getArtistName(), audioData.getArtist())) {
                saveReasons.add("Artist name changed from '%s' to '%s'.".formatted(existingSong.getArtistName(), audioData.getArtist()));
            }
            if (!Objects.equals(existingSong.getAlbumArtistName(), audioData.getAlbumArtist())) {
                saveReasons.add("Album artist name changed from '%s' to '%s'.".formatted(existingSong.getAlbumArtistName(), audioData.getAlbumArtist()));
            }
            if (!Objects.equals(existingSong.getAlbumName(), audioData.getAlbum())) {
                saveReasons.add("Album name changed from '%s' to '%s'.".formatted(existingSong.getAlbumName(), audioData.getAlbum()));
            }
            if (!Objects.equals(existingSong.getYear(), audioData.getYear())) {
                saveReasons.add("Year changed from '%s' to '%s'.".formatted(existingSong.getYear(), audioData.getYear()));
            }
            if (!Objects.equals(existingSong.getGenre(), genre)) {
                overriddenGenre = existingSong.getGenre();
                saveReasons.add("Genre changed from '%s' to '%s'.".formatted(existingSong.getGenre(), genre));
            }
            if (!Objects.equals(existingSong.getAlbum(), album)) {
                overriddenAlbum = existingSong.getAlbum();
                saveReasons.add("Album changed from '%s' to '%s'.".formatted(existingSong.getAlbum(), album));
            }
            if (!Objects.equals(existingSong.getArtwork(), artwork)) {
                overriddenArtwork = existingSong.getArtwork();
                saveReasons.add("Artwork changed from '%s' to '%s'.".formatted(existingSong.getArtwork(), artwork));
            }
        }
        if (!saveReasons.isEmpty()) {

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

            if (!album.getArtist().hasGenre(genre.getId())) {
                album.getArtist().getGenres().add(new ArtistGenre()
                        .setArtist(album.getArtist())
                        .setGenre(genre));
            }

            Song savedSong = songRepository.save(songToSave);
            if (existingSong != null) {
                logger.debug("{} Updating song '{}': '{}'.", String.join(" ", saveReasons), existingSong, savedSong);
            } else {
                logger.debug("{} Creating song '{}'.", String.join(" ", saveReasons), savedSong);
            }
            if (overriddenAlbum != null) {
                libraryCleaner.deleteAlbumIfUnused(overriddenAlbum);
                libraryCleaner.deleteArtistIfUnused(overriddenAlbum.getArtist());
            }
            if (overriddenGenre != null) {
                if (libraryCleaner.deleteGenreIfUnused(overriddenGenre)) {
                    Genre deletedGenre = overriddenGenre;
                    // Remove from the artist as well, otherwise Hibernate will complain on commit.
                    album.getArtist().getGenres().stream()
                            .filter(artistGenre -> Objects.equals(artistGenre.getGenre().getId(), deletedGenre.getId()))
                            .findFirst()
                            .ifPresent(artistGenre -> album.getArtist().getGenres().remove(artistGenre));
                }
            }
            if (
                    overriddenArtwork != null &&
                            libraryCleaner.deleteArtworkIfUnused(overriddenArtwork) &&
                            Objects.equals(album.getArtwork(), overriddenArtwork)
            ) {
                albumRepository.save(album
                        .setArtwork(null));
            }
            if (shouldImportAlbumArtwork(album, artwork)) {
                importAlbumArtwork(album, artwork);
            }
            return savedSong;
        } else {
            logger.debug("No changes found, storing new update date for song '{}'.", songToSave);
            return songRepository.save(songToSave.setUpdateDate(LocalDateTime.now()));
        }
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
    public Optional<Song> importArtwork(AudioNode audioNode) {
        Song song = songRepository.findByPath(audioNode.getFile().getAbsolutePath());
        boolean modified = false;
        if (song != null) {
            if (song.getArtwork() == null) {
                ArtworkFiles artworkFiles = findAndSaveFileArtwork(audioNode);
                if (artworkFiles != null) {
                    song = songRepository.save(song
                            .setArtwork(artworkFiles.getArtwork()));
                    modified = true;
                }
            }
            if (shouldImportAlbumArtwork(song.getAlbum(), song.getArtwork())) {
                importAlbumArtwork(song.getAlbum(), song.getArtwork());
            }
        }
        return modified ? Optional.of(song) : Optional.empty();
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
                logger.debug("Updating genre '{}': '{}'.", existingGenre, savedGenre);
            } else {
                logger.debug("Creating genre '{}'.", savedGenre);
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
                logger.debug("Updating artist '{}': '{}'.", existingArtist, savedArtist);
            } else {
                logger.debug("Creating artist '{}'.", savedArtist);
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
                logger.debug("Updating album '{}': '{}'.", existingAlbum, savedAlbum);
            } else {
                logger.debug("Creating album '{}'.", savedAlbum);
            }
            return savedAlbum;
        }
        return existingAlbum;
    }

    private void importAlbumArtwork(Album album, Artwork artwork) {
        logger.debug("Setting artwork for album '{}': '{}'.", album, artwork);
        albumRepository.save(album.setArtwork(artwork));
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
            logService.warn(logger, "Could not find and save embedded artwork for data '{}'.",
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
