package net.dorokhov.pony.core.library.service;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.Artist;
import net.dorokhov.pony.api.library.domain.ArtworkFiles;
import net.dorokhov.pony.api.library.domain.Genre;
import net.dorokhov.pony.api.library.domain.Song;
import net.dorokhov.pony.core.library.repository.ArtistRepository;
import net.dorokhov.pony.core.library.repository.GenreRepository;
import net.dorokhov.pony.core.library.repository.SongRepository;
import net.dorokhov.pony.core.library.service.artwork.ArtworkStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony.test.SongFixtures.song;
import static net.dorokhov.pony.test.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryServiceImplTest {

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtworkStorage artworkStorage;
    @Mock
    private RandomFetcher randomFetcher;

    @Test
    public void shouldGetGenres() {

        List<Genre> genres = ImmutableList.of(Genre.builder().build(), Genre.builder().build());
        when(genreRepository.findAll((Sort) any())).thenReturn(genres);

        assertThat(libraryService.getGenres()).isEqualTo(genres);
    }

    @Test
    public void shouldGetArtists() {

        List<Artist> artists = ImmutableList.of(Artist.builder().build(), Artist.builder().build());
        when(artistRepository.findAll((Sort) any())).thenReturn(artists);

        assertThat(libraryService.getArtists()).isEqualTo(artists);
    }

    @Test
    public void shouldGetArtistById() {

        Artist artist = Artist.builder().build();
        when(artistRepository.findOne((Long) any())).thenReturn(artist);

        assertThat(libraryService.getArtistById(1L)).isSameAs(artist);
    }

    @Test
    public void shouldGetGenreById() {

        Genre genre = Genre.builder().build();
        when(genreRepository.findOne((Long) any())).thenReturn(genre);

        assertThat(libraryService.getGenreById(1L)).isSameAs(genre);
    }

    @Test
    public void shouldGetSongById() {

        Song song = song();
        when(songRepository.findOne((Long) any())).thenReturn(song);

        assertThat(libraryService.getSongById(1L)).isSameAs(song);
    }

    @Test
    public void shouldGetSongsByGenreId() {

        List<Song> songs = ImmutableList.of(song(), song());
        when(songRepository.findByGenreId(any(), any())).thenReturn(new PageImpl<>(songs));

        Page<Song> result = libraryService.getSongsByGenreId(1L, 0);

        assertThat(result.getContent()).isEqualTo(songs);
    }

    @Test
    public void shouldGetArtworkFilesById() {

        ArtworkFiles artworkFiles = new ArtworkFiles(artwork(), mock(File.class), mock(File.class));
        when(artworkStorage.getArtworkFile(any())).thenReturn(artworkFiles);

        assertThat(libraryService.getArtworkFilesById(1L)).isSameAs(artworkFiles);
    }

    @Test
    public void shouldGetSongsByIds() {

        List<Song> songs = ImmutableList.of(
                songBuilder().id(1L).build(), songBuilder().id(2L).build());
        when(songRepository.findAll(anyIterable())).thenReturn(songs);

        List<Song> result = libraryService.getSongsByIds(ImmutableList.of(1L, 2L));

        assertThat(result).isEqualTo(songs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRandomSongs() {

        when(songRepository.findAll((Pageable) any())).thenReturn(new PageImpl<>(emptyList()));
        List<Song> songs = new ArrayList<>();
        when(randomFetcher.fetch(eq(2), (RandomFetcher.Repository<Song>) any())).thenAnswer(invocation -> {
            RandomFetcher.Repository repository = invocation.getArgument(1);
            repository.fetchCount();
            repository.fetchContent(new PageRequest(0, 1));
            return songs;
        });

        assertThat(libraryService.getRandomSongs(2)).isSameAs(songs);

        verify(songRepository).count();
        verify(songRepository).findAll((Pageable) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRandomSongsByAlbumId() {

        List<Song> songs = new ArrayList<>();
        when(randomFetcher.fetch(eq(3), (RandomFetcher.Repository<Song>) any())).thenAnswer(invocation -> {
            RandomFetcher.Repository repository = invocation.getArgument(1);
            repository.fetchCount();
            repository.fetchContent(new PageRequest(0, 1));
            return songs;
        });

        assertThat(libraryService.getRandomSongsByAlbumId(1L, 3)).isSameAs(songs);

        verify(songRepository).countByAlbumId(1L);
        verify(songRepository).findByAlbumId(eq(1L), (Pageable) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRandomSongsByArtistId() {

        List<Song> songs = new ArrayList<>();
        when(randomFetcher.fetch(eq(4), (RandomFetcher.Repository<Song>) any())).thenAnswer(invocation -> {
            RandomFetcher.Repository repository = invocation.getArgument(1);
            repository.fetchCount();
            repository.fetchContent(new PageRequest(0, 1));
            return songs;
        });

        assertThat(libraryService.getRandomSongsByArtistId(1L, 4)).isSameAs(songs);

        verify(songRepository).countByAlbumArtistId(1L);
        verify(songRepository).findByAlbumArtistId(eq(1L), (Pageable) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRandomSongsByGenreId() {

        when(songRepository.findByGenreId(any(), any())).thenReturn(new PageImpl<>(emptyList()));
        List<Song> songs = new ArrayList<>();
        when(randomFetcher.fetch(eq(5), (RandomFetcher.Repository<Song>) any())).thenAnswer(invocation -> {
            RandomFetcher.Repository repository = invocation.getArgument(1);
            repository.fetchCount();
            repository.fetchContent(new PageRequest(0, 1));
            return songs;
        });

        assertThat(libraryService.getRandomSongsByGenreId(1L, 5)).isSameAs(songs);

        verify(songRepository).countByGenreId(1L);
        verify(songRepository).findByGenreId(eq(1L), any());
    }
}