package net.dorokhov.pony.audio;

import com.google.common.io.Files;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
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
    
    private static final FileType FILE_TYPE_MP3 = new FileType("audio/mpeg", "mp3");
    private static final FileType FILE_TYPE_PNG = new FileType("image/png", "png");
    
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
        assertThat(new ReadableAudioData.Builder().build().toString()).startsWith("ReadableAudioData{");
        assertThat(new WritableAudioData.Builder().build().toString()).startsWith("WritableAudioData{");
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
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isEmpty();
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
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).hasValue("песня");
        assertThat(data.getArtist()).hasValue("артист");
        assertThat(data.getAlbumArtist()).hasValue("артист альбома");
        assertThat(data.getAlbum()).hasValue("альбом");
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).hasValue("хэви метал");
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3DiscNumber() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setDiscNumber(1)
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(1);
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }
    
    @Test
    public void writeMp3DiscCount() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setDiscCount(1)
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).hasValue(1);
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3TrackNumber() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setTrackNumber(1)
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).hasValue(1);
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3TrackCount() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setTrackCount(1)
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).hasValue(1);
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3Title() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setTitle("значение")
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).hasValue("значение");
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3Artist() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setArtist("значение")
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).hasValue("значение");
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3AlbumArtist() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setAlbumArtist("значение")
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).hasValue("значение");
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }
    
    @Test
    public void writeMp3Album() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setAlbum("значение")
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).hasValue("значение");
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3Year() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setYear(1986)
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3Genre() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setGenre("значение")
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).hasValue("значение");
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void writeMp3Artwork() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .setArtworkFile(FILE_ARTWORK.getFile())
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        checkArtwork(data);
    }

    @Test
    public void writeMp3All() throws Exception {
        createTempFile(FILE_EMPTY);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
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
                .setArtworkFile(FILE_ARTWORK.getFile())
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkId3V2Mp3(writeToFile, data);
    }

    @Test
    public void deleteMp3DiscNumber() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetDiscNumber()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3DiscCount() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetDiscCount()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3TrackNumber() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetTrackNumber()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3TrackCount() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetTrackCount()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Title() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetTitle()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Artist() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetArtist()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3AlbumArtist() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetAlbumArtist()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Album() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetAlbum()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Year() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetYear()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Genre() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetGenre()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
        checkArtwork(data);
    }

    @Test
    public void deleteMp3Artwork() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetArtworkFile()
                .build();
        ReadableAudioData data = audioTagger.write(writeToFile, dataWritable);
        checkMp3File(writeToFile, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }

    @Test
    public void deleteMp3All() throws Exception {
        createTempFile(FILE_ID3V2);
        WritableAudioData dataWritable = new WritableAudioData.Builder()
                .unsetDiscNumber()
                .unsetDiscCount()
                .unsetTrackNumber()
                .unsetTrackCount()
                .unsetTitle()
                .unsetArtist()
                .unsetAlbumArtist()
                .unsetAlbum()
                .unsetYear()
                .unsetGenre()
                .unsetArtworkFile()
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
        assertThat(data.getDiscNumber()).isEmpty();
        assertThat(data.getDiscCount()).isEmpty();
        assertThat(data.getTrackNumber()).isEmpty();
        assertThat(data.getTrackCount()).isEmpty();
        assertThat(data.getTitle()).isEmpty();
        assertThat(data.getArtist()).isEmpty();
        assertThat(data.getAlbumArtist()).isEmpty();
        assertThat(data.getAlbum()).isEmpty();
        assertThat(data.getYear()).isEmpty();
        assertThat(data.getGenre()).isEmpty();
        assertThat(data.getEmbeddedArtwork()).isEmpty();
    }
    
    private void checkId3V2Mp3(File file, ReadableAudioData data) {
        checkMp3File(file, data);
        assertThat(data.getDiscNumber()).hasValue(2);
        assertThat(data.getDiscCount()).hasValue(3);
        assertThat(data.getTrackNumber()).hasValue(3);
        assertThat(data.getTrackCount()).hasValue(10);
        assertThat(data.getTitle()).hasValue("someTitle");
        assertThat(data.getArtist()).hasValue("someArtist");
        assertThat(data.getAlbumArtist()).hasValue("someAlbumArtist");
        assertThat(data.getAlbum()).hasValue("someAlbum");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Heavy Metal");
        assertThat(data.getEmbeddedArtwork()).isNotEmpty();
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
        data.getEmbeddedArtwork().ifPresent(embeddedArtwork -> {
            try {
                assertThat(embeddedArtwork.getBinaryData().size()).isEqualTo(3002);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(embeddedArtwork.getFileType()).isEqualTo(FILE_TYPE_PNG);
            assertThat(embeddedArtwork.getChecksum()).isEqualTo("fc3adeae14ecc5f77d6dde58d40b1559");
        });
    }
}
