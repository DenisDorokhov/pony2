package net.dorokhov.pony3.core.library.service.file;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import net.dorokhov.pony3.api.library.domain.FileType;
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

@Component
public class FileTypeResolver {
    
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final Map<String, String> EXTENSION_TO_MIME_TYPE = ImmutableMap.<String, String>builder()
            .put("jpg", "image/jpeg")
		    .put("jpeg", "image/jpeg")
		    .put("png", "image/png")
		    .put("mp3", "audio/mpeg")
            .build();
    private static final Map<String, String> EXTENSION_TO_CORRECTION = ImmutableMap.of(".mpga", ".mp3");
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public FileType resolve(File file) {
        String extension = Files.getFileExtension(file.getName()).toLowerCase();
        String mimeType = EXTENSION_TO_MIME_TYPE.get(extension);
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        return new FileType(mimeType, extension);
    }
    
    public FileType resolve(byte[] content) {
        try (InputStream stream = new ByteArrayInputStream(content)) {
            return resolve(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private FileType resolve(InputStream stream) throws IOException {
        try (InputStream bufferedStream = new BufferedInputStream(stream)) {

            TikaConfig config = TikaConfig.getDefaultConfig();
            MediaType mediaType = config.getMimeRepository().detect(bufferedStream, new Metadata());

            try {
                MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
                String extension = correctExtension(mimeType.getExtension());
                return FileType.of(mimeType.toString(), extension);
            } catch (MimeTypeException e) {
                logger.debug("Could not resolve mime type.", e);
                return FileType.of(DEFAULT_MIME_TYPE, "bin");
            }
        }
    }
    
    private String correctExtension(String extension) {
        String correctedExtension = EXTENSION_TO_CORRECTION.get(extension);
        if (correctedExtension == null) {
            correctedExtension = extension;
        }
        if (!correctedExtension.isEmpty()) {
            return correctedExtension.substring(1);
        } else {
            return correctedExtension;
        }
    }
}
