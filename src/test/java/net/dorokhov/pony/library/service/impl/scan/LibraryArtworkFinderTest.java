package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import net.dorokhov.pony.fixture.SongFixtures;
import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.repository.AlbumRepository;
import net.dorokhov.pony.library.repository.ArtistRepository;
import net.dorokhov.pony.library.repository.GenreRepository;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkFileFinder;
import net.dorokhov.pony.library.service.impl.artwork.ArtworkStorage;
import net.dorokhov.pony.library.service.impl.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony.library.service.impl.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData.EmbeddedArtwork;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.log.service.LogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.fixture.ArtworkFixtures.artwork;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryArtworkFinderTest {

    @InjectMocks
    private LibraryArtworkFinder libraryArtworkFinder;

    @Mock
    private GenreRepository genreRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private SongRepository songRepository;
    @Mock
    private ArtworkFileFinder artworkFileFinder;
    @Mock
    private ArtworkStorage artworkStorage;
    @Mock
    @SuppressWarnings("unused")
    private LogService logService;

    @Test
    public void shouldFindAndSaveFileArtwork() throws Exception {

        AudioNode audioNode = mock(AudioNode.class);
        given(audioNode.getFile()).willReturn(new File("someFile"));
        given(artworkFileFinder.findArtwork(any())).willReturn(mock(ImageNode.class));
        Artwork artwork = artwork();
        given(artworkStorage.getOrSave((ImageNodeArtworkStorageCommand) any())).willReturn(artwork);

        Artwork savedArtwork = libraryArtworkFinder.findAndSaveFileArtwork(audioNode);
        assertThat(savedArtwork).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveFileArtworkWhenArtworkNotFound() throws Exception {
        given(artworkFileFinder.findArtwork(any())).willReturn(null);
        Artwork savedArtwork = libraryArtworkFinder.findAndSaveFileArtwork(mock(AudioNode.class));
        assertThat(savedArtwork).isNull();
        verify(artworkStorage, never()).getOrSave((ImageNodeArtworkStorageCommand) any());
    }

    @Test
    public void shouldFindAndSaveEmbeddedArtwork() throws Exception {

        ReadableAudioData audioData = ReadableAudioData.builder()
                .path("somePath")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .embeddedArtwork(new EmbeddedArtwork(
                        ByteSource.empty(), FileType.of("image/png", "png")))
                .build();
        Artwork artwork = artwork();
        given(artworkStorage.getOrSave((ByteSourceArtworkStorageCommand) any())).willReturn(artwork);

        assertThat(libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData)).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveEmbeddedArtworkWhenArtworkNotFound() throws Exception {
        ReadableAudioData audioData = ReadableAudioData.builder()
                .path("somePath")
                .fileType(FileType.of("audio/mpeg", "mp3"))
                .embeddedArtwork(null)
                .build();
        assertThat(libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData)).isNull();
        verify(artworkStorage, never()).getOrSave((ByteSourceArtworkStorageCommand) any());
    }

    @Test
    public void shouldFindAndSaveGenreArtwork() throws Exception {

        given(songRepository.countByGenreIdAndArtworkNotNull(any())).willReturn(3L);
        Artwork artwork = artwork();
        Song song = songBuilder()
                .artwork(artwork)
                .build();
        Pageable requiredSongPageable = new PageRequest(1, 1, Sort.Direction.ASC, "year");
        given(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .willReturn(new PageImpl<>(ImmutableList.of(song)));
        given(genreRepository.save((Genre) any())).willAnswer(returnsFirstArg());

        Genre genre = Genre.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenSongsWithArtworkNotFound() throws Exception {
        given(songRepository.countByGenreIdAndArtworkNotNull(any())).willReturn(0L);
        Genre genre = Genre.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isNull();
        verify(genreRepository, never()).save((Genre) any());
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenMiddleSongNotFound() throws Exception {
        
        given(songRepository.countByGenreIdAndArtworkNotNull(any())).willReturn(1L);
        Pageable requiredSongPageable = new PageRequest(0, 1, Sort.Direction.ASC, "year");
        given(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .willReturn(new PageImpl<>(emptyList()));

        Genre genre = Genre.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isNull();
        verify(genreRepository, never()).save((Genre) any());
    }
    
    @Test
    public void shouldFindAndSaveAlbumArtwork() throws Exception {
        
        Artwork artwork = artwork();
        Song song = SongFixtures.songBuilder()
                .artwork(artwork)
                .build();
        given(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).willReturn(song);
        given(albumRepository.save((Album) any())).willAnswer(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(song.getAlbum()).getArtwork())
                .isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveAlbumArtworkWhenSongsWithArtworkNotFound() throws Exception {
        given(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).willReturn(null);
        Album album = Album.builder()
                .artist(Artist.builder().build())
                .build();
        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(album).getArtwork()).isNull();
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldFindAndSaveArtistArtwork() throws Exception {
        
        given(albumRepository.countByArtistIdAndArtworkNotNull(any())).willReturn(3L);
        Artwork artwork = artwork();
        Artist artist = Artist.builder().build();
        Album album = Album.builder()
                .artist(artist)
                .artwork(artwork)
                .build();
        Pageable requiredSongPageable = new PageRequest(1, 1, Sort.Direction.ASC, "year");
        given(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .willReturn(new PageImpl<>(ImmutableList.of(album)));
        given(artistRepository.save((Artist) any())).willAnswer(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenAlbumsWithArtworkNotFound() throws Exception {
        given(albumRepository.countByArtistIdAndArtworkNotNull(any())).willReturn(0L);
        Artist artist = Artist.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save((Artist) any());
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenMiddleAlbumNotFound() throws Exception {

        given(albumRepository.countByArtistIdAndArtworkNotNull(any())).willReturn(1L);
        Pageable requiredSongPageable = new PageRequest(0, 1, Sort.Direction.ASC, "year");
        given(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .willReturn(new PageImpl<>(emptyList()));

        Artist artist = Artist.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save((Artist) any());
    }
}