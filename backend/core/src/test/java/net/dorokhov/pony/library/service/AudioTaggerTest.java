package net.dorokhov.pony.library.service;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.domain.ReadableAudioData;
import net.dorokhov.pony.library.domain.WritableAudioData;
import net.dorokhov.pony.library.service.file.FileTypeResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AudioTaggerTest {
    
    private static final Resource FILE_EMPTY = new ClassPathResource("audio/empty.mp3");
    private static final Resource FILE_ID3V1 = new ClassPathResource("audio/id3v1.mp3");
    private static final Resource FILE_ID3V1_EMPTY = new ClassPathResource("audio/id3v1-empty.mp3");
    private static final Resource FILE_ID3V1_ID3V2 = new ClassPathResource("audio/id3v1-id3v2.mp3");
    private static final Resource FILE_ID3V2 = new ClassPathResource("audio/id3v2.mp3");
    private static final Resource FILE_ID3V2_EMPTY = new ClassPathResource("audio/id3v2-empty.mp3");
    private static final Resource FILE_ID3V2_UNICODE = new ClassPathResource("audio/id3v2-unicode.mp3");
    private static final Resource FILE_ARTWORK = new ClassPathResource("image.png");
    
    private static final FileType FILE_TYPE_MP3 = FileType.of("audio/mpeg", "mp3");
    private static final FileType FILE_TYPE_PNG = FileType.of("image/png", "png");

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @InjectMocks
    private AudioTagger audioTagger;
    
    @Mock
    private FileTypeResolver fileTypeResolver;

    @Before
    public void setUp() throws Exception {
        when(fileTypeResolver.resolve((File) any())).thenReturn(FileType.of("audio/mpeg", "mp3"));
        when(fileTypeResolver.resolve((byte[]) any())).thenReturn(FileType.of("image/png", "png"));
    }

    @Test
    public void shouldReadEmptyMp3() throws Exception {
        File file = FILE_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V1Mp3() throws Exception {
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
    public void shouldReadEmptyId3V1Mp3() throws Exception {
        File file = FILE_ID3V1_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V1Id3V2Mp3() throws Exception {
        File file = FILE_ID3V1_ID3V2.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkId3V2Mp3(file, data);
    }

    @Test
    public void shouldReadId3V2Mp3() throws Exception {
        File file = FILE_ID3V2.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkId3V2Mp3(file, data);
    }
    
    @Test
    public void shouldReadEmptyId3V2Mp3() throws Exception {
        File file = FILE_ID3V2_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void shouldReadId3V2Unicode() throws Exception {
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
    public void shouldWriteMp3DiscNumber() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .discNumber(1)
                .build();
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
    public void shouldWriteMp3DiscCount() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .discCount(1)
                .build();
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
    public void shouldWriteMp3TrackNumber() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .trackNumber(1)
                .build();
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
    public void shouldWriteMp3TrackCount() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .trackCount(1)
                .build();
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
    public void shouldWriteMp3Title() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .title("значение")
                .build();
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
    public void shouldWriteMp3Artist() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .artist("значение")
                .build();
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
    public void shouldWriteMp3AlbumArtist() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .albumArtist("значение")
                .build();
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
    public void shouldWriteMp3Album() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .album("значение")
                .build();
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
    public void shouldWriteMp3Year() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .year(1986)
                .build();
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
    public void shouldWriteMp3Genre() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .genre("значение")
                .build();
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
    public void shouldWriteMp3Artwork() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .artworkFile(FILE_ARTWORK.getFile())
                .build();
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
    public void shouldWriteMp3All() throws Exception {
        File writeToFile = copyToTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .discNumber(2)
                .discCount(3)
                .trackNumber(3)
                .trackCount(10)
                .title("someTitle")
                .artist("someArtist")
                .albumArtist("someAlbumArtist")
                .album("someAlbum")
                .year(1986)
                .genre("Heavy Metal")
                .artworkFile(FILE_ARTWORK.getFile())
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkId3V2Mp3(writeToFile, data);
    }

    @Test
    public void shouldDeleteMp3DiscNumber() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearDiscNumber()
                .build();
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
    public void shouldDeleteMp3DiscCount() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearDiscCount()
                .build();
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
    public void shouldDeleteMp3TrackNumber() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearTrackNumber()
                .build();
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
    public void shouldDeleteMp3TrackCount() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearTrackCount()
                .build();
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
    public void shouldDeleteMp3Title() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearTitle()
                .build();
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
    public void shouldDeleteMp3Artist() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearArtist()
                .build();
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
    public void shouldDeleteMp3AlbumArtist() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearAlbumArtist()
                .build();
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
    public void shouldDeleteMp3Album() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearAlbum()
                .build();
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
    public void shouldDeleteMp3Year() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearYear()
                .build();
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
    public void shouldDeleteMp3Genre() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearGenre()
                .build();
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
    public void shouldDeleteMp3Artwork() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearArtworkFile()
                .build();
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
    public void shouldDeleteMp3All() throws Exception {
        File writeToFile = copyToTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = WritableAudioData.builder()
                .clearDiscNumber()
                .clearDiscCount()
                .clearTrackNumber()
                .clearTrackCount()
                .clearTitle()
                .clearArtist()
                .clearAlbumArtist()
                .clearAlbum()
                .clearYear()
                .clearGenre()
                .clearArtworkFile()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkEmptyMp3(writeToFile, data);
    }
    
    private File copyToTempFile(Resource resource) throws IOException {
        File writeToFile = tempFolder.newFile("test.mp3");
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
