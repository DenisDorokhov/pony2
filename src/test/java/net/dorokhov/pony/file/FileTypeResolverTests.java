package net.dorokhov.pony.file;

import net.dorokhov.pony.file.FileTypeResolver.FileType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FileTypeResolverTests {
    
    private FileTypeResolver fileTypeResolver;

    @Before
    public void setUp() {
        fileTypeResolver = new FileTypeResolver();
    }

    @Test
    public void resolveImageFileType() throws Exception {
        
        File file = new ClassPathResource("image.png").getFile();
        Optional<FileType> fileType = fileTypeResolver.resolve(file);
        
        assertThat(fileType.map(FileType::getMimeType)).hasValue("image/png");
        assertThat(fileType.map(FileType::getFileExtension)).hasValue("png");
    }
    
    @Test
    public void resolveMp3FileType() throws Exception {

        File file = new ClassPathResource("metallica-battery.mp3").getFile();
        Optional<FileType> fileType = fileTypeResolver.resolve(file);

        assertThat(fileType.map(FileType::getMimeType)).hasValue("audio/mpeg");
        assertThat(fileType.map(FileType::getFileExtension)).hasValue("mp3");
    }
}
