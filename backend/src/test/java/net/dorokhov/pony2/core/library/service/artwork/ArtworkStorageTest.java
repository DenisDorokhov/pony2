package net.dorokhov.pony2.core.library.service.artwork;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.core.library.repository.ArtworkRepository;
import net.dorokhov.pony2.core.library.service.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony2.core.library.service.image.ThumbnailGenerator;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

@ExtendWith(MockitoExtension.class)
public class ArtworkStorageTest {
    
    private static final String PATH_LARGE = "foo.png";
    private static final String PATH_SMALL = "bar.png";

    private static final Resource RESOURCE = new ClassPathResource("image.png");
    private static final String CHECKSUM = "fc3adeae14ecc5f77d6dde58d40b1559";
    private static final FileType FILE_TYPE = FileType.of("image/png", "png");
    
    private static final ImageSize LARGE_IMAGE_SIZE = ImageSize.of(50, 50);
    private static final ImageSize SMALL_IMAGE_SIZE = ImageSize.of(20, 20);

    @TempDir
    public Path tempFolder;
    
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private FileTypeResolver fileTypeResolver;
    @Mock
    private ChecksumCalculator checksumCalculator;
    @Mock
    private ThumbnailGenerator thumbnailGenerator;
    
    private File artworkFolder;

    private ArtworkStorage artworkStorage;

    @BeforeEach
    public void setUp() {
        artworkFolder = tempFolder.resolve("some/nested/path").toFile();
        artworkStorage = new ArtworkStorage(artworkRepository, 
                fileTypeResolver, checksumCalculator, thumbnailGenerator, artworkFolder, 
                new int[]{SMALL_IMAGE_SIZE.getWidth(), SMALL_IMAGE_SIZE.getHeight()}, 
                new int[]{LARGE_IMAGE_SIZE.getWidth(), LARGE_IMAGE_SIZE.getHeight()});
        initSynchronization();
    }

    @AfterEach
    public void tearDown() {
        clearSynchronization();
    }

    @Test
    public void shouldGetArtworkFile() {

        Artwork artwork = artwork();
        when(artworkRepository.findById(any())).thenReturn(Optional.of(artwork));

        assertThat(artworkStorage.getArtworkFile("1")).hasValueSatisfying(artworkFiles -> {
            assertThat(artworkFiles.getLargeFile()).isEqualTo(new File(artworkFolder, PATH_LARGE));
            assertThat(artworkFiles.getSmallFile()).isEqualTo(new File(artworkFolder, PATH_SMALL));
        });
    }

    @Test
    public void shouldGetOrSaveByteSourceArtwork() throws IOException {

        when(checksumCalculator.calculate((byte[]) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((byte[]) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save(any())).then(returnsFirstArg());
        
        byte[] bytes = Files.toByteArray(RESOURCE.getFile());
        ByteSourceArtworkStorageCommand command = new ByteSourceArtworkStorageCommand(sourceUri(), ByteSource.wrap(bytes));

        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveFileArtwork() throws IOException {

        when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((File) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save(any())).then(returnsFirstArg());
        
        File file = RESOURCE.getFile();
        FileArtworkStorageCommand command = new FileArtworkStorageCommand(sourceUri(), file);

        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveImageNodeArtwork() throws IOException {

        when(artworkRepository.save(any())).then(returnsFirstArg());
        
        ImageNode imageNode = mock(ImageNode.class);
        when(imageNode.getFile()).thenReturn(RESOURCE.getFile());
        when(imageNode.getFileType()).thenReturn(FileType.of("image/png", "png"));
        when(imageNode.getChecksum()).thenReturn(CHECKSUM);
        ImageNodeArtworkStorageCommand command = new ImageNodeArtworkStorageCommand(sourceUri(), imageNode);

        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldDeleteCreatedFilesOnRollback() throws IOException {

        when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((File) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save(any())).then(returnsFirstArg());

        FileArtworkStorageCommand command = new FileArtworkStorageCommand(sourceUri(), RESOURCE.getFile());

        ArtworkFiles artworkFiles = artworkStorage.getOrSave(command);
        
        Files.createParentDirs(artworkFiles.getLargeFile());
        Files.createParentDirs(artworkFiles.getSmallFile());
        Files.touch(artworkFiles.getLargeFile());
        Files.touch(artworkFiles.getSmallFile());
        
        getSynchronizations().forEach(transactionSynchronization -> 
                transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        
        assertThat(artworkFiles.getLargeFile()).doesNotExist();
        assertThat(artworkFiles.getSmallFile()).doesNotExist();
    }

    @Test
    public void shouldDelete() throws IOException {
        
        File largeImageFile = new File(artworkFolder, PATH_LARGE);
        File smallImageFile = new File(artworkFolder, PATH_SMALL);
        Files.createParentDirs(largeImageFile);
        Files.createParentDirs(smallImageFile);
        Files.touch(largeImageFile);
        Files.touch(smallImageFile);

        Artwork artwork = artwork();
        when(artworkRepository.findById(any())).thenReturn(Optional.of(artwork));

        artworkStorage.delete("5");

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(artworkRepository).delete(artwork);
        
        assertThat(largeImageFile).doesNotExist();
        assertThat(smallImageFile).doesNotExist();
    }

    @Test
    public void shouldIgnoreNotExistingFilesWhenDeleting() {

        Artwork artwork = artwork();
        when(artworkRepository.findById(any())).thenReturn(Optional.of(artwork));

        artworkStorage.delete("5");

        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private void checkGetAndSaveArtwork(Supplier<ArtworkFiles> doGetAndSave) throws IOException {

        ArgumentCaptor<Artwork> savedArtwork = ArgumentCaptor.forClass(Artwork.class);

        ArtworkFiles artworkFiles = doGetAndSave.get();
        
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(artworkRepository).save(savedArtwork.capture());
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), 
                eq(SMALL_IMAGE_SIZE), any(), eq(artworkFiles.getSmallFile()));
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), 
                eq(LARGE_IMAGE_SIZE), any(), eq(artworkFiles.getLargeFile()));

        assertThat(savedArtwork.getValue()).isSameAs(artworkFiles.getArtwork());
        checkSavedArtwork(savedArtwork.getValue());

        when(artworkRepository.findByChecksumAndSourceUriScheme(any(), any())).thenReturn(artworkFiles.getArtwork());

        doGetAndSave.get();

        verify(artworkRepository, times(1)).save(savedArtwork.capture());
    }
    
    private void checkSavedArtwork(Artwork savedArtwork) {
        assertThat(savedArtwork.getMimeType()).isEqualTo(FILE_TYPE.getMimeType());
        assertThat(savedArtwork.getChecksum()).isEqualTo(CHECKSUM);
        assertThat(savedArtwork.getLargeImagePath()).endsWith(".large.png");
        assertThat(savedArtwork.getSmallImagePath()).endsWith(".small.png");
        assertThat(savedArtwork.getLargeImageSize()).isEqualTo(0L);
        assertThat(savedArtwork.getSmallImageSize()).isEqualTo(0L);
        assertThat(savedArtwork.getSourceUri()).isEqualTo(sourceUri());
    }
    
    private Artwork artwork() {
        return new Artwork()
                .setMimeType("image/png")
                .setChecksum("someChecksum")
                .setLargeImageSize(0L)
                .setLargeImagePath(PATH_LARGE)
                .setSmallImageSize(0L)
                .setSmallImagePath(PATH_SMALL)
                .setSourceUri(sourceUri());
    }
    
    private URI sourceUri() {
        return UriComponentsBuilder
                .fromUriString("file:sourceUri")
                .build().toUri();
    }
}
