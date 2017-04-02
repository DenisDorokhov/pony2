package net.dorokhov.pony.image;

import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageSizeReaderTests {
    
    private ImageSizeReader imageSizeReader;

    @Before
    public void setUp() throws Exception {
        imageSizeReader = new ImageSizeReader();
    }

    @Test
    public void readFromInputStream() throws Exception {
        try (InputStream stream = new ClassPathResource("image.png").getInputStream()) {
            ImageSize size = imageSizeReader.read(stream);
            assertThat(size.getWidth()).isEqualTo(90);
            assertThat(size.getHeight()).isEqualTo(100);
        }
    }

    @Test
    public void readFromBytes() throws Exception {
        try (InputStream stream = new ClassPathResource("image.png").getInputStream()) {
            ImageSize size = imageSizeReader.read(ByteStreams.toByteArray(stream));
            assertThat(size.getWidth()).isEqualTo(90);
            assertThat(size.getHeight()).isEqualTo(100);
        }
    }

    @Test
    public void readFromFile() throws Exception {
        File file = new ClassPathResource("image.png").getFile();
        ImageSize size = imageSizeReader.read(file);
        assertThat(size.getWidth()).isEqualTo(90);
        assertThat(size.getHeight()).isEqualTo(100);
    }
}
