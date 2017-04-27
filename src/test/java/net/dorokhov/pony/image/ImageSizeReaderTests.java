package net.dorokhov.pony.image;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony.image.domain.ImageSize;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageSizeReaderTests {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize IMAGE_SIZE = ImageSize.of(90, 100);
    
    private ImageSizeReader imageSizeReader;

    @Before
    public void setUp() throws Exception {
        imageSizeReader = new ImageSizeReader();
    }

    @Test
    public void readFromInputStream() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            ImageSize size = imageSizeReader.read(stream);
            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void readFromBytes() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            ImageSize size = imageSizeReader.read(ByteStreams.toByteArray(stream));
            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void readFromFile() throws Exception {
        File file = IMAGE_RESOURCE.getFile();
        ImageSize size = imageSizeReader.read(file);
        assertThat(size).isEqualTo(IMAGE_SIZE);
    }

    @Test
    public void strongify() throws Exception {
        assertThat(ImageSize.of(100, 100).toString()).startsWith("ImageSize{");
    }
}
