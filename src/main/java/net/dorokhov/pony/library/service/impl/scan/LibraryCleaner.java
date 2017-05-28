package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.Lists;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.*;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
class LibraryCleaner {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtworkStorage artworkStorage;
    private final LogService logService;
    private final int cleaningBufferSize;
    
    private final TransactionTemplate transactionTemplate;

    public LibraryCleaner(SongRepository songRepository,
                          AlbumRepository albumRepository,
                          ArtistRepository artistRepository,
                          GenreRepository genreRepository,
                          ArtworkRepository artworkRepository,
                          ArtworkStorage artworkStorage,
                          LogService logService,
                          @Value("${pony.scan.cleaningBufferSize}") int cleaningBufferSize, 
                          PlatformTransactionManager transactionManager) {
        
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.artworkRepository = artworkRepository;
        this.artworkStorage = artworkStorage;
        this.logService = logService;
        this.cleaningBufferSize = cleaningBufferSize;
        
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }
    
    public void cleanSongs(List<AudioNode> existingAudioNodes, ItemProgressObserver itemProgressObserver) {

        Set<String> existingAudioPaths = existingAudioNodes.stream()
                .map(AudioNode::getFile)
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        List<Long> songsToDelete = transactionTemplate.execute(status -> {
            List<Long> result = new ArrayList<>();
            Pageable pageable = new PageRequest(0, cleaningBufferSize, new Sort("id"));
            while (pageable != null) {
                Page<Song> songs = songRepository.findAll(pageable);
                for (Song song : songs.getContent()) {
                    if (!existingAudioPaths.contains(song.getPath())) {
                        result.add(song.getId());
                    }
                }
                pageable = songs.nextPageable();
            }
            return result;
        });

        AtomicInteger counter = new AtomicInteger();
        for (List<Long> chunk : Lists.partition(songsToDelete, cleaningBufferSize)) {
            transactionTemplate.execute(status -> {
                for (Long id : chunk) {
                    Song song = songRepository.findOne(id);
                    if (song != null) {
                        songRepository.delete(song);
                        deleteAlbumIfUnused(song.getAlbum());
                        deleteArtistIfUnused(song.getAlbum().getArtist());
                        deleteGenreIfUnused(song.getGenre());
                        if (song.getArtwork() != null) {
                            deleteArtworkIfUnused(song.getArtwork());
                        }
                        logService.debug(logger, "Song '{}' has been deleted: file '{}' not found.", song, song.getPath());
                    }
                    itemProgressObserver.onProgress(counter.incrementAndGet(), songsToDelete.size());
                }
                return null;
            });
        }
    }
    
    public void cleanArtworks(List<ImageNode> existingImageNodes, ItemProgressObserver itemProgressObserver) {

        Set<String> existingImagePaths = existingImageNodes.stream()
                .map(ImageNode::getFile)
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        List<Long> artworksToDelete = transactionTemplate.execute(status -> {
            List<Long> result = new ArrayList<>();
            Pageable pageable = new PageRequest(0, cleaningBufferSize, new Sort("id"));
            while (pageable != null) {
                Page<Artwork> artworks = artworkRepository.findAll(pageable);
                for (Artwork artwork : artworks.getContent()) {
                    if (artwork.getSourceUri().getScheme().equals("file")) {
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
                pageable = artworks.nextPageable();
            }
            return result;
        });

        AtomicInteger counter = new AtomicInteger();
        for (List<Long> chunk : Lists.partition(artworksToDelete, cleaningBufferSize)) {
            transactionTemplate.execute(status -> {
                for (Long id : chunk) {
                    Artwork artwork = artworkRepository.findOne(id);
                    if (artwork != null) {
                        songRepository.clearArtworkByArtworkId(id);
                        albumRepository.clearArtworkByArtworkId(id);
                        artistRepository.clearArtworkByArtworkId(id);
                        genreRepository.clearArtworkByArtworkId(id);
                        artworkStorage.delete(id);
                        logService.debug(logger, "Artwork '{}' has been deleted: file '{}' not found or has been modified.", artwork, artwork.getSourceUri().getPath());
                    }
                    itemProgressObserver.onProgress(counter.incrementAndGet(), artworksToDelete.size());
                }
                return null;
            });
        }
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
