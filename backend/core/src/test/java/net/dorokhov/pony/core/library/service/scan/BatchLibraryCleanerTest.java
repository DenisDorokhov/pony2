package net.dorokhov.pony.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.core.ProgressObserverFixture;
import net.dorokhov.pony.api.library.domain.Artwork;
import net.dorokhov.pony.api.library.domain.Song;
import net.dorokhov.pony.core.library.repository.*;
import net.dorokhov.pony.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony.test.ArtworkFixtures.artworkBuilder;
import static net.dorokhov.pony.core.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.test.SongFixtures.song;
import static net.dorokhov.pony.test.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BatchLibraryCleanerTest {

    private BatchLibraryCleaner batchLibraryCleaner;

    @Mock
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
    private LogService logService;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private PlatformTransactionManager transactionManager = transactionManager();

    @Before
    public void setUp() throws Exception {
        batchLibraryCleaner = new BatchLibraryCleaner(libraryCleaner, 
                songRepository, albumRepository, artistRepository, genreRepository, 
                artworkRepository, artworkStorage, logService, 1, transactionManager);
    }

    @Test
    public void shouldCleanNotExistingSongs() throws Exception {

        File file1 = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        AudioNode audioNode1 = mock(AudioNode.class);
        AudioNode audioNode2 = mock(AudioNode.class);
        when(audioNode1.getFile()).thenReturn(file1);
        when(audioNode2.getFile()).thenReturn(file2);

        Artwork artwork = artwork();
        Song song1 = songBuilder().id(1L).path(file1.getAbsolutePath()).build();
        Song song2 = songBuilder().id(2L).path(file2.getAbsolutePath()).build();
        Song song3 = songBuilder().id(3L).path("notExistingPath3").build();
        Song song4 = songBuilder().id(4L).path("notExistingPath4").artwork(artwork).build();
        Song song5 = songBuilder().id(5L).path("notExistingPath4").build();

        when(songRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(
                ImmutableList.of(song1, song2, song3, song4, song5)));
        when(songRepository.findOne(3L)).thenReturn(song3);
        when(songRepository.findOne(4L)).thenReturn(song4);
        when(songRepository.findOne(5L)).thenReturn(null);

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryCleaner.cleanSongs(ImmutableList.of(audioNode1, audioNode2), observer);

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
        when(imageNode1.getFile()).thenReturn(file1);
        when(imageNode2.getFile()).thenReturn(file2);

        Artwork artwork1 = artworkBuilder()
                .id(1L)
                .sourceUri(file1.toURI())
                .build();
        Artwork artwork2 = artworkBuilder()
                .id(2L)
                .date(LocalDateTime.now().minusDays(1))
                .sourceUri(file2.toURI())
                .build();
        Artwork artwork3 = artworkBuilder()
                .id(3L)
                .sourceUri(new File("notExistingPath3").toURI())
                .build();
        Artwork artwork4 = artworkBuilder()
                .id(4L)
                .sourceUri(new File("notExistingPath4").toURI())
                .build();
        Artwork artwork5 = artworkBuilder()
                .id(4L)
                .sourceUri(UriComponentsBuilder
                        .fromUriString("http://google.com/logo.png")
                        .build().toUri())
                .build();

        when(artworkRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(
                ImmutableList.of(artwork1, artwork2, artwork3, artwork4, artwork5)));
        when(artworkRepository.findOne(2L)).thenReturn(artwork2);
        when(artworkRepository.findOne(3L)).thenReturn(artwork3);
        when(artworkRepository.findOne(4L)).thenReturn(null);

        ProgressObserverFixture observer = new ProgressObserverFixture();
        batchLibraryCleaner.cleanArtworks(ImmutableList.of(imageNode1, imageNode2), observer);

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

    @Test
    public void shouldNotFailSongsCleanupOnObserverException() throws Exception {
        when(songRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(song())));
        batchLibraryCleaner.cleanSongs(emptyList(), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldNotFailArtworksCleanupOnObserverException() throws Exception {
        when(artworkRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(ImmutableList.of(artwork())));
        batchLibraryCleaner.cleanArtworks(emptyList(), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }
}