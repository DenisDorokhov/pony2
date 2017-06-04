package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.fixture.ProgressObserverFixture;
import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.PlatformTransactionManager;

import static net.dorokhov.pony.fixture.PlatformTransactionManagerFixtures.transactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
    
    private PlatformTransactionManager transactionManager = transactionManager();

    @Before
    public void setUp() throws Exception {
        batchLibraryArtworkFinder = new BatchLibraryArtworkFinder(libraryArtworkFinder, 
                genreRepository, artistRepository, albumRepository,
                1, transactionManager);
    }

    @Test
    public void shouldFindAllArtworks() throws Exception {
        
        when(albumRepository.countByArtworkId(null)).thenReturn(1L);
        when(artistRepository.countByArtworkId(null)).thenReturn(1L);
        when(genreRepository.countByArtworkId(null)).thenReturn(1L);

        Artist artist = Artist.builder().build();
        Album album = Album.builder().artist(artist).build();
        Genre genre = Genre.builder().build();
        
        when(albumRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(album)));
        when(artistRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(artist)));
        when(genreRepository.findByArtworkId(isNull(), any()))
                .thenReturn(new PageImpl<>(ImmutableList.of(genre)));

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
}