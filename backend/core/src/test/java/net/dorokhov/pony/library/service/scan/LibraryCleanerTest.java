package net.dorokhov.pony.library.service.scan;

import net.dorokhov.pony.library.domain.Album;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.Genre;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.scan.LibraryCleaner;
import net.dorokhov.pony.log.service.LogService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static net.dorokhov.pony.fixture.ArtworkFixtures.artworkBuilder;
import static org.assertj.core.api.Assertions.assertThat;
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
    private ArtworkStorage artworkStorage;
    @Mock
    private LogService logService;

    @Before
    public void setUp() throws Exception {
        libraryCleaner = new LibraryCleaner(songRepository, albumRepository, artistRepository, genreRepository,
                artworkStorage, logService);
    }

    @Test
    public void shouldDeleteArtistIfUnused() throws Exception {

        Artist artist1 = Artist.builder().id(1L).build();
        Artist artist2 = Artist.builder().id(2L).build();

        when(albumRepository.countByArtistId(1L)).thenReturn(0L);
        when(albumRepository.countByArtistId(2L)).thenReturn(1L);

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

        when(songRepository.countByAlbumId(1L)).thenReturn(0L);
        when(songRepository.countByAlbumId(2L)).thenReturn(1L);

        assertThat(libraryCleaner.deleteAlbumIfUnused(album1)).isTrue();
        verify(albumRepository).delete(1L);

        assertThat(libraryCleaner.deleteAlbumIfUnused(album2)).isFalse();
        verify(albumRepository, never()).delete(2L);
    }

    @Test
    public void shouldDeleteGenreIfUnused() throws Exception {

        Genre genre1 = Genre.builder().id(1L).build();
        Genre genre2 = Genre.builder().id(2L).build();

        when(songRepository.countByGenreId(1L)).thenReturn(0L);
        when(songRepository.countByGenreId(2L)).thenReturn(1L);

        assertThat(libraryCleaner.deleteGenreIfUnused(genre1)).isTrue();
        verify(genreRepository).delete(1L);

        assertThat(libraryCleaner.deleteGenreIfUnused(genre2)).isFalse();
        verify(genreRepository, never()).delete(2L);
    }

    @Test
    public void shouldDeleteArtworkIfUnused() throws Exception {

        Artwork artwork1 = artworkBuilder().id(1L).build();
        Artwork artwork2 = artworkBuilder().id(2L).build();

        when(songRepository.countByArtworkId(1L)).thenReturn(0L);
        when(songRepository.countByArtworkId(2L)).thenReturn(1L);

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
}