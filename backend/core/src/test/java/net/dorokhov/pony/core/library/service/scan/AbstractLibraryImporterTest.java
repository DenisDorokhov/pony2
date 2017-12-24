package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.*;
import net.dorokhov.pony.core.library.repository.AlbumRepository;
import net.dorokhov.pony.core.library.repository.ArtistRepository;
import net.dorokhov.pony.core.library.repository.GenreRepository;
import net.dorokhov.pony.core.library.repository.SongRepository;
import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.function.UnaryOperator;

import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractLibraryImporterTest {

    @InjectMocks
    protected LibraryImporter libraryImporter;

    @Mock
    protected GenreRepository genreRepository;
    @Mock
    protected ArtistRepository artistRepository;
    @Mock
    protected AlbumRepository albumRepository;
    @Mock
    protected SongRepository songRepository;
    @Mock
    protected LibraryArtworkFinder libraryArtworkFinder;
    @Mock
    protected LibraryCleaner libraryCleaner;
    @Mock
    @SuppressWarnings("unused")
    protected LogService logService;

    @Before
    public void setUp() throws Exception {
        when(genreRepository.save((Genre) any())).then(returnsFirstArg());
        when(artistRepository.save((Artist) any())).then(returnsFirstArg());
        when(albumRepository.save((Album) any())).then(returnsFirstArg());
        when(songRepository.save((Song) any())).then(returnsFirstArg());
    }

    protected AudioNode audioNode() {
        AudioNode audioNode = mock(AudioNode.class);
        when(audioNode.getFile()).thenReturn(new File("someFile"));
        return audioNode;
    }

    protected ReadableAudioData.Builder mockExistingSong(UnaryOperator<Song.Builder> songModifier) {
        return mockExistingSong(songModifier, builder -> builder);
    }

    protected ReadableAudioData.Builder mockExistingSong(UnaryOperator<Song.Builder> songModifier,
                                                       UnaryOperator<Album.Builder> albumModifier) {
        Genre existingGenre = Genre.builder().build();
        Artist existingArtist = Artist.builder().build();
        Album existingAlbum = albumModifier.apply(Album.builder().artist(existingArtist)).build();
        Song existingSong = songModifier.apply(
                Song.builder()
                        .path(audioNode().getFile().getAbsolutePath())
                        .fileType(FileType.of("audio/mpeg", "mp3"))
                        .size(10L)
                        .duration(100L)
                        .bitRate(1000L)
                        .bitRateVariable(true)
                        .album(existingAlbum)
                        .genre(existingGenre))
                .build();
        when(genreRepository.findByName(any())).thenReturn(existingGenre);
        when(artistRepository.findByName(any())).thenReturn(existingArtist);
        when(albumRepository.findByArtistIdAndName(any(), any())).thenReturn(existingAlbum);
        when(songRepository.findByPath(any())).thenReturn(existingSong);
        return readableAudioDataBuilder()
                .size(10L)
                .duration(100L)
                .bitRate(1000L)
                .bitRateVariable(true);
    }
}
