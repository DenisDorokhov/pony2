package net.dorokhov.pony3.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.api.library.domain.Album;
import net.dorokhov.pony3.api.library.domain.Artist;
import net.dorokhov.pony3.api.library.domain.Genre;
import net.dorokhov.pony3.core.library.ProgressObserverFixture;
import net.dorokhov.pony3.core.library.repository.AlbumRepository;
import net.dorokhov.pony3.core.library.repository.ArtistRepository;
import net.dorokhov.pony3.core.library.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.PlatformTransactionManager;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony3.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BatchLibraryArtworkFinderTest {

    private BatchLibraryArtworkFinder batchLibraryArtworkFinder;

    @Mock
    private LibraryArtworkFinder libraryArtworkFinder;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
    
    private final PlatformTransactionManager transactionManager = transactionManager();

    @BeforeEach
    public void setUp() {
        batchLibraryArtworkFinder = new BatchLibraryArtworkFinder(libraryArtworkFinder, 
                genreRepository, artistRepository, albumRepository,
                1, transactionManager);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFindAllArtworks() {
        
        when(albumRepository.countByArtworkId(null)).thenReturn(1L);
        when(artistRepository.countByArtworkId(null)).thenReturn(1L);
        when(genreRepository.countByArtworkId(null)).thenReturn(1L);

        Artist artist = new Artist();
        Album album = new Album().setArtist(artist);
        Genre genre = new Genre();
        
        when(albumRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(album)));
        when(artistRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(artist)));
        when(genreRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(genre)));
        
        when(albumRepository.findAllById(any(Iterable.class)))
                .thenReturn(ImmutableList.of(album));
        when(artistRepository.findAllById(any(Iterable.class)))
                .thenReturn(ImmutableList.of(artist));
        when(genreRepository.findAllById(any(Iterable.class)))
                .thenReturn(ImmutableList.of(genre));

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryArtworkFinder.findAllArtworks(observer);
        
        verify(libraryArtworkFinder).findAndSaveAlbumArtwork(any());
        verify(libraryArtworkFinder).findAndSaveArtistArtwork(any());
        verify(libraryArtworkFinder).findAndSaveGenreArtwork(any());

        assertThat(observer.size()).isEqualTo(3);
        observer.assertThatAt(0, 1, 3);
        observer.assertThatAt(1, 2, 3);
        observer.assertThatAt(2, 3, 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldNotFailOnObserverException() {

        when(albumRepository.countByArtworkId(null)).thenReturn(0L);
        when(artistRepository.countByArtworkId(null)).thenReturn(0L);
        when(genreRepository.countByArtworkId(null)).thenReturn(1L);

        Genre genre = new Genre();

        when(albumRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(emptyList()));
        when(artistRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(emptyList()));
        when(genreRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(genre)));

        when(genreRepository.findAllById(any(Iterable.class)))
                .thenReturn(ImmutableList.of(genre));

        batchLibraryArtworkFinder.findAllArtworks((itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }
}