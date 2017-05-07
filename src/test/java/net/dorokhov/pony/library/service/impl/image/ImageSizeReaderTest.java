package net.dorokhov.pony.library.service.impl.image;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageSizeReaderTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize IMAGE_SIZE = ImageSize.of(90, 100);
    
    private final ImageSizeReader imageSizeReader = new ImageSizeReader();

    @Test
    public void shouldReadFromInputStream() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            ImageSize size = imageSizeReader.read(stream);
            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void shouldReadFromBytes() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            ImageSize size = imageSizeReader.read(ByteStreams.toByteArray(stream));
            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void shouldReadFromFile() throws Exception {
        File file = IMAGE_RESOURCE.getFile();
        ImageSize size = imageSizeReader.read(file);
        assertThat(size).isEqualTo(IMAGE_SIZE);
    }

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(ImageSize.of(100, 100).toString()).startsWith("ImageSize{");
    }
}
