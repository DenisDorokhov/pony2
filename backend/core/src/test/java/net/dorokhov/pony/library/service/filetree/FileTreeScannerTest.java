package net.dorokhov.pony.library.service.filetree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import net.dorokhov.pony.api.library.domain.FileType;
import net.dorokhov.pony.api.library.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.AudioTagger;
import net.dorokhov.pony.library.service.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.file.FileTypeResolver;
import net.dorokhov.pony.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.filetree.domain.FileNode;
import net.dorokhov.pony.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.filetree.domain.Node;
import net.dorokhov.pony.library.service.image.ImageSizeReader;
import net.dorokhov.pony.library.service.image.domain.ImageSize;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileTreeScannerTest {

    private static final Resource FOLDER = new ClassPathResource("filetree");

    private static final Resource FILE_IMAGE = new ClassPathResource("filetree/artist1/album1-1/cover1-1-1.png");
    private static final Resource FILE_AUDIO = new ClassPathResource("filetree/artist1/album1-1/song1-1-1.mp3");

    private static final ImageSize IMAGE_SIZE = ImageSize.of(100, 100);
    private static final String CHECKSUM = "someChecksum";
    private static final ReadableAudioData AUDIO_DATA = ReadableAudioData.builder()
            .path("somePath")
            .fileType(FileType.of("text/plain", "txt"))
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

    private final FileType fileTypeAudio = FileType.of("audio/mpeg", "mp3");
    private final FileType fileTypeImage = FileType.of("image/png", "png");
    private final FileType fileTypeOther = FileType.of("text/plain", "txt");

    @Before
    public void setUp() throws Exception {

        when(fileTypeResolver.resolve((File) any())).then(invocation -> {
            File file = (File) invocation.getArguments()[0];
            if (file.getPath().endsWith(".mp3")) {
                return fileTypeAudio;
            } else if (file.getPath().endsWith(".png")) {
                return fileTypeImage;
            }
            return fileTypeOther;
        });

        when(imageSizeReader.read((File) any())).thenReturn(IMAGE_SIZE);
        when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        when(audioTagger.read(any())).thenReturn(AUDIO_DATA);
    }

    @Test
    public void shouldScanFolder() throws Exception {
        checkRoot(fileTreeScanner.scanFolder(FOLDER.getFile()));
    }

    @Test
    public void shouldScanImageFile() throws Exception {
        FileNode fileNode = fileTreeScanner.scanFile(FILE_IMAGE.getFile(), ImmutableList.of(FOLDER.getFile()));
        assertThat(fileNode).isInstanceOfSatisfying(ImageNode.class, rethrow(image -> {
            assertThat(image.getFile()).isEqualTo(FILE_IMAGE.getFile());
            assertThat(image.getParentFolder()).isNotNull();
            assertThat(image.getParentFolder().getFile()).isEqualTo(FILE_IMAGE.getFile().getParentFile());
            assertThat(image.getFileType()).isSameAs(fileTypeImage);
            assertThat(image.getChecksum()).isSameAs(CHECKSUM);
            assertThat(image.getImageSize()).isSameAs(IMAGE_SIZE);
        }));
    }

    @Test
    public void shouldScanAudioFile() throws Exception {
        FileNode fileNode = fileTreeScanner.scanFile(FILE_AUDIO.getFile(), ImmutableList.of(FOLDER.getFile()));
        assertThat(fileNode).isInstanceOfSatisfying(AudioNode.class, rethrow(audio -> {
            assertThat(audio.getFile()).isEqualTo(FILE_AUDIO.getFile());
            assertThat(audio.getParentFolder()).isNotNull();
            assertThat(audio.getParentFolder().getFile()).isEqualTo(FILE_AUDIO.getFile().getParentFile());
            assertThat(audio.getFileType()).isSameAs(fileTypeAudio);
            assertThat(audio.getChecksum()).isSameAs(CHECKSUM);
            assertThat(audio.getAudioData()).isSameAs(AUDIO_DATA);
        }));
    }

    @Test
    public void shouldFailIfFileIsNotFoundInRootFolders() throws Exception {
        assertThatThrownBy(() -> fileTreeScanner.scanFile(FILE_AUDIO.getFile(), ImmutableList.of(Files.createTempDir())))
                .isInstanceOf(FileNotFoundException.class);
    }

    private void checkRoot(FolderNode root) throws IOException {

        assertThat(root.getFile()).isEqualTo(FOLDER.getFile());
        assertThat(root.getParentFolder()).isNull();

        assertThat(nodesToNames(root.getChildFolders())).containsOnly("artist1", "artist2");

        assertThat(nodesToNames(root.getChildImagesRecursively())).containsOnly("cover1-1-1.png", "cover1-2-1.png");
        assertThat(nodesToNames(root.getChildAudiosRecursively())).containsOnly("song1-1-1.mp3", "song1-1-2.mp3", "song1-2-1.mp3", "song2-1.mp3");

        root.getChildFolders().forEach(f -> {
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

        assertThat(folder.getParentFolder()).isSameAs(root);

        assertThat(nodesToNames(folder.getChildFolders())).containsOnly("album1-1", "album1-2");
        assertThat(nodesToNames(folder.getChildImages())).isEmpty();
        assertThat(nodesToNames(folder.getChildAudios())).isEmpty();

        folder.getChildFolders().forEach(f -> {
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
        assertThat(nodesToNames(folder.getChildFolders())).isEmpty();
        assertThat(nodesToNames(folder.getChildImages())).containsOnly("cover1-1-1.png");
        assertThat(nodesToNames(folder.getChildAudios())).containsOnly("song1-1-1.mp3", "song1-1-2.mp3");
    }

    private void checkAlbum1_2(FolderNode folder) {
        assertThat(nodesToNames(folder.getChildFolders())).isEmpty();
        assertThat(nodesToNames(folder.getChildImages())).containsOnly("cover1-2-1.png");
        assertThat(nodesToNames(folder.getChildAudios())).containsOnly("song1-2-1.mp3");
    }

    private void checkArtist2(FolderNode folder, FolderNode root) {

        assertThat(folder.getParentFolder()).isSameAs(root);

        assertThat(nodesToNames(folder.getChildFolders())).isEmpty();
        assertThat(nodesToNames(folder.getChildImages())).isEmpty();
        assertThat(nodesToNames(folder.getChildAudios())).containsOnly("song2-1.mp3");
    }

    private <T extends Node> Set<String> nodesToNames(List<T> nodes) {
        return nodes.stream().map(n -> n.getFile().getName()).collect(Collectors.toSet());
    }
}
