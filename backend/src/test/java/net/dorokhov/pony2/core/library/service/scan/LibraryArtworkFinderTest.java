package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData.EmbeddedArtwork;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkFileFinder;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony2.core.library.service.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony2.core.library.service.filetree.domain.ImageNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.File;
import java.io.IOException;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony2.test.ArtworkFixtures.artworkFiles;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    public void shouldFindAndSaveFileArtwork() throws IOException {

        ImageNode imageNode = mock(ImageNode.class);
        when(imageNode.getFile()).thenReturn(new File("someFile"));
        when(artworkFileFinder.findArtwork(any())).thenReturn(imageNode);
        ArtworkFiles artworkFiles = artworkFiles();
        when(artworkStorage.getOrSave((ImageNodeArtworkStorageCommand) any())).thenReturn(artworkFiles);

        ArtworkFiles savedArtworkFiles = libraryArtworkFinder.findAndSaveFileArtwork(mock(AudioNode.class));

        assertThat(savedArtworkFiles).isSameAs(artworkFiles);
    }

    @Test
    public void shouldNotSaveFileArtworkWhenArtworkNotFound() throws IOException {

        when(artworkFileFinder.findArtwork(any())).thenReturn(null);

        ArtworkFiles savedArtworkFiles = libraryArtworkFinder.findAndSaveFileArtwork(mock(AudioNode.class));

        assertThat(savedArtworkFiles).isNull();
        verify(artworkStorage, never()).getOrSave((ImageNodeArtworkStorageCommand) any());
    }

    @Test
    public void shouldFindAndSaveEmbeddedArtwork() throws IOException {

        ReadableAudioData audioData = new ReadableAudioData()
                .setPath("somePath")
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setEmbeddedArtwork(new EmbeddedArtwork(
                        ByteSource.empty(), FileType.of("image/png", "png")));
        ArtworkFiles artworkFiles = artworkFiles();
        when(artworkStorage.getOrSave((ByteSourceArtworkStorageCommand) any())).thenReturn(artworkFiles);

        assertThat(libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData)).isSameAs(artworkFiles);
    }

    @Test
    public void shouldNotSaveEmbeddedArtworkWhenArtworkNotFound() throws IOException {

        ReadableAudioData audioData = new ReadableAudioData()
                .setPath("somePath")
                .setFileType(FileType.of("audio/mpeg", "mp3"))
                .setEmbeddedArtwork(null);

        assertThat(libraryArtworkFinder.findAndSaveEmbeddedArtwork(audioData)).isNull();
        verify(artworkStorage, never()).getOrSave((ByteSourceArtworkStorageCommand) any());
    }

    @Test
    public void shouldFindAndSaveGenreArtwork() {

        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(3L);
        Artwork artwork = artwork();
        Song song = song()
                .setArtwork(artwork);
        Pageable requiredSongPageable = PageRequest.of(1, 1, Sort.Direction.ASC, "year");
        when(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(ImmutableList.of(song)));
        when(genreRepository.save(any())).then(returnsFirstArg());
        Genre genre = new Genre();

        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenSongsWithArtworkNotFound() {

        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(0L);
        Genre genre = new Genre();

        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isNull();
        verify(genreRepository, never()).save(any());
    }

    @Test
    public void shouldNotSaveGenreArtworkWhenMiddleSongNotFound() {
        
        when(songRepository.countByGenreIdAndArtworkNotNull(any())).thenReturn(1L);
        Pageable requiredSongPageable = PageRequest.of(0, 1, Sort.Direction.ASC, "year");
        when(songRepository.findByGenreIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(emptyList()));
        Genre genre = new Genre();

        assertThat(libraryArtworkFinder.findAndSaveGenreArtwork(genre).getArtwork()).isNull();
        verify(genreRepository, never()).save(any());
    }
    
    @Test
    public void shouldFindAndSaveAlbumArtwork() {
        
        Artwork artwork = artwork();
        Song song = song()
                .setArtwork(artwork);
        when(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).thenReturn(song);
        when(albumRepository.save(any())).then(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(song.getAlbum()).getArtwork())
                .isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveAlbumArtworkWhenSongsWithArtworkNotFound() {

        when(songRepository.findFirstByAlbumIdAndArtworkNotNull(any())).thenReturn(null);
        Album album = new Album()
                .setArtist(new Artist());

        assertThat(libraryArtworkFinder.findAndSaveAlbumArtwork(album).getArtwork()).isNull();
        verify(albumRepository, never()).save(any());
    }

    @Test
    public void shouldFindAndSaveArtistArtwork() {
        
        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(3L);
        Artwork artwork = artwork();
        Artist artist = new Artist();
        Album album = new Album()
                .setArtist(artist)
                .setArtwork(artwork);
        Pageable requiredSongPageable = PageRequest.of(1, 1, Sort.Direction.ASC, "year");
        when(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(ImmutableList.of(album)));
        when(artistRepository.save(any())).then(returnsFirstArg());
        
        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isSameAs(artwork);
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenAlbumsWithArtworkNotFound() {

        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(0L);
        Artist artist = new Artist();

        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save(any());
    }

    @Test
    public void shouldNotSaveArtistArtworkWhenMiddleAlbumNotFound() {

        when(albumRepository.countByArtistIdAndArtworkNotNull(any())).thenReturn(1L);
        Pageable requiredSongPageable = PageRequest.of(0, 1, Sort.Direction.ASC, "year");
        when(albumRepository.findByArtistIdAndArtworkNotNull(any(), eq(requiredSongPageable)))
                .thenReturn(new PageImpl<>(emptyList()));
        Artist artist = new Artist();

        assertThat(libraryArtworkFinder.findAndSaveArtistArtwork(artist).getArtwork()).isNull();
        verify(artistRepository, never()).save(any());
    }
}