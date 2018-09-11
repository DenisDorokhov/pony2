package net.dorokhov.pony.core.library.service.scan;

import net.dorokhov.pony.api.library.domain.Artwork;
import net.dorokhov.pony.api.library.domain.ArtworkFiles;
import net.dorokhov.pony.api.library.domain.FileType;
import net.dorokhov.pony.api.library.domain.Song;
import net.dorokhov.pony.api.library.domain.ReadableAudioData;
import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static net.dorokhov.pony.test.ArtworkFixtures.artworkBuilder;
import static net.dorokhov.pony.test.ArtworkFixtures.artworkFiles;
import static net.dorokhov.pony.test.ReadableAudioDataFixtures.readableAudioDataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryImporterSongTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Song> songCaptor;

    @Test
    public void shouldCreateSong() {

        AudioNode audioNode = audioNode();
        ReadableAudioData audioData = readableAudioDataBuilder()
                .size(100L)
                .duration(1000L)
                .bitRate(128L)
                .bitRateVariable(true)
                .discNumber(1)
                .discCount(2)
                .trackNumber(3)
                .trackCount(4)
                .title("songName")
                .genre("songGenre")
                .artist("songArtist")
                .albumArtist("songAlbumArtist")
                .album("someAlbum")
                .year(1986)
                .build();

        Song song = libraryImporter.importAudioData(audioNode, audioData);

        verify(songRepository).save((Song) any());
        assertThat(song.getPath()).isEqualTo(audioNode.getFile().getAbsolutePath());
        assertThat(song.getFileType()).isEqualTo(audioData.getFileType());
        assertThat(song.getSize()).isEqualTo(audioData.getSize());
        assertThat(song.getDuration()).isEqualTo(audioData.getDuration());
        assertThat(song.getBitRate()).isEqualTo(audioData.getBitRate());
        assertThat(song.isBitRateVariable()).isEqualTo(audioData.isBitRateVariable());
        assertThat(song.getDiscNumber()).isEqualTo(audioData.getDiscNumber());
        assertThat(song.getDiscCount()).isEqualTo(audioData.getDiscCount());
        assertThat(song.getTrackNumber()).isEqualTo(audioData.getTrackNumber());
        assertThat(song.getTrackCount()).isEqualTo(audioData.getTrackCount());
        assertThat(song.getName()).isEqualTo(audioData.getTitle());
        assertThat(song.getGenreName()).isEqualTo(audioData.getGenre());
        assertThat(song.getArtistName()).isEqualTo(audioData.getArtist());
        assertThat(song.getAlbumArtistName()).isEqualTo(audioData.getAlbumArtist());
        assertThat(song.getAlbumName()).isEqualTo(audioData.getAlbum());
        assertThat(song.getYear()).isEqualTo(audioData.getYear());
        assertThat(song.getArtwork()).isNull();
        assertThat(song.getGenre().getName()).isEqualTo(audioData.getGenre());
        assertThat(song.getAlbum().getName()).isEqualTo(audioData.getAlbum());
        assertThat(song.getAlbum().getYear()).isEqualTo(audioData.getYear());
        assertThat(song.getAlbum().getArtist().getName()).isEqualTo(audioData.getAlbumArtist());
    }

    @Test
    public void shouldUpdateSongIfFileTypeChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .fileType(FileType.of("audio/mpeg", "mp3")));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .fileType(FileType.of("audio/ogg", "ogg"))
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getFileType())
                .isEqualTo(FileType.of("audio/ogg", "ogg"));
    }

    @Test
    public void shouldUpdateSongIfSizeChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .size(1L));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .size(2L)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getSize()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfDurationChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .duration(1L));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .duration(2L)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDuration()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfBitrateChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .bitRate(1L));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .bitRate(2L)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getBitRate()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfBitrateVariableChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .bitRateVariable(false));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .bitRateVariable(true)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().isBitRateVariable()).isEqualTo(true);
    }

    @Test
    public void shouldUpdateSongIfDiscNumberChanged() {
        
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .discNumber(1));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .discNumber(2)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDiscNumber()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfDiscCountChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .discCount(1));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .discCount(2)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDiscCount()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfTrackNumberChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .trackNumber(1));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .trackNumber(2)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getTrackNumber()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfTrackCountChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .trackCount(1));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .trackCount(2)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getTrackCount()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .name("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .title("value")
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfGenreNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .genreName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .genre("value")
                .build());

        verify(songRepository).save(songCaptor.capture());
        verify(libraryCleaner).deleteGenreIfUnused(any());
        assertThat(songCaptor.getValue().getGenreName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfArtistNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .artistName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .artist("value")
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getArtistName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfAlbumArtistNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .albumArtistName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .albumArtist("value")
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getAlbumArtistName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfAlbumNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .albumName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .album("value")
                .build());

        verify(songRepository).save(songCaptor.capture());
        verify(libraryCleaner).deleteAlbumIfUnused(any());
        verify(libraryCleaner).deleteArtistIfUnused(any());
        assertThat(songCaptor.getValue().getAlbumName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfYearNameChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder
                .year(1986));

        libraryImporter.importAudioData(audioNode(), audioDataBuilder
                .year(1960)
                .build());

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getYear()).isEqualTo(1960);
    }

    @Test
    public void shouldUpdateSongIfArtworkChanged() throws IOException {
        
        Artwork existingArtwork = artworkBuilder().id("1").build();
        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder.artwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artworkBuilder().id("2").build());
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);
        
        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());
        
        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getArtwork()).isEqualTo(newArtworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipSongIfNothingChanged() {

        ReadableAudioData.Builder audioDataBuilder = mockExistingSong(builder -> builder);

        libraryImporter.importAudioData(audioNode(), audioDataBuilder.build());

        verify(songRepository, never()).save((Song) any());
    }
}