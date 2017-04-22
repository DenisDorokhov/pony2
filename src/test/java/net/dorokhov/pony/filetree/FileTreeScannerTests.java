package net.dorokhov.pony.filetree;

import net.dorokhov.pony.audio.AudioTagger;
import net.dorokhov.pony.audio.domain.ReadableAudioData;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.domain.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import net.dorokhov.pony.filetree.domain.*;
import net.dorokhov.pony.image.domain.ImageSize;
import net.dorokhov.pony.image.ImageSizeReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static net.dorokhov.pony.util.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FileTreeScannerTests {
    
    private static final Resource FOLDER = new ClassPathResource("filetree");
    
    private static final Resource FILE_IMAGE = new ClassPathResource("filetree/artist1/album1-1/cover1-1-1.png");
    private static final Resource FILE_AUDIO = new ClassPathResource("filetree/artist1/album1-1/song1-1-1.mp3");
    
    private static final ImageSize IMAGE_SIZE = new ImageSize(100, 100);
    private static final String CHECKSUM = "someChecksum";
    private static final ReadableAudioData AUDIO_DATA = ReadableAudioData.builder()
            .path("somePath")
            .fileType(new FileType("text/plain", "txt"))
            .build();

    @InjectMocks
    private FileTreeScanner fileTreeScanner;
    
    @Mock
    private FileTypeResolver fileTypeResolver;
    @Mock
    private ImageSizeReader imageSizeReader;
    @Mock
    private ChecksumCalculator checksumCalculator;
    @Mock
    private AudioTagger audioTagger;
    
    @Mock
    private FileType fileTypeImage;
    @Mock
    private FileType fileTypeAudio;
    @Mock
    private FileType fileTypeOther;

    @Before
    public void setUp() throws Exception {
        
        given(fileTypeImage.isImage()).willReturn(true);
        given(fileTypeAudio.isAudio()).willReturn(true);
        
        given(fileTypeResolver.resolve((File) any())).willAnswer(invocation -> {
            File file = (File) invocation.getArguments()[0];
            if (file.getPath().endsWith(".mp3")) {
                return fileTypeAudio;
            } else if (file.getPath().endsWith(".png")) {
                return fileTypeImage;
            }
            return fileTypeOther;
        });
        
        given(imageSizeReader.read((File) any())).willReturn(IMAGE_SIZE);
        given(checksumCalculator.calculate((File) any())).willReturn(CHECKSUM);
        given(audioTagger.read(any())).willReturn(AUDIO_DATA);
    }

    @Test
    public void scanFolder() throws Exception {
        checkRoot(fileTreeScanner.scanFolder(FOLDER.getFile()));
    }

    @Test
    public void scanImageFile() throws Exception {
        Optional<FileNode> file = fileTreeScanner.scanFile(FILE_IMAGE.getFile());
        assertThat(file).hasValueSatisfying(rethrow(image -> {
            assertThat(image.getFile().getAbsolutePath()).isEqualTo(FILE_IMAGE.getFile().getAbsolutePath());
            assertThat(image.getParentFolder()).isEmpty();
            assertThat(image.getFileType()).isSameAs(fileTypeImage);
            assertThat(image.getChecksum()).isSameAs(CHECKSUM);
            assertThat(image).isInstanceOf(ImageNode.class);
            assertThat(((ImageNode) image).getImageSize()).isSameAs(IMAGE_SIZE);
        }));
    }

    @Test
    public void scanAudioFile() throws Exception {
        Optional<FileNode> file = fileTreeScanner.scanFile(FILE_AUDIO.getFile());
        assertThat(file).hasValueSatisfying(rethrow(audio -> {
            assertThat(audio.getFile().getAbsolutePath()).isEqualTo(FILE_AUDIO.getFile().getAbsolutePath());
            assertThat(audio.getParentFolder()).isEmpty();
            assertThat(audio.getFileType()).isSameAs(fileTypeAudio);
            assertThat(audio.getChecksum()).isSameAs(CHECKSUM);
            assertThat(audio).isInstanceOf(AudioNode.class);
        }));
    }

    @Test
    public void cacheFileType() throws Exception {
        Optional<FileNode> file = fileTreeScanner.scanFile(FILE_IMAGE.getFile());
        assertThat(file).hasValueSatisfying(rethrow(image -> {
            assertThat(image.getFileType()).isSameAs(fileTypeImage);
            verify(fileTypeResolver, times(2)).resolve((File) any());
            assertThat(image.getFileType()).isSameAs(fileTypeImage);
            verify(fileTypeResolver, times(2)).resolve((File) any());
        }));
    }

    @Test
    public void cacheChecksum() throws Exception {
        Optional<FileNode> file = fileTreeScanner.scanFile(FILE_IMAGE.getFile());
        assertThat(file).hasValueSatisfying(rethrow(image -> {
            assertThat(image.getChecksum()).isSameAs(CHECKSUM);
            verify(checksumCalculator, times(1)).calculate((File) any());
            assertThat(image.getChecksum()).isSameAs(CHECKSUM);
            verify(checksumCalculator, times(1)).calculate((File) any());
        }));
    }

    @Test
    public void cacheAudioData() throws Exception {
        Optional<FileNode> file = fileTreeScanner.scanFile(FILE_AUDIO.getFile());
        assertThat(file).hasValueSatisfying(rethrow(audio -> {
            assertThat(((AudioNode) audio).getAudioData()).isSameAs(AUDIO_DATA);
            verify(audioTagger, times(1)).read(any());
            assertThat(((AudioNode) audio).getAudioData()).isSameAs(AUDIO_DATA);
            verify(audioTagger, times(1)).read(any());
        }));
    }

    private void checkRoot(FolderNode root) throws IOException {

        assertThat(root.getFile().getAbsolutePath()).isEqualTo(FOLDER.getFile().getAbsolutePath());
        assertThat(root.getParentFolder()).isEmpty();

        assertThat(nodesToNames(root.getChildFolders(false))).containsOnly("artist1", "artist2");

        assertThat(nodesToNames(root.getChildImages(true))).containsOnly("cover1-1-1.png", "cover1-2-1.png");
        assertThat(nodesToNames(root.getChildAudios(true))).containsOnly("song1-1-1.mp3", "song1-1-2.mp3", "song1-2-1.mp3", "song2-1.mp3");
        assertThat(nodesToNames(root.getChildFolders(true))).containsOnly("artist1", "artist2", "album1-1", "album1-2");
        
        root.getChildFolders(false).forEach(f -> {
            switch (f.getFile().getName()) {
                case "artist1":
                    checkArtist1(f, root);
                    break;
                case "artist2":
                    checkArtist2(f, root);
                    break;
            }
        });
    }
    
    private void checkArtist1(FolderNode folder, FolderNode root) {
        
        assertThat(folder.getParentFolder()).hasValue(root);
        
        assertThat(nodesToNames(folder.getChildFolders(false))).containsOnly("album1-1", "album1-2");
        assertThat(nodesToNames(folder.getChildImages(false))).isEmpty();
        assertThat(nodesToNames(folder.getChildAudios(false))).isEmpty();

        folder.getChildFolders(false).forEach(f -> {
            switch (f.getFile().getName()) {
                case "album1-1":
                    checkAlbum1_1(f);
                    break;
                case "album1-2":
                    checkAlbum1_2(f);
                    break;
            }
        });
    }

    private void checkAlbum1_1(FolderNode folder) {
        assertThat(nodesToNames(folder.getChildFolders(false))).isEmpty();
        assertThat(nodesToNames(folder.getChildImages(false))).containsOnly("cover1-1-1.png");
        assertThat(nodesToNames(folder.getChildAudios(false))).containsOnly("song1-1-1.mp3", "song1-1-2.mp3");
    }

    private void checkAlbum1_2(FolderNode folder) {
        assertThat(nodesToNames(folder.getChildFolders(false))).isEmpty();
        assertThat(nodesToNames(folder.getChildImages(false))).containsOnly("cover1-2-1.png");
        assertThat(nodesToNames(folder.getChildAudios(false))).containsOnly("song1-2-1.mp3");
    }

    private void checkArtist2(FolderNode folder, FolderNode root) {

        assertThat(folder.getParentFolder()).hasValue(root);
        
        assertThat(nodesToNames(folder.getChildFolders(false))).isEmpty();
        assertThat(nodesToNames(folder.getChildImages(false))).isEmpty();
        assertThat(nodesToNames(folder.getChildAudios(false))).containsOnly("song2-1.mp3");
    }
    
    private <T extends Node> Set<String> nodesToNames(List<T> nodes) {
        return nodes.stream().map(n -> n.getFile().getName()).collect(Collectors.toSet());
    }
}
