package net.dorokhov.pony2.core.library.service.filetree;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData;
import net.dorokhov.pony2.core.library.service.AudioTagger;
import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.filetree.domain.*;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class FileTreeScannerTest {

    private static final Resource FOLDER = new ClassPathResource("filetree");

    private static final Resource FILE_IMAGE = new ClassPathResource("filetree/artist1/album1-1/cover1-1-1.png");
    private static final Resource FILE_AUDIO = new ClassPathResource("filetree/artist1/album1-1/song1-1-1.mp3");

    private static final ImageSize IMAGE_SIZE = ImageSize.of(100, 100);
    private static final String CHECKSUM = "someChecksum";
    private static final ReadableAudioData AUDIO_DATA = new ReadableAudioData()
            .setPath("somePath")
            .setFileType(FileType.of("text/plain", "txt"));

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

    @TempDir
    public Path tempFolder;

    private final FileType fileTypeAudio = FileType.of("audio/mpeg", "mp3");
    private final FileType fileTypeImage = FileType.of("image/png", "png");
    private final FileType fileTypeOther = FileType.of("text/plain", "txt");

    @BeforeEach
    public void setUp() throws IOException {

        lenient().when(fileTypeResolver.resolve((File) any())).then(invocation -> {
            File file = (File) invocation.getArguments()[0];
            if (file.getPath().endsWith(".mp3")) {
                return fileTypeAudio;
            } else if (file.getPath().endsWith(".png")) {
                return fileTypeImage;
            }
            return fileTypeOther;
        });

        lenient().when(imageSizeReader.read((File) any())).thenReturn(IMAGE_SIZE);
        lenient().when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        lenient().when(audioTagger.read(any())).thenReturn(AUDIO_DATA);
    }

    @Test
    public void shouldScanFolder() throws IOException {
        checkRoot(fileTreeScanner.scanFolder(FOLDER.getFile()));
    }

    @Test
    public void shouldScanImageFile() throws IOException {

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
    public void shouldScanAudioFile() throws IOException {

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
    public void shouldFailIfFileIsNotFoundInRootFolders() {
        assertThatThrownBy(() -> fileTreeScanner.scanFile(FILE_AUDIO.getFile(), ImmutableList.of(tempFolder.toFile())))
                .isInstanceOf(FileNotFoundException.class);
    }

    private void checkRoot(FolderNode root) throws IOException {

        assertThat(root.getFile()).isEqualTo(FOLDER.getFile());
        assertThat(root.getParentFolder()).isNull();

        assertThat(nodesToNames(root.getChildFolders())).containsOnly("artist1", "artist2");

        assertThat(nodesToNames(root.getNotIgnoredChildImagesRecursively())).containsOnly("cover1-1-1.png", "cover1-2-1.png");
        assertThat(nodesToNames(root.getNotIgnoredChildAudiosRecursively())).containsOnly("song1-1-1.mp3", "song1-1-2.mp3", "song1-2-1.mp3", "song2-1.mp3");

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
        assertThat(nodesToNames(folder.getChildFolders())).containsOnly("ignored");
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
