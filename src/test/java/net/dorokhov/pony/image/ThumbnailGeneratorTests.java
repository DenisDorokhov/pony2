package net.dorokhov.pony.image;

import com.google.common.io.ByteStreams;
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
    private static final ImageSize THUMBNAIL_SIZE = new ImageSize(50, 50);
    
    private ThumbnailGenerator thumbnailGenerator;
    
    private File targetFile;

    @Before
    public void setUp() throws Exception {
        thumbnailGenerator = new ThumbnailGenerator();
    }

    @After
    public void tearDown() throws Exception {
        if (targetFile != null) {
            targetFile.delete();
            targetFile = null;
        }
    }

    @Test
    public void generateFromInputStream() throws Exception {
        targetFile = createTempFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(stream, THUMBNAIL_SIZE, targetFile);
        }
        checkTargetFile();
    }

    @Test
    public void generateFromBytes() throws Exception {
        targetFile = createTempFile();
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            thumbnailGenerator.generateThumbnail(ByteStreams.toByteArray(stream), THUMBNAIL_SIZE, targetFile);
        }
        checkTargetFile();
    }

    @Test
    public void generateFromFile() throws Exception {
        targetFile = createTempFile();
        thumbnailGenerator.generateThumbnail(IMAGE_RESOURCE.getFile(), THUMBNAIL_SIZE, targetFile);
        checkTargetFile();
    }
    
    private File createTempFile() throws IOException {
        return File.createTempFile(getClass().getSimpleName(), ".tmp");
    }
    
    private void checkTargetFile() throws IOException {
        BufferedImage targetImage = ImageIO.read(targetFile);
        assertThat(targetImage.getWidth()).isEqualTo(45);
        assertThat(targetImage.getHeight()).isEqualTo(50);
    }
}
