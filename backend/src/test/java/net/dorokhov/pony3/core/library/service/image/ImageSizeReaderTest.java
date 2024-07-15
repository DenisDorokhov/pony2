package net.dorokhov.pony3.core.library.service.image;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony3.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageSizeReaderTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize IMAGE_SIZE = ImageSize.of(400, 444);
    
    private final ImageSizeReader imageSizeReader = new ImageSizeReader();

    @Test
    public void shouldReadFromInputStream() throws IOException {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {

            ImageSize size = imageSizeReader.read(stream);

            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void shouldReadFromBytes() throws IOException {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {

            ImageSize size = imageSizeReader.read(ByteStreams.toByteArray(stream));

            assertThat(size).isEqualTo(IMAGE_SIZE);
        }
    }

    @Test
    public void shouldReadFromFile() throws IOException {

        ImageSize size = imageSizeReader.read(IMAGE_RESOURCE.getFile());

        assertThat(size).isEqualTo(IMAGE_SIZE);
    }
}
