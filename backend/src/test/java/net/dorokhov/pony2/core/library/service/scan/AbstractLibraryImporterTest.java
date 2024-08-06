package net.dorokhov.pony2.core.library.service.scan;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.repository.ArtistRepository;
import net.dorokhov.pony2.core.library.repository.GenreRepository;
import net.dorokhov.pony2.core.library.repository.SongRepository;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static net.dorokhov.pony2.test.ReadableAudioDataFixtures.readableAudioData;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setUp() {
        lenient().when(genreRepository.save(any())).then(returnsFirstArg());
        lenient().when(artistRepository.save(any())).then(returnsFirstArg());
        lenient().when(albumRepository.save(any())).then(returnsFirstArg());
        lenient().when(songRepository.save(any())).then(returnsFirstArg());
    }

    protected AudioNode audioNode() {
        AudioNode audioNode = mock(AudioNode.class);
        when(audioNode.getFile()).thenReturn(new File("someFile"));
        return audioNode;
    }

    protected ReadableAudioData mockExistingSong(UnaryOperator<Song> songModifier) {
        return mockExistingSong(songModifier, song -> song);
    }

    protected ReadableAudioData mockExistingSong(UnaryOperator<Song> songModifier,
                                                       UnaryOperator<Album> albumModifier) {
        Genre existingGenre = new Genre().setId("1");
        Artist existingArtist = new Artist().setId("2");
        Album existingAlbum = albumModifier.apply(new Album().setId("3").setArtist(existingArtist));
        Song existingSong = songModifier.apply(
                new Song()
                        .setId("4")
                        .setPath(audioNode().getFile().getAbsolutePath())
                        .setFileType(FileType.of("audio/mpeg", "mp3"))
                        .setSize(10L)
                        .setDuration(100L)
                        .setBitRate(1000L)
                        .setBitRateVariable(true)
                        .setAlbum(existingAlbum)
                        .setGenre(existingGenre));
        when(genreRepository.findByName(any())).thenAnswer(answer ->
                answer.getArgument(0) == null ? existingGenre : null);
        when(artistRepository.findByName(any())).thenAnswer(answer ->
                answer.getArgument(0) == null ? existingArtist : null);
        when(albumRepository.findByArtistIdAndName(any(), any())).thenAnswer(answer ->
                Objects.equals("2", answer.getArgument(0)) && answer.getArgument(1) == null ? existingAlbum : null);
        when(songRepository.findByPath(eq(audioNode().getFile().getAbsolutePath()))).thenReturn(existingSong);
        return readableAudioData()
                .setSize(10L)
                .setDuration(100L)
                .setBitRate(1000L)
                .setBitRateVariable(true);
    }
}
