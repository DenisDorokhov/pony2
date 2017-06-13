package net.dorokhov.pony.library.service.impl;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.Artist;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.util.List;

import static net.dorokhov.pony.fixture.ArtworkFixtures.artwork;
import static net.dorokhov.pony.fixture.SongFixtures.song;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LibraryServiceImplTest {

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtworkStorage artworkStorage;

    @Test
    public void shouldGetArtists() throws Exception {
        List<Artist> artists = ImmutableList.of(Artist.builder().build(), Artist.builder().build());
        when(artistRepository.findAll((Sort) any())).thenReturn(artists);
        List<Artist> result = libraryService.getArtists();
        assertThat(result).isSameAs(artists);
    }

    @Test
    public void shouldGetArtistById() throws Exception {
        Artist artist = Artist.builder().build();
        when(artistRepository.findOne(any())).thenReturn(artist);
        assertThat(libraryService.getArtistById(1L)).isSameAs(artist);
    }

    @Test
    public void shouldGetSongById() throws Exception {
        Song song = song();
        when(songRepository.findOne(any())).thenReturn(song);
        assertThat(libraryService.getSongById(1L)).isSameAs(song);
    }

    @Test
    public void shouldGetArtworkFilesById() throws Exception {
        ArtworkFiles artworkFiles = new ArtworkFiles(artwork(), mock(File.class), mock(File.class));
        when(artworkStorage.getArtworkFile(any())).thenReturn(artworkFiles);
        assertThat(libraryService.getArtworkFilesById(1L)).isSameAs(artworkFiles);
    }

    @Test
    public void shouldGetSongsByIds() throws Exception {
        List<Song> songs = ImmutableList.of(
                songBuilder().id(1L).build(), songBuilder().id(2L).build());
        when(songRepository.findAll(anyIterable())).thenReturn(songs);
        List<Song> result = libraryService.getSongsByIds(ImmutableList.of(1L, 2L));
        assertThat(result).isEqualTo(songs);
    }
}