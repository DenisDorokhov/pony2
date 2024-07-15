package net.dorokhov.pony3.core.library.service.scan;

import net.dorokhov.pony3.api.library.domain.*;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static net.dorokhov.pony3.test.ArtworkFixtures.artwork;
import static net.dorokhov.pony3.test.ArtworkFixtures.artworkFiles;
import static net.dorokhov.pony3.test.ReadableAudioDataFixtures.readableAudioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryImporterSongTest extends AbstractLibraryImporterTest {

    @Captor
    private ArgumentCaptor<Song> songCaptor;

    @Test
    public void shouldCreateSong() {

        AudioNode audioNode = audioNode();
        ReadableAudioData audioData = readableAudioData()
                .setSize(100L)
                .setDuration(1000L)
                .setBitRate(128L)
                .setBitRateVariable(true)
                .setDiscNumber(1)
                .setDiscCount(2)
                .setTrackNumber(3)
                .setTrackCount(4)
                .setTitle("songName")
                .setGenre("songGenre")
                .setArtist("songArtist")
                .setAlbumArtist("songAlbumArtist")
                .setAlbum("someAlbum")
                .setYear(1986);

        Song song = libraryImporter.importAudioData(audioNode, audioData);

        verify(songRepository).save(any());
        assertThat(song.getPath()).isEqualTo(audioNode.getFile().getAbsolutePath());
        assertThat(song.getFileType()).isEqualTo(audioData.getFileType());
        assertThat(song.getSize()).isEqualTo(audioData.getSize());
        assertThat(song.getDuration()).isEqualTo(audioData.getDuration());
        assertThat(song.getBitRate()).isEqualTo(audioData.getBitRate());
        assertThat(song.getBitRateVariable()).isEqualTo(audioData.isBitRateVariable());
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

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setFileType(FileType.of("audio/mpeg", "mp3")));

        libraryImporter.importAudioData(audioNode(), audioData
                .setFileType(FileType.of("audio/ogg", "ogg")));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getFileType())
                .isEqualTo(FileType.of("audio/ogg", "ogg"));
    }

    @Test
    public void shouldUpdateSongIfSizeChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setSize(1L));

        libraryImporter.importAudioData(audioNode(), audioData
                .setSize(2L));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getSize()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfDurationChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setDuration(1L));

        libraryImporter.importAudioData(audioNode(), audioData
                .setDuration(2L));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDuration()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfBitrateChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setBitRate(1L));

        libraryImporter.importAudioData(audioNode(), audioData
                .setBitRate(2L));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getBitRate()).isEqualTo(2L);
    }

    @Test
    public void shouldUpdateSongIfBitrateVariableChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setBitRateVariable(false));

        libraryImporter.importAudioData(audioNode(), audioData
                .setBitRateVariable(true));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getBitRateVariable()).isEqualTo(true);
    }

    @Test
    public void shouldUpdateSongIfDiscNumberChanged() {
        
        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setDiscNumber(1));

        libraryImporter.importAudioData(audioNode(), audioData
                .setDiscNumber(2));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDiscNumber()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfDiscCountChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setDiscCount(1));

        libraryImporter.importAudioData(audioNode(), audioData
                .setDiscCount(2));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getDiscCount()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfTrackNumberChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setTrackNumber(1));

        libraryImporter.importAudioData(audioNode(), audioData
                .setTrackNumber(2));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getTrackNumber()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfTrackCountChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setTrackCount(1));

        libraryImporter.importAudioData(audioNode(), audioData
                .setTrackCount(2));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getTrackCount()).isEqualTo(2);
    }

    @Test
    public void shouldUpdateSongIfNameChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioData
                .setTitle("value"));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfGenreNameChanged() {

        ReadableAudioData audioData = mockExistingSong(song -> song.setGenreName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioData.setGenre("value"));

        verify(songRepository).save(songCaptor.capture());
        verify(libraryCleaner).deleteGenreIfUnused(any());
        assertThat(songCaptor.getValue().getGenreName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfArtistNameChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setArtistName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioData
                .setArtist("value"));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getArtistName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfAlbumArtistNameChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setAlbumArtistName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioData
                .setAlbumArtist("value"));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getAlbumArtistName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfAlbumNameChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setAlbumName("oldValue"));

        libraryImporter.importAudioData(audioNode(), audioData
                .setAlbum("value"));

        verify(songRepository).save(songCaptor.capture());
        verify(libraryCleaner).deleteAlbumIfUnused(any());
        verify(libraryCleaner).deleteArtistIfUnused(any());
        assertThat(songCaptor.getValue().getAlbumName()).isEqualTo("value");
    }

    @Test
    public void shouldUpdateSongIfYearNameChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder
                .setYear(1986));

        libraryImporter.importAudioData(audioNode(), audioData
                .setYear(1960));

        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getYear()).isEqualTo(1960);
    }

    @Test
    public void shouldUpdateSongIfArtworkChanged() throws IOException {
        
        Artwork existingArtwork = artwork().setId("1");
        ReadableAudioData audioData = mockExistingSong(song -> song.setArtwork(existingArtwork));
        ArtworkFiles newArtworkFiles = artworkFiles(artwork().setId("2"));
        when(libraryArtworkFinder.findAndSaveEmbeddedArtwork(any())).thenReturn(newArtworkFiles);
        
        libraryImporter.importAudioData(audioNode(), audioData);
        
        verify(songRepository).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getArtwork()).isEqualTo(newArtworkFiles.getArtwork());
    }

    @Test
    public void shouldSkipSongIfNothingChanged() {

        ReadableAudioData audioData = mockExistingSong(builder -> builder);

        libraryImporter.importAudioData(audioNode(), audioData);

        verify(songRepository, never()).save(any());
    }
}