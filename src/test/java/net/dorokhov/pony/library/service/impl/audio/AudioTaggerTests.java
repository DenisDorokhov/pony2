package net.dorokhov.pony.library.service.impl.audio;

import com.google.common.io.Files;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AudioTaggerTests {
    
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
    
    private AudioTagger audioTagger;
    
    private File writeToFile;

    @Before
    public void setUp() throws Exception {
        audioTagger = new AudioTagger(new FileTypeResolver(), new ChecksumCalculator());
    }

    @After
    public void tearDown() throws Exception {
        if (writeToFile != null) {
            if (!writeToFile.delete()) {
                throw new RuntimeException("Could not delete temporary file.");
            }
            writeToFile = null;
        }
    }

    @Test
    public void stringify() throws Exception {
        assertThat(ReadableAudioData.builder()
                .path("somePath")
                .fileType(FileType.of("text/plain", "txt"))
                .build().toString()).startsWith("ReadableAudioData{");
        assertThat(WritableAudioData.builder()
                .build().toString()).startsWith("WritableAudioData{");
    }

    @Test
    public void readEmptyMp3() throws Exception {
        File file = FILE_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void readId3V1Mp3() throws Exception {
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
    public void readEmptyId3V1Mp3() throws Exception {
        File file = FILE_ID3V1_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void readId3V1Id3V2Mp3() throws Exception {
        File file = FILE_ID3V1_ID3V2.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkId3V2Mp3(file, data);
    }

    @Test
    public void readId3V2Mp3() throws Exception {
        File file = FILE_ID3V2.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkId3V2Mp3(file, data);
    }
    
    @Test
    public void readEmptyId3V2Mp3() throws Exception {
        File file = FILE_ID3V2_EMPTY.getFile();
        ReadableAudioData data = audioTagger.read(file);
        checkEmptyMp3(file, data);
    }

    @Test
    public void readId3V2Unicode() throws Exception {
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
    public void writeMp3DiscNumber() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3DiscCount() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3TrackNumber() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3TrackCount() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Title() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Artist() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3AlbumArtist() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Album() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Year() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Genre() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3Artwork() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void writeMp3All() throws Exception {
        createTempFile(FILE_EMPTY);
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
    public void deleteMp3DiscNumber() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3DiscCount() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3TrackNumber() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3TrackCount() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Title() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Artist() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3AlbumArtist() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Album() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Year() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Genre() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3Artwork() throws Exception {
        createTempFile(FILE_ID3V2);
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
    public void deleteMp3All() throws Exception {
        createTempFile(FILE_ID3V2);
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
    
    private void createTempFile(Resource resource) throws IOException {
        writeToFile = File.createTempFile(getClass().getSimpleName(), ".mp3");
        Files.copy(resource.getFile(), writeToFile);
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
                assertThat(embeddedArtwork.getBinaryData().size()).isEqualTo(3002);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(embeddedArtwork.getFileType()).isEqualTo(FILE_TYPE_PNG);
            assertThat(embeddedArtwork.getChecksum()).isEqualTo("fc3adeae14ecc5f77d6dde58d40b1559");
        }
    }
}
