package net.dorokhov.pony.library.service.file;

import com.google.common.io.ByteStreams;
import net.dorokhov.pony.api.library.domain.FileType;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeResolverTest {
    
    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");
    
    private FileTypeResolver fileTypeResolver = new FileTypeResolver();

    @Test
    public void shouldResolveImageInputStream() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            checkImageFileType(fileTypeResolver.resolve(stream));
        }
    }

    @Test
    public void shouldResolveImageFile() throws Exception {
        File file = IMAGE_RESOURCE.getFile();
        checkImageFileType(fileTypeResolver.resolve(file));
    }

    @Test
    public void shouldResolveImageContent() throws Exception {
        try (InputStream stream = IMAGE_RESOURCE.getInputStream()) {
            checkImageFileType(fileTypeResolver.resolve(ByteStreams.toByteArray(stream)));
        }
    }
    
    @Test
    public void shouldResolveMp3() throws Exception {

        File file = new ClassPathResource("audio/empty.mp3").getFile();
        FileType fileType = fileTypeResolver.resolve(file);

        assertThat(fileType.getMimeType()).isEqualTo("audio/mpeg");
        assertThat(fileType.getFileExtension()).isEqualTo("mp3");
    }

    @Test
    public void shouldImplementToString() throws Exception {
        assertThat(FileType.of("text/plain", "txt").toString()).startsWith("FileType{");
    }

    private void checkImageFileType(FileType fileType) {
        assertThat(fileType.getMimeType()).isEqualTo("image/png");
        assertThat(fileType.getFileExtension()).isEqualTo("png");
    }
}
