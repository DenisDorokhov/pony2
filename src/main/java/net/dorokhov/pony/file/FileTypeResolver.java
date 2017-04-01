package net.dorokhov.pony.file;

import com.google.common.collect.ImmutableMap;
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
    
    public Optional<FileType> resolve(File file) throws IOException {

        TikaConfig config = TikaConfig.getDefaultConfig();
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        MediaType mediaType = config.getMimeRepository().detect(stream, new Metadata());
        
        try {
            MimeType mimeType = config.getMimeRepository().forName(mediaType.toString());
            String extension = correctExtension(mimeType.getExtension());
            return Optional.of(new FileType(mimeType.toString(), extension));
        } catch (MimeTypeException e) {
            log.debug("Could not resolve mime type for file '{}'.", file.getAbsolutePath(), e);
            return Optional.empty();
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

    public static class FileType {

        private final String mimeType;

        private final String fileExtension;

        public FileType(String mimeType, String fileExtension) {
            this.mimeType = mimeType;
            this.fileExtension = fileExtension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }
}
