package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.*;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.log.service.LogService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static net.dorokhov.pony.fixture.ArtworkFixtures.artwork;
import static net.dorokhov.pony.fixture.ArtworkFixtures.artworkBuilder;
import static net.dorokhov.pony.fixture.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryCleanerTest {

    private LibraryCleaner libraryCleaner;

    @Mock
    private SongRepository songRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ArtworkStorage artworkStorage;
    @Mock
    @SuppressWarnings("unused")
    private LogService logService;

    @Spy
    @SuppressWarnings("unused")
    private PlatformTransactionManager transactionManager = transactionManager();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        libraryCleaner = spy(new LibraryCleaner(songRepository, albumRepository, artistRepository, genreRepository, artworkRepository, 
                artworkStorage, logService, 10, transactionManager));
    }

    @Test
    public void shouldDeleteArtistIfUnused() throws Exception {

        Artist artist1 = Artist.builder().id(1L).build();
        Artist artist2 = Artist.builder().id(2L).build();
        
        given(albumRepository.countByArtistId(1L)).willReturn(0L);
        given(albumRepository.countByArtistId(2L)).willReturn(1L);
        
        assertThat(libraryCleaner.deleteArtistIfUnused(artist1)).isTrue();
        verify(artistRepository).delete(1L);
        
        assertThat(libraryCleaner.deleteArtistIfUnused(artist2)).isFalse();
        verify(artistRepository, never()).delete(2L);
    }

    @Test
    public void shouldDeleteAlbumIfUnused() throws Exception {
        
        Artist artist = Artist.builder().build();

        Album album1 = Album.builder().id(1L).artist(artist).build();
        Album album2 = Album.builder().id(2L).artist(artist).build();

        given(songRepository.countByAlbumId(1L)).willReturn(0L);
        given(songRepository.countByAlbumId(2L)).willReturn(1L);

        assertThat(libraryCleaner.deleteAlbumIfUnused(album1)).isTrue();
        verify(albumRepository).delete(1L);

        assertThat(libraryCleaner.deleteAlbumIfUnused(album2)).isFalse();
        verify(albumRepository, never()).delete(2L);
    }

    @Test
    public void shouldDeleteGenreIfUnused() throws Exception {

        Genre genre1 = Genre.builder().id(1L).build();
        Genre genre2 = Genre.builder().id(2L).build();

        given(songRepository.countByGenreId(1L)).willReturn(0L);
        given(songRepository.countByGenreId(2L)).willReturn(1L);

        assertThat(libraryCleaner.deleteGenreIfUnused(genre1)).isTrue();
        verify(genreRepository).delete(1L);

        assertThat(libraryCleaner.deleteGenreIfUnused(genre2)).isFalse();
        verify(genreRepository, never()).delete(2L);
    }

    @Test
    public void shouldDeleteArtworkIfUnused() throws Exception {

        Artwork artwork1 = artworkBuilder().id(1L).build();
        Artwork artwork2 = artworkBuilder().id(2L).build();

        given(songRepository.countByArtworkId(1L)).willReturn(0L);
        given(songRepository.countByArtworkId(2L)).willReturn(1L);

        assertThat(libraryCleaner.deleteArtworkIfUnused(artwork1)).isTrue();
        verify(songRepository).clearArtworkByArtworkId(1L);
        verify(albumRepository).clearArtworkByArtworkId(1L);
        verify(artistRepository).clearArtworkByArtworkId(1L);
        verify(genreRepository).clearArtworkByArtworkId(1L);
        verify(artworkStorage).delete(1L);

        assertThat(libraryCleaner.deleteArtworkIfUnused(artwork2)).isFalse();
        verify(songRepository, never()).clearArtworkByArtworkId(2L);
        verify(albumRepository, never()).clearArtworkByArtworkId(2L);
        verify(artistRepository, never()).clearArtworkByArtworkId(2L);
        verify(genreRepository, never()).clearArtworkByArtworkId(2L);
        verify(artworkStorage, never()).delete(2L);
    }

    @Test
    public void shouldCleanNotExistingSongs() throws Exception {

        File file1 = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        AudioNode audioNode1 = mock(AudioNode.class);
        AudioNode audioNode2 = mock(AudioNode.class);
        given(audioNode1.getFile()).willReturn(file1);
        given(audioNode2.getFile()).willReturn(file2);
        
        Artwork artwork = artwork();
        Song song1 = songBuilder().id(1L).path(file1.getAbsolutePath()).build();
        Song song2 = songBuilder().id(2L).path(file2.getAbsolutePath()).build();
        Song song3 = songBuilder().id(3L).path("notExistingPath3").build();
        Song song4 = songBuilder().id(4L).path("notExistingPath4").artwork(artwork).build();
        Song song5 = songBuilder().id(5L).path("notExistingPath4").build();
        
        given(songRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(
                ImmutableList.of(song1, song2, song3, song4, song5)));
        given(songRepository.findOne(3L)).willReturn(song3);
        given(songRepository.findOne(4L)).willReturn(song4);
        given(songRepository.findOne(5L)).willReturn(null);
        
        ItemProgressObserverImpl observer = new ItemProgressObserverImpl();
        libraryCleaner.cleanSongs(ImmutableList.of(audioNode1, audioNode2), observer);
        
        verify(songRepository, times(2)).delete((Song) any());
        
        verify(songRepository).delete(song3);
        verify(libraryCleaner).deleteArtistIfUnused(song3.getAlbum().getArtist());
        verify(libraryCleaner).deleteAlbumIfUnused(song3.getAlbum());
        verify(libraryCleaner).deleteGenreIfUnused(song3.getGenre());
        verify(libraryCleaner, never()).deleteArtworkIfUnused(song3.getArtwork());
        
        verify(songRepository).delete(song4);
        verify(libraryCleaner).deleteArtistIfUnused(song4.getAlbum().getArtist());
        verify(libraryCleaner).deleteAlbumIfUnused(song4.getAlbum());
        verify(libraryCleaner).deleteGenreIfUnused(song4.getGenre());
        verify(libraryCleaner).deleteArtworkIfUnused(song4.getArtwork());
        
        assertThat(observer.size()).isEqualTo(3);
        observer.assertThatAt(0, 1, 3);
        observer.assertThatAt(1, 2, 3);
        observer.assertThatAt(2, 3, 3);
    }

    @Test
    public void shouldCleanNotExistingArtworks() throws Exception {

        File file1 = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        ImageNode imageNode1 = mock(ImageNode.class);
        ImageNode imageNode2 = mock(ImageNode.class);
        given(imageNode1.getFile()).willReturn(file1);
        given(imageNode2.getFile()).willReturn(file2);

        Artwork artwork1 = artworkBuilder().id(1L).sourceUri(file1.toURI()).build();
        Artwork artwork2 = artworkBuilder().id(2L).date(LocalDateTime.now().minusDays(1)).sourceUri(file2.toURI()).build();
        Artwork artwork3 = artworkBuilder().id(3L).sourceUri(new File("notExistingPath3").toURI()).build();
        Artwork artwork4 = artworkBuilder().id(4L).sourceUri(new File("notExistingPath4").toURI()).build();
        
        given(artworkRepository.findAll((Pageable) any())).willReturn(new PageImpl<>(
                ImmutableList.of(artwork1, artwork2, artwork3, artwork4)));
        given(artworkRepository.findOne(2L)).willReturn(artwork2);
        given(artworkRepository.findOne(3L)).willReturn(artwork3);
        given(artworkRepository.findOne(4L)).willReturn(null);

        ItemProgressObserverImpl observer = new ItemProgressObserverImpl();
        libraryCleaner.cleanArtworks(ImmutableList.of(imageNode1, imageNode2), observer);

        verify(artworkStorage, times(2)).delete(any());

        verify(artworkStorage).delete(artwork2.getId());
        verify(songRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(albumRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(artistRepository).clearArtworkByArtworkId(artwork2.getId());
        verify(genreRepository).clearArtworkByArtworkId(artwork2.getId());

        verify(artworkStorage).delete(artwork3.getId());
        verify(songRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(albumRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(artistRepository).clearArtworkByArtworkId(artwork3.getId());
        verify(genreRepository).clearArtworkByArtworkId(artwork3.getId());

        assertThat(observer.size()).isEqualTo(3);
        observer.assertThatAt(0, 1, 3);
        observer.assertThatAt(1, 2, 3);
        observer.assertThatAt(2, 3, 3);
    }
    
    private static class ItemProgressObserverImpl implements ItemProgressObserver {
        
        private final List<Integer> itemsCompleteCalls = new ArrayList<>();
        private final List<Integer> itemsTotalCalls = new ArrayList<>();
        
        @Override
        public void onProgress(int itemsComplete, int itemsTotal) {
            itemsCompleteCalls.add(itemsComplete);
            itemsTotalCalls.add(itemsTotal);
        }
        
        public int size() {
            return itemsCompleteCalls.size();
        }
        
        public void assertThatAt(int index, int itemsComplete, int itemsTotal) {
            assertThat(itemsCompleteCalls).element(index).isEqualTo(itemsComplete);
            assertThat(itemsTotalCalls).element(index).isEqualTo(itemsTotal);
        }
    }
}