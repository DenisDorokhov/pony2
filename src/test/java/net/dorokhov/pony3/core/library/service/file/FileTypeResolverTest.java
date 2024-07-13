package net.dorokhov.pony3.core.library.service.file;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import net.dorokhov.pony3.api.library.domain.FileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeResolverTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    private static final Resource AUDIO_RESOURCE = new ClassPathResource("audio/empty.mp3");
    
    private final FileTypeResolver fileTypeResolver = new FileTypeResolver();

    @TempDir
    public Path tempFolder;

    @Test
    public void shouldResolveImageFile() throws IOException {
        checkImageFileType(fileTypeResolver.resolve(IMAGE_RESOURCE.getFile()));
    }

    @Test
    public void shouldResolveImageContent() throws IOException {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            checkImageFileType(fileTypeResolver.resolve(ByteStreams.toByteArray(stream)));
        }
    }
    
    @Test
    public void shouldResolveMp3File() throws IOException {

        File file = AUDIO_RESOURCE.getFile();

        FileType fileType = fileTypeResolver.resolve(file);

        assertThat(fileType.getMimeType()).isEqualTo("audio/mpeg");
        assertThat(fileType.getFileExtension()).isEqualTo("mp3");
    }
    
    @Test
    public void shouldResolveMp3FileByUpperCaseExtension() throws IOException {

        File file = tempFolder.resolve("empty.MP3").toFile();
        Files.copy(AUDIO_RESOURCE.getFile(), file);

        FileType fileType = fileTypeResolver.resolve(file);

        assertThat(fileType.getMimeType()).isEqualTo("audio/mpeg");
        assertThat(fileType.getFileExtension()).isEqualTo("mp3");
    }

    @Test
    public void shouldResolveUnknownFile() throws IOException {

        File file = new ClassPathResource("test.txt").getFile();

        FileType fileType = fileTypeResolver.resolve(file);

        assertThat(fileType.getMimeType()).isEqualTo("application/octet-stream");
        assertThat(fileType.getFileExtension()).isEqualTo("txt");
    }

    @Test
    public void shouldImplementToString() {
        
        FileType fileType = FileType.of("text/plain", "txt");
        
        assertThat(fileType.toString()).startsWith("FileType{");
    }

    private void checkImageFileType(FileType fileType) {
        assertThat(fileType.getMimeType()).isEqualTo("image/png");
        assertThat(fileType.getFileExtension()).isEqualTo("png");
    }
}
