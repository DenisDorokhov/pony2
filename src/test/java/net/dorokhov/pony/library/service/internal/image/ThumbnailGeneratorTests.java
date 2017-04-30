package net.dorokhov.pony.library.service.internal.image;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony.library.service.internal.image.ThumbnailGenerator;
import net.dorokhov.pony.library.service.internal.image.domain.ImageSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ThumbnailGeneratorTests {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final ImageSize THUMBNAIL_SIZE = ImageSize.of(50, 50);
    
    private ThumbnailGenerator thumbnailGenerator;
    
    private File writeToFile;

    @Before
    public void setUp() throws Exception {
        thumbnailGenerator = new ThumbnailGenerator();
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
    public void generateFromInputStream() throws Exception {
        writeToFile = createTempFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(stream, THUMBNAIL_SIZE, writeToFile);
        }
        checkTargetFile();
    }

    @Test
    public void generateFromBytes() throws Exception {
        writeToFile = createTempFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(ByteStreams.toByteArray(stream), THUMBNAIL_SIZE, writeToFile);
        }
        checkTargetFile();
    }

    @Test
    public void generateFromFile() throws Exception {
        writeToFile = createTempFile();
        thumbnailGenerator.generateThumbnail(IMAGE_RESOURCE.getFile(), THUMBNAIL_SIZE, writeToFile);
        checkTargetFile();
    }
    
    private File createTempFile() throws IOException {
        return File.createTempFile(getClass().getSimpleName(), ".tmp");
    }
    
    private void checkTargetFile() throws IOException {
        BufferedImage targetImage = ImageIO.read(writeToFile);
        assertThat(targetImage.getWidth()).isEqualTo(45);
        assertThat(targetImage.getHeight()).isEqualTo(50);
    }
}
