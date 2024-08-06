package net.dorokhov.pony2.core.library.service.image;

import net.coobird.thumbnailator.Thumbnails;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class ThumbnailGenerator {
    
    public void generateThumbnail(InputStream imageStream, ImageSize maxSize, File targetFile) throws IOException {
        try (OutputStream targetStream = new FileOutputStream(targetFile)) {
            Thumbnails.of(imageStream).size(maxSize.getWidth(), maxSize.getHeight()).outputQuality(1.0).toOutputStream(targetStream);
        }
    }

    public void generateThumbnail(byte[] imageContent, ImageSize maxSize, File targetFile) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(imageContent)) {
            generateThumbnail(stream, maxSize, targetFile);
        }
    }

    public void generateThumbnail(File imageFile, ImageSize maxSize, File targetFile) throws IOException {
        try (FileInputStream stream = new FileInputStream(imageFile)) {
            generateThumbnail(stream, maxSize, targetFile);
        }
    }
}
