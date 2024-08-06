package net.dorokhov.pony2.core.library.service.filetree.domain;

import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CachingImageNodeTest {

    @Mock
    private FileTypeResolver fileTypeResolver;

    @Mock
    private ChecksumCalculator checksumCalculator;
    
    @Mock
    private ImageSizeReader imageSizeReader;

    @Test
    public void shouldCacheFileType() throws IOException {

        FileType fileType = FileType.of("text/plan", "txt");
        when(fileTypeResolver.resolve((File) any())).thenReturn(fileType);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        FileType result = imageNode.getFileType();

        assertThat(result).isSameAs(fileType);
        verify(fileTypeResolver, times(1)).resolve((File) any());

        assertThat(imageNode.getFileType()).isSameAs(result);
        verify(fileTypeResolver, times(1)).resolve((File) any());
    }

    @Test
    public void shouldCacheChecksum() throws IOException {

        String checksum = "someChecksum";
        when(checksumCalculator.calculate((File) any())).thenReturn(checksum);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        String result = imageNode.getChecksum();

        assertThat(result).isSameAs(checksum);
        verify(checksumCalculator, times(1)).calculate((File) any());

        assertThat(imageNode.getChecksum()).isSameAs(result);
        verify(checksumCalculator, times(1)).calculate((File) any());
    }

    @Test
    public void shouldCacheImageSize() throws IOException {

        ImageSize imageSize = ImageSize.of(100, 100);
        when(imageSizeReader.read((File) any())).thenReturn(imageSize);
        CachingImageNode imageNode = new CachingImageNode(mock(File.class), null,
                fileTypeResolver, checksumCalculator, imageSizeReader);

        ImageSize result = imageNode.getImageSize();

        assertThat(result).isSameAs(imageSize);
        verify(imageSizeReader, times(1)).read((File) any());

        assertThat(imageNode.getImageSize()).isSameAs(result);
        verify(imageSizeReader, times(1)).read((File) any());
    }
}