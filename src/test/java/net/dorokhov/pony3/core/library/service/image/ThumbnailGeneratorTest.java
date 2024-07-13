package net.dorokhov.pony3.core.library.service.image;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony3.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ThumbnailGeneratorTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize THUMBNAIL_SIZE = ImageSize.of(50, 50);

    @TempDir
    public Path tempFolder;
    
    private final ThumbnailGenerator thumbnailGenerator = new ThumbnailGenerator();
    
    @Test
    public void shouldGenerateThumbnailFromInputStream() throws IOException {

        File file = tempFolder.resolve(UUID.randomUUID().toString()).toFile();

        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(stream, THUMBNAIL_SIZE, file);
        }

        checkFile(file);
    }

    @Test
    public void shouldGenerateThumbnailFromBytes() throws IOException {

        File file = tempFolder.resolve(UUID.randomUUID().toString()).toFile();

        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(ByteStreams.toByteArray(stream), THUMBNAIL_SIZE, file);
        }

        checkFile(file);
    }

    @Test
    public void shouldGenerateThumbnailFromFile() throws IOException {

        File file = tempFolder.resolve(UUID.randomUUID().toString()).toFile();

        thumbnailGenerator.generateThumbnail(IMAGE_RESOURCE.getFile(), THUMBNAIL_SIZE, file);

        checkFile(file);
    }
    
    private void checkFile(File file) throws IOException {
        BufferedImage targetImage = ImageIO.read(file);
        assertThat(targetImage.getWidth()).isEqualTo(45);
        assertThat(targetImage.getHeight()).isEqualTo(50);
    }
}
