package net.dorokhov.pony.core.library.service.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony.core.library.service.image.domain.ImageSize;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class ThumbnailGeneratorTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize THUMBNAIL_SIZE = ImageSize.of(50, 50);

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    private final ThumbnailGenerator thumbnailGenerator = new ThumbnailGenerator();
    
    @Test
    public void shouldGenerateThumbnailFromInputStream() throws Exception {
        File file = tempFolder.newFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(stream, THUMBNAIL_SIZE, file);
        }
        checkFile(file);
    }

    @Test
    public void shouldGenerateThumbnailFromBytes() throws Exception {
        File file = tempFolder.newFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(ByteStreams.toByteArray(stream), THUMBNAIL_SIZE, file);
        }
        checkFile(file);
    }

    @Test
    public void shouldGenerateThumbnailFromFile() throws Exception {
        File file = tempFolder.newFile();
        thumbnailGenerator.generateThumbnail(IMAGE_RESOURCE.getFile(), THUMBNAIL_SIZE, file);
        checkFile(file);
    }
    
    private void checkFile(File file) throws IOException {
        BufferedImage targetImage = ImageIO.read(file);
        assertThat(targetImage.getWidth()).isEqualTo(45);
        assertThat(targetImage.getHeight()).isEqualTo(50);
    }
}
