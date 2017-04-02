package net.dorokhov.pony.audio;

import net.dorokhov.pony.audio.AudioDataReadable.EmbeddedArtwork;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class AudioTaggerTests {
    
    private static final Resource TEST_MP3_FILE = new ClassPathResource("metallica-battery.mp3");
    
    private AudioTagger audioTagger;

    @Before
    public void setUp() throws Exception {
        audioTagger = new AudioTagger(new FileTypeResolver(), new ChecksumCalculator());
    }

    @Test
    public void readMp3Data() throws Exception {
        AudioDataReadable data = audioTagger.read(TEST_MP3_FILE.getFile());
        assertThat(data.getPath()).isEqualTo(TEST_MP3_FILE.getFile().getAbsolutePath());
        assertThat(data.getFileType()).isEqualTo(new FileType("audio/mpeg", "mp3"));
        assertThat(data.getSize()).isEqualTo(24797);
        assertThat(data.getDuration()).isEqualTo(1);
        assertThat(data.getBitRate()).isEqualTo(128);
        assertThat(data.isBitRateVariable()).isFalse();
        assertThat(data.getDiscNumber()).hasValue(1);
        assertThat(data.getDiscCount()).hasValue(1);
        assertThat(data.getTrackNumber()).hasValue(1);
        assertThat(data.getTrackCount()).hasValue(8);
        assertThat(data.getTitle()).hasValue("Battery");
        assertThat(data.getArtist()).hasValue("Metallica");
        assertThat(data.getAlbumArtist()).hasValue("Metallica");
        assertThat(data.getAlbum()).hasValue("Master Of Puppets");
        assertThat(data.getYear()).hasValue(1986);
        assertThat(data.getGenre()).hasValue("Rock");
        assertThat(data.getEmbeddedArtwork().map(EmbeddedArtwork::getBinaryData)).isNotEmpty();
        assertThat(data.getEmbeddedArtwork().map(EmbeddedArtwork::getFileType)).hasValue(new FileType("image/png", "png"));
        assertThat(data.getEmbeddedArtwork().map(EmbeddedArtwork::getChecksum)).hasValue("0a6632570700e5f595a75999508fc46d");
    }
}
