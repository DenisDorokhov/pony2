package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.Lists;
import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.Song;
import net.dorokhov.pony2.common.UriUtils;
import net.dorokhov.pony2.core.library.repository.*;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony2.core.library.service.filetree.domain.ImageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.api.library.domain.Artwork.SOURCE_URI_SCHEME_FILE;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class BatchLibraryCleaner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LibraryCleaner libraryCleaner;
    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtworkStorage artworkStorage;
    private final PlaylistSongRepository playlistSongRepository;

    private final int cleaningFetchingBufferSize;
    private final int cleaningDeletionBufferSize;

    private final TransactionTemplate transactionTemplate;

    public BatchLibraryCleaner(
            LibraryCleaner libraryCleaner,
            SongRepository songRepository,
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            GenreRepository genreRepository,
            ArtworkRepository artworkRepository,
            ArtworkStorage artworkStorage,
            PlaylistSongRepository playlistSongRepository,
            @Value("${pony.scan.cleaningFetchingBufferSize}") int cleaningFetchingBufferSize,
            @Value("${pony.scan.cleaningDeletionBufferSize}") int cleaningDeletionBufferSize,
            PlatformTransactionManager transactionManager
    ) {

        this.libraryCleaner = libraryCleaner;
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.artworkRepository = artworkRepository;
        this.artworkStorage = artworkStorage;
        this.playlistSongRepository = playlistSongRepository;
        this.cleaningFetchingBufferSize = cleaningFetchingBufferSize;
        this.cleaningDeletionBufferSize = cleaningDeletionBufferSize;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void cleanSongs(List<AudioNode> existingAudioNodes, ProgressObserver progressObserver) {

        Set<String> existingAudioPaths = existingAudioNodes.stream()
                .map(AudioNode::getFile)
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        List<String> songsToDelete = requireNonNull(transactionTemplate.execute(transactionStatus -> {
            List<String> result = new ArrayList<>();
            Pageable pageable = PageRequest.of(0, cleaningFetchingBufferSize, Sort.by("id"));
            while (pageable != null) {
                Page<Song> songs = songRepository.findAll(pageable);
                for (Song song : songs.getContent()) {
                    if (!existingAudioPaths.contains(song.getPath())) {
                        result.add(song.getId());
                    }
                }
                pageable = songs.hasNext() ? songs.nextPageable() : null;
            }
            return result;
        }));

        AtomicInteger counter = new AtomicInteger();
        for (List<String> chunk : Lists.partition(songsToDelete, cleaningDeletionBufferSize)) {
            transactionTemplate.execute(transactionStatus -> {
                for (String id : chunk) {
                    songRepository.findById(id).ifPresent(song -> {
                        logger.debug("Deleting song '{}': file '{}' not found.", song, song.getPath());
                        playlistSongRepository.deleteBySongId(song.getId());
                        songRepository.delete(song);
                        libraryCleaner.deleteAlbumIfUnused(song.getAlbum());
                        libraryCleaner.deleteArtistIfUnused(song.getAlbum().getArtist());
                        libraryCleaner.deleteGenreIfUnused(song.getGenre());
                        if (song.getArtwork() != null) {
                            libraryCleaner.deleteArtworkIfUnused(song.getArtwork());
                        }
                    });
                    notifyObserver(progressObserver, counter.incrementAndGet(), songsToDelete.size());
                }
                return null;
            });
        }
    }

    public void cleanArtworks(List<ImageNode> existingImageNodes, ProgressObserver progressObserver) {

        Set<String> existingImagePaths = existingImageNodes.stream()
                .map(ImageNode::getFile)
                .map(UriUtils::fileToUriPath)
                .collect(Collectors.toSet());

        List<String> artworksToDelete = requireNonNull(transactionTemplate.execute(transactionStatus -> {
            List<String> result = new ArrayList<>();
            Pageable pageable = PageRequest.of(0, cleaningFetchingBufferSize, Sort.by("id"));
            while (pageable != null) {
                Page<Artwork> artworks = artworkRepository.findAll(pageable);
                for (Artwork artwork : artworks.getContent()) {
                    if (Objects.equals(artwork.getSourceUriScheme(), SOURCE_URI_SCHEME_FILE)) {
                        if (!existingImagePaths.contains(artwork.getSourceUri().getPath())) {
                            result.add(artwork.getId());
                        } else {
                            File file = new File(artwork.getSourceUri().getPath());
                            LocalDateTime modificationDate = Instant.ofEpochMilli(file.lastModified())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                            if (artwork.getDate().isBefore(modificationDate)) {
                                result.add(artwork.getId());
                            }
                        }
                    }
                }
                pageable = artworks.hasNext() ? artworks.nextPageable() : null;
            }
            return result;
        }));

        AtomicInteger counter = new AtomicInteger();
        for (List<String> chunk : Lists.partition(artworksToDelete, cleaningDeletionBufferSize)) {
            transactionTemplate.execute(transactionStatus -> {
                for (String id : chunk) {
                    artworkRepository.findById(id).ifPresent(artwork -> {
                        logger.debug("Deleting artwork '{}': file '{}' not found or has been modified.", artwork, artwork.getSourceUri().getPath());
                        songRepository.clearArtworkByArtworkId(id);
                        albumRepository.clearArtworkByArtworkId(id);
                        artistRepository.clearArtworkByArtworkId(id);
                        genreRepository.clearArtworkByArtworkId(id);
                        artworkStorage.delete(id);
                    });
                    notifyObserver(progressObserver, counter.incrementAndGet(), artworksToDelete.size());
                }
                return null;
            });
        }
    }

    private void notifyObserver(ProgressObserver observer, long itemsComplete, long itemsTotal) {
        try {
            observer.onProgress(itemsComplete, itemsTotal);
        } catch (Exception e) {
            logger.error("Could not call progress observer {}.", observer, e);
        }
    }
}
