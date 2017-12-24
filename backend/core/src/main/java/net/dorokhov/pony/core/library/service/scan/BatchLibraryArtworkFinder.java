package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Album;
import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.core.library.repository.AlbumRepository;
import net.dorokhov.pony.core.library.repository.ArtistRepository;
import net.dorokhov.pony.core.library.repository.GenreRepository;
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class BatchLibraryArtworkFinder {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LibraryArtworkFinder libraryArtworkFinder;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final int artworkSearchBufferSize;

    private final TransactionTemplate transactionTemplate;

    public BatchLibraryArtworkFinder(LibraryArtworkFinder libraryArtworkFinder,
                                     GenreRepository genreRepository,
                                     ArtistRepository artistRepository,
                                     AlbumRepository albumRepository,
                                     @Value("${pony.scan.artworkSearchBufferSize}") int artworkSearchBufferSize,
                                     PlatformTransactionManager transactionManager) {
        
        this.libraryArtworkFinder = libraryArtworkFinder;
        this.genreRepository = genreRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.artworkSearchBufferSize = artworkSearchBufferSize;
        
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void findAllArtworks(ProgressObserver progressObserver) {

        long itemsTotal = albumRepository.countByArtworkId(null) +
                artistRepository.countByArtworkId(null) +
                genreRepository.countByArtworkId(null);
        
        AtomicInteger counter = new AtomicInteger();
        findAllAlbumArtworks(() -> notifyObserver(progressObserver, counter.incrementAndGet(), itemsTotal));
        findAllArtistArtworks(() -> notifyObserver(progressObserver, counter.incrementAndGet(), itemsTotal));
        findAllGenreArtworks(() -> notifyObserver(progressObserver, counter.incrementAndGet(), itemsTotal));
    }

    private void findAllAlbumArtworks(Runnable onItemProgress) {
        AtomicReference<Pageable> pageable = new AtomicReference<>(new PageRequest(0, artworkSearchBufferSize, new Sort("id")));
        while (pageable.get() != null) {
            pageable.set(transactionTemplate.execute(status -> {
                Page<Album> albums = albumRepository.findByArtworkId(null, pageable.get());
                for (Album album : albums) {
                    libraryArtworkFinder.findAndSaveAlbumArtwork(album);
                    onItemProgress.run();
                }
                return albums.nextPageable();
            }));
        }
    }

    private void findAllArtistArtworks(Runnable onItemProgress) {
        AtomicReference<Pageable> pageable = new AtomicReference<>(new PageRequest(0, artworkSearchBufferSize, new Sort("id")));
        while (pageable.get() != null) {
            pageable.set(transactionTemplate.execute(status -> {
                Page<Artist> artists = artistRepository.findByArtworkId(null, pageable.get());
                for (Artist artist : artists) {
                    libraryArtworkFinder.findAndSaveArtistArtwork(artist);
                    onItemProgress.run();
                }
                return artists.nextPageable();
            }));
        }
    }

    private void findAllGenreArtworks(Runnable onItemProgress) {
        AtomicReference<Pageable> pageable = new AtomicReference<>(new PageRequest(0, artworkSearchBufferSize, new Sort("id")));
        while (pageable.get() != null) {
            pageable.set(transactionTemplate.execute(status -> {
                Page<Genre> genres = genreRepository.findByArtworkId(null, pageable.get());
                for (Genre genre : genres) {
                    libraryArtworkFinder.findAndSaveGenreArtwork(genre);
                    onItemProgress.run();
                }
                return genres.nextPageable();
            }));
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
