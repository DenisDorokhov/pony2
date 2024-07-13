package net.dorokhov.pony3.core.library.service.image;

import net.dorokhov.pony3.core.library.service.image.domain.ImageSize;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.Iterator;

@Component
public class ImageSizeReader {

    public ImageSize read(InputStream stream) throws IOException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageStream);
                    return ImageSize.of(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            } else {
                throw new IOException("No image readers registered for the supplied data.");
            }
        } catch (Exception e) {
            throw new IOException("Could not read image size.", e);
        }
    }

    public ImageSize read(byte[] content) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(content)) {
            return read(stream);
        }
    }

    public ImageSize read(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return read(stream);
        }
    }
}
