package net.dorokhov.pony.file;

import com.google.common.collect.ImmutableMap;
import net.dorokhov.pony.file.domain.FileType;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.Optional;

@Component
public class FileTypeResolver {
    
    private static final Map<String, String> EXTENSION_TO_CORRECTION = ImmutableMap.of(".mpga", ".mp3");
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public FileType resolve(InputStream stream) throws IOException {
        try (InputStream bufferedStream = new BufferedInputStream(stream)) {

            TikaConfig config = TikaConfig.getDefaultConfig();
            MediaType mediaType = config.getMimeRepository().detect(bufferedStream, new Metadata());

            try {
                MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
                String extension = correctExtension(mimeType.getExtension());
                return FileType.of(mimeType.toString(), extension);
            } catch (MimeTypeException e) {
                log.debug("Could not resolve mime type.", e);
                return FileType.of("application/octet-stream", "bin");
            }
        }
    }
    
    public FileType resolve(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return resolve(stream);
        }
    }
    
    public FileType resolve(byte[] content) {
        try (InputStream stream = new ByteArrayInputStream(content)) {
            return resolve(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private String correctExtension(String extension) {
        String correctedExtension = Optional.ofNullable(EXTENSION_TO_CORRECTION.get(extension)).orElse(extension);
        if (correctedExtension.length() >= 1) {
            return correctedExtension.substring(1);
        } else {
            return correctedExtension;
        }
    }
}
