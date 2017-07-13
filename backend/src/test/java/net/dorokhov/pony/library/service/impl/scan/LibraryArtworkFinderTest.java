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
import static net.dorokhov.pony.fixture.ArtworkFixtures.artworkFiles;
import static net.dorokhov.pony.fixture.SongFixtures.songBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
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

        ImageNode imageNode = mock(ImageNode.class);
        when(imageNode.getFile()).thenReturn(new File("someFile"));
        when(artworkFileFinder.findArtwork(any())).thenReturn(imageNode);
        ArtworkFiles artworkFiles = artworkFiles();
        when(artworkStorage.getOrSave((ImageNodeArtworkStorageCommand) any())).thenReturn(artworkFiles);

        ArtworkFiles savedArtworkFiles = libraryArtworkFinder.findAndSaveFileArtwork(mock(AudioNode.class));
        assertThat(savedArtworkFiles).isSameAs(artworkFiles);
    }

    @Test
    public void shouldNotSaveFileArtworkWhenArtworkNotFound() throws Exception {
        when(artworkFileFinder.findArtwork(any())).thenReturn(null);
        ArtworkFiles savedArtworkFiles = libraryArtworkFinder.findAndSaveFileArtwork(mock(AudioNode.class));
        assertThat(savedArtworkFiles).isNull();
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
        ArtworkFiles artworkFiles = artworkFiles();
        when(artworkStorage.getOrSave((ByteSourceArtworkStorageCommand) any())).thenReturn(artworkFiles);

        assertThat(libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData)).isSameAs(artworkFiles);
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

        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(3L);
        Artwork artwork = artwork();
        Song song = songBuilder()
                .artwork(artwork)
                .build();
        Pageable requiredSongPageable = new PageRequest(1, 1, Sort.Direction.ASC, "year");
        when(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(ImmutableList.of(song)));
        when(genreRepository.save((Genre) any())).then(returnsFirstArg());

        Genre genre = Genre.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenSongsWithArtworkNotFound() throws Exception {
        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(0L);
        Genre genre = Genre.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isNull();
        verify(genreRepository, never()).save((Genre) any());
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenMiddleSongNotFound() throws Exception {
        
        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(1L);
        Pageable requiredSongPageable = new PageRequest(0, 1, Sort.Direction.ASC, "year");
        when(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(emptyList()));

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
        when(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).thenReturn(song);
        when(albumRepository.save((Album) any())).then(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(song.getAlbum()).getArtwork())
                .isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveAlbumArtworkWhenSongsWithArtworkNotFound() throws Exception {
        when(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).thenReturn(null);
        Album album = Album.builder()
                .artist(Artist.builder().build())
                .build();
        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(album).getArtwork()).isNull();
        verify(albumRepository, never()).save((Album) any());
    }

    @Test
    public void shouldFindAndSaveArtistArtwork() throws Exception {
        
        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(3L);
        Artwork artwork = artwork();
        Artist artist = Artist.builder().build();
        Album album = Album.builder()
                .artist(artist)
                .artwork(artwork)
                .build();
        Pageable requiredSongPageable = new PageRequest(1, 1, Sort.Direction.ASC, "year");
        when(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(ImmutableList.of(album)));
        when(artistRepository.save((Artist) any())).then(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenAlbumsWithArtworkNotFound() throws Exception {
        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(0L);
        Artist artist = Artist.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save((Artist) any());
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenMiddleAlbumNotFound() throws Exception {

        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(1L);
        Pageable requiredSongPageable = new PageRequest(0, 1, Sort.Direction.ASC, "year");
        when(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(emptyList()));

        Artist artist = Artist.builder().build();
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save((Artist) any());
    }
}