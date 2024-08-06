package net.dorokhov.pony2.core.library.service;

import com.google.common.io.Files;
import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData;
import net.dorokhov.pony2.api.library.domain.WritableAudioData;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AudioTaggerTest {

    private static final Resource FILE_EMPTY = new ClassPathResource("audio/empty.mp3");
    private static final Resource FILE_ID3V1 = new ClassPathResource("audio/id3v1.mp3");
    private static final Resource FILE_ID3V1_EMPTY = new ClassPathResource("audio/id3v1-empty.mp3");
    private static final Resource FILE_ID3V1_ID3V2 = new ClassPathResource("audio/id3v1-id3v2.mp3");
    private static final Resource FILE_ID3V2 = new ClassPathResource("audio/id3v2.mp3");
    private static final Resource FILE_ID3V2_EMPTY = new ClassPathResource("audio/id3v2-empty.mp3");
    private static final Resource FILE_ID3V2_UNICODE = new ClassPathResource("audio/id3v2-unicode.mp3");
    private static final Resource FILE_ID3V2_YEAR_AS_DATE = new ClassPathResource("audio/id3v2-year-as-date.mp3");
    private static final Resource FILE_ARTWORK = new ClassPathResource("image.png");

    private static final FileType FILE_TYPE_MP3 = FileType.of("audio/mpeg", "mp3");
    private static final FileType FILE_TYPE_PNG = FileType.of("image/png", "png");

    @TempDir
    public Path tempFolder;

    @InjectMocks
    private AudioTagger audioTagger;

    @Mock
    private FileTypeResolver fileTypeResolver;

    @BeforeEach
    public void setUp() {
        when(fileTypeResolver.resolve((File) any())).thenReturn(FileType.of("audio/mpeg", "mp3"));
        lenient().when(fileTypeResolver.resolve((byte[]) any())).thenReturn(FileType.of("image/png", "png"));
    }

    @Test
    public void shouldReadEmptyMp3() throws IOException {

        File file = FILE_EMPTY.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V1Mp3() throws IOException {

        File file = FILE_ID3V1.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkMp3File(file, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldReadEmptyId3V1Mp3() throws IOException {

        File file = FILE_ID3V1_EMPTY.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V1Id3V2Mp3() throws IOException {

        File file = FILE_ID3V1_ID3V2.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkId3V2Mp3(file, data);
    }

    @Test
    public void shouldReadId3V2Mp3() throws IOException {

        File file = FILE_ID3V2.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkId3V2Mp3(file, data);
    }

    @Test
    public void shouldReadEmptyId3V2Mp3() throws IOException {

        File file = FILE_ID3V2_EMPTY.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V2Unicode() throws IOException {

        File file = FILE_ID3V2_UNICODE.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkMp3File(file, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isEqualTo("песня");
        assertThat(data.getArtist()).isEqualTo("артист");
        assertThat(data.getAlbumArtist()).isEqualTo("артист альбома");
        assertThat(data.getAlbum()).isEqualTo("альбом");
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isEqualTo("хэви метал");
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldReadId3V2YearAsDate() throws IOException {

        File file = FILE_ID3V2_YEAR_AS_DATE.getFile();

        ReadableAudioData data = audioTagger.read(file);

        checkMp3File(file, data);
        assertThat(data.getYear()).isEqualTo(2024);
    }

    @Test
    public void shouldWriteMp3DiscNumber() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscNumber(1);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(1);
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3DiscCount() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscCount(1);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isEqualTo(1);
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3TrackNumber() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTrackNumber(1);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isEqualTo(1);
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3TrackCount() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTrackCount(1);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isEqualTo(1);
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Title() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTitle("значение");

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isEqualTo("значение");
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Artist() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setArtist("значение");

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isEqualTo("значение");
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3AlbumArtist() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setAlbumArtist("значение");

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isEqualTo("значение");
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Album() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setAlbum("значение");

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isEqualTo("значение");
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Year() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setYear(1986);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Genre() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setGenre("значение");

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isEqualTo("значение");
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldWriteMp3Artwork() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setArtworkFile(FILE_ARTWORK.getFile());

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        checkArtwork(data);
    }

    @Test
    public void shouldWriteMp3All() throws IOException {

        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscNumber(2)
                .setDiscCount(3)
                .setTrackNumber(3)
                .setTrackCount(10)
                .setTitle("someTitle")
                .setArtist("someArtist")
                .setAlbumArtist("someAlbumArtist")
                .setAlbum("someAlbum")
                .setYear(1986)
                .setGenre("Heavy Metal")
                .setArtworkFile(FILE_ARTWORK.getFile());

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkId3V2Mp3(writeToFile, data);
    }

    @Test
    public void shouldDeleteMp3DiscNumber() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscNumber(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3DiscCount() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscCount(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3TrackNumber() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTrackNumber(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3TrackCount() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTrackCount(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Title() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setTitle(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Artist() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setArtist(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3AlbumArtist() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setAlbumArtist(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Album() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setAlbum(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Year() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setYear(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Genre() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setGenre(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    @Test
    public void shouldDeleteMp3Artwork() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setArtworkFile(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    @Test
    public void shouldDeleteMp3All() throws IOException {

        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData()
                .setDiscNumber(null)
                .setDiscCount(null)
                .setTrackNumber(null)
                .setTrackCount(null)
                .setTitle(null)
                .setArtist(null)
                .setAlbumArtist(null)
                .setAlbum(null)
                .setYear(null)
                .setGenre(null)
                .setArtworkFile(null);

        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);

        checkEmptyMp3(writeToFile, data);
    }

    private File copyToTempFile(Resource resource) throws IOException {
        File writeToFile = tempFolder.resolve("test.mp3").toFile();
        Files.copy(resource.getFile(), writeToFile);
        return writeToFile;
    }

    private void checkEmptyMp3(File file, ReadableAudioData data) {
        checkMp3File(file, data);
        assertThat(data.getDiscNumber()).isNull();
        assertThat(data.getDiscCount()).isNull();
        assertThat(data.getTrackNumber()).isNull();
        assertThat(data.getTrackCount()).isNull();
        assertThat(data.getTitle()).isNull();
        assertThat(data.getArtist()).isNull();
        assertThat(data.getAlbumArtist()).isNull();
        assertThat(data.getAlbum()).isNull();
        assertThat(data.getYear()).isNull();
        assertThat(data.getGenre()).isNull();
        assertThat(data.getEmbeddedArtwork()).isNull();
    }

    private void checkId3V2Mp3(File file, ReadableAudioData data) {
        checkMp3File(file, data);
        assertThat(data.getDiscNumber()).isEqualTo(2);
        assertThat(data.getDiscCount()).isEqualTo(3);
        assertThat(data.getTrackNumber()).isEqualTo(3);
        assertThat(data.getTrackCount()).isEqualTo(10);
        assertThat(data.getTitle()).isEqualTo("someTitle");
        assertThat(data.getArtist()).isEqualTo("someArtist");
        assertThat(data.getAlbumArtist()).isEqualTo("someAlbumArtist");
        assertThat(data.getAlbum()).isEqualTo("someAlbum");
        assertThat(data.getYear()).isEqualTo(1986);
        assertThat(data.getGenre()).isEqualTo("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotNull();
        checkArtwork(data);
    }

    private void checkMp3File(File file, ReadableAudioData data) {
        assertThat(data.getPath()).isEqualTo(file.getAbsolutePath());
        assertThat(data.getFileType()).isEqualTo(FILE_TYPE_MP3);
        assertThat(data.getSize()).isEqualTo(file.length());
        assertThat(data.getDuration()).isEqualTo(1);
        assertThat(data.getBitRate()).isEqualTo(128);
        assertThat(data.isBitRateVariable()).isFalse();
    }

    private void checkArtwork(ReadableAudioData data) {
        ReadableAudioData.EmbeddedArtwork embeddedArtwork = data.getEmbeddedArtwork();
        if (embeddedArtwork != null) {
            try {
                assertThat(embeddedArtwork.getBinaryData().size()).isEqualTo(4285L);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(embeddedArtwork.getFileType()).isEqualTo(FILE_TYPE_PNG);
        }
    }
}
