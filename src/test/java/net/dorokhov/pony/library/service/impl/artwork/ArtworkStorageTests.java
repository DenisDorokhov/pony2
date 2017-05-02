package net.dorokhov.pony.library.service.impl.artwork;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.repository.ArtworkRepository;
import net.dorokhov.pony.library.service.impl.artwork.command.ByteSourceArtworkCommand;
import net.dorokhov.pony.library.service.impl.artwork.command.FileArtworkCommand;
import net.dorokhov.pony.library.service.impl.artwork.command.ImageNodeArtworkCommand;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.impl.image.ThumbnailGenerator;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtworkStorageTests {
    
    private static final String PATH_LARGE = "foo.png";
    private static final String PATH_SMALL = "bar.png";

    private static final Resource RESOURCE = new ClassPathResource("image.png");
    private static final String CHECKSUM = "fc3adeae14ecc5f77d6dde58d40b1559";
    private static final FileType FILE_TYPE = FileType.of("image/png", "png");
    
    private static final ImageSize LARGE_IMAGE_SIZE = ImageSize.of(50, 50);
    private static final ImageSize SMALL_IMAGE_SIZE = ImageSize.of(20, 20);
    
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private FileTypeResolver fileTypeResolver;
    @Mock
    private ChecksumCalculator checksumCalculator;
    @Mock
    private ThumbnailGenerator thumbnailGenerator;

    private ArtworkStorage artworkStorage;
    
    private File artworkFolder;

    @Before
    public void setUp() throws Exception {
        
        artworkFolder = Files.createTempDir();
        artworkStorage = new ArtworkStorage(artworkRepository, 
                fileTypeResolver, checksumCalculator, 
                thumbnailGenerator, 
                artworkFolder, 
                new int[]{20, 20}, new int[]{50, 50});
        
        given(fileTypeResolver.resolve((File) any())).willReturn(FILE_TYPE);
        given(fileTypeResolver.resolve((byte[]) any())).willReturn(FILE_TYPE);
        given(checksumCalculator.calculate((File) any())).willReturn(CHECKSUM);
        given(checksumCalculator.calculate((byte[]) any())).willReturn(CHECKSUM);
        
        given(artworkRepository.save((Artwork) any())).willAnswer(invocation -> {
            Artwork artwork =  (Artwork) invocation.getArguments()[0];
            Field idField = artwork.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            ReflectionUtils.setField(idField, artwork, 1L);
            return artwork;
        });
        
        TransactionSynchronizationManager.initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        TransactionSynchronizationManager.clearSynchronization();
        if (artworkFolder != null) {
            if (!FileSystemUtils.deleteRecursively(artworkFolder)) {
                throw new RuntimeException("Could not delete artwork folder.");
            }
            artworkFolder = null;
        }
    }

    @Test
    public void getLargeImageFile() throws Exception {
        Artwork artwork = buildArtwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkStorage.getLargeImageFile(3L)).satisfies(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder, PATH_LARGE).getAbsolutePath()));
    }

    @Test
    public void getSmallImageFile() throws Exception {
        Artwork artwork = buildArtwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkStorage.getSmallImageFile(4L)).satisfies(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder, PATH_SMALL).getAbsolutePath()));
    }

    @Test
    public void getOrSaveByteSourceArtwork() throws Exception {
        byte[] bytes = Files.toByteArray(RESOURCE.getFile());
        ByteSourceArtworkCommand command = new ByteSourceArtworkCommand(buildUri(), ByteSource.wrap(bytes));
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void getOrSaveFileArtwork() throws Exception {
        File file = RESOURCE.getFile();
        FileArtworkCommand command = new FileArtworkCommand(buildUri(), file);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void getOrSaveImageNodeArtwork() throws Exception {
        ImageNode imageNode = mock(ImageNode.class);
        given(imageNode.getFile()).willReturn(RESOURCE.getFile());
        given(imageNode.getFileType()).willReturn(FileType.of("image/png", "png"));
        given(imageNode.getChecksum()).willReturn(CHECKSUM);
        ImageNodeArtworkCommand command = new ImageNodeArtworkCommand(buildUri(), imageNode);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void deleteCreatedFilesOnRollback() throws Exception {

        File file = RESOURCE.getFile();

        FileArtworkCommand command = new FileArtworkCommand(buildUri(), file);

        Artwork artwork = artworkStorage.getOrSave(command);
        
        File largeFile = new File(artworkFolder, artwork.getLargeImagePath());
        File smallFile = new File(artworkFolder, artwork.getSmallImagePath());
        Files.createParentDirs(largeFile);
        Files.createParentDirs(smallFile);
        Files.touch(largeFile);
        Files.touch(smallFile);
        
        TransactionSynchronizationManager.getSynchronizations().forEach(transactionSynchronization -> transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        
        assertThat(largeFile).doesNotExist();
        assertThat(smallFile).doesNotExist();
    }

    @Test
    public void delete() throws Exception {
        
        File largeImageFile = new File(artworkFolder, PATH_LARGE);
        File smallImageFile = new File(artworkFolder, PATH_SMALL);
        
        Files.touch(largeImageFile);
        Files.touch(smallImageFile);

        Artwork artwork = buildArtwork();

        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkStorage.delete(5L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(artworkRepository).delete(artwork);
        
        assertThat(largeImageFile).doesNotExist();
        assertThat(smallImageFile).doesNotExist();
        assertThat(artworkFolder).exists();
    }

    @Test
    public void whenDeletingIgnoreNotExistingFiles() throws Exception {
        Artwork artwork = buildArtwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkStorage.delete(5L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private void checkGetAndSaveArtwork(Supplier<Artwork> doGetAndSave) throws Exception {

        ArgumentCaptor<Artwork> savedArtwork = ArgumentCaptor.forClass(Artwork.class);

        Artwork artwork = doGetAndSave.get();
        
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(artworkRepository).save(savedArtwork.capture());
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), eq(SMALL_IMAGE_SIZE), eq(new File(artworkFolder, artwork.getSmallImagePath())));
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), eq(LARGE_IMAGE_SIZE), eq(new File(artworkFolder, artwork.getLargeImagePath())));

        assertThat(savedArtwork.getValue()).isSameAs(artwork);
        checkSavedArtwork(savedArtwork.getValue());

        given(artworkRepository.findByChecksum(any())).willReturn(artwork);
        doGetAndSave.get();
        verify(artworkRepository, times(1)).save(savedArtwork.capture());
    }
    
    private Artwork buildArtwork() {
        return Artwork.builder()
                .mimeType("image/png")
                .checksum("someChecksum")
                .largeImageSize(0L)
                .largeImagePath(PATH_LARGE)
                .smallImageSize(0L)
                .smallImagePath(PATH_SMALL)
                .sourceUri(buildUri())
                .build();
    }
    
    private void checkSavedArtwork(Artwork savedArtwork) {
        assertThat(savedArtwork.getMimeType()).isEqualTo(FILE_TYPE.getMimeType());
        assertThat(savedArtwork.getChecksum()).isEqualTo(CHECKSUM);
        assertThat(savedArtwork.getLargeImagePath()).endsWith(".large.png");
        assertThat(savedArtwork.getSmallImagePath()).endsWith(".small.png");
        assertThat(savedArtwork.getLargeImageSize()).isEqualTo(0L);
        assertThat(savedArtwork.getSmallImageSize()).isEqualTo(0L);
        assertThat(savedArtwork.getSourceUri()).isEqualTo(buildUri());
    }
    
    private URI buildUri() {
        return UriComponentsBuilder
                .fromUriString("sourceUri")
                .build().toUri();
    }
}
