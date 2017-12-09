package net.dorokhov.pony.library.service.artwork;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.repository.ArtworkRepository;
import net.dorokhov.pony.library.service.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony.library.service.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony.library.service.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.file.FileTypeResolver;
import net.dorokhov.pony.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.image.ThumbnailGenerator;
import net.dorokhov.pony.library.service.image.domain.ImageSize;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.web.util.UriComponentsBuilder;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization;
import static org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations;
import static org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization;

@RunWith(MockitoJUnitRunner.class)
public class ArtworkStorageTest {
    
    private static final String PATH_LARGE = "foo.png";
    private static final String PATH_SMALL = "bar.png";

    private static final Resource RESOURCE = new ClassPathResource("image.png");
    private static final String CHECKSUM = "fc3adeae14ecc5f77d6dde58d40b1559";
    private static final FileType FILE_TYPE = FileType.of("image/png", "png");
    
    private static final ImageSize LARGE_IMAGE_SIZE = ImageSize.of(50, 50);
    private static final ImageSize SMALL_IMAGE_SIZE = ImageSize.of(20, 20);

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
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

    @Before
    public void setUp() throws Exception {
        artworkFolder = new File(tempFolder.getRoot(), "some/nested/path");
        artworkStorage = new ArtworkStorage(artworkRepository, 
                fileTypeResolver, checksumCalculator, thumbnailGenerator, artworkFolder, 
                new int[]{SMALL_IMAGE_SIZE.getWidth(), SMALL_IMAGE_SIZE.getHeight()}, 
                new int[]{LARGE_IMAGE_SIZE.getWidth(), LARGE_IMAGE_SIZE.getHeight()});
        initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        clearSynchronization();
    }

    @Test
    public void shouldGetArtworkFile() throws Exception {
        Artwork artwork = artwork();
        when(artworkRepository.findOne((Long) any())).thenReturn(artwork);
        ArtworkFiles artworkFiles = artworkStorage.getArtworkFile(1L);
        assertThat(artworkFiles).isNotNull();
        assertThat(artworkFiles.getLargeFile()).isEqualTo(new File(artworkFolder, PATH_LARGE));
        assertThat(artworkFiles.getSmallFile()).isEqualTo(new File(artworkFolder, PATH_SMALL));
    }

    @Test
    public void shouldGetOrSaveByteSourceArtwork() throws Exception {

        when(checksumCalculator.calculate((byte[]) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((byte[]) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save((Artwork) any())).then(returnsFirstArg());
        
        byte[] bytes = Files.toByteArray(RESOURCE.getFile());
        ByteSourceArtworkStorageCommand command = new ByteSourceArtworkStorageCommand(sourceUri(), ByteSource.wrap(bytes));
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveFileArtwork() throws Exception {

        when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((File) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save((Artwork) any())).then(returnsFirstArg());
        
        File file = RESOURCE.getFile();
        FileArtworkStorageCommand command = new FileArtworkStorageCommand(sourceUri(), file);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveImageNodeArtwork() throws Exception {

        when(artworkRepository.save((Artwork) any())).then(returnsFirstArg());
        
        ImageNode imageNode = mock(ImageNode.class);
        when(imageNode.getFile()).thenReturn(RESOURCE.getFile());
        when(imageNode.getFileType()).thenReturn(FileType.of("image/png", "png"));
        when(imageNode.getChecksum()).thenReturn(CHECKSUM);
        ImageNodeArtworkStorageCommand command = new ImageNodeArtworkStorageCommand(sourceUri(), imageNode);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldDeleteCreatedFilesOnRollback() throws Exception {

        when(checksumCalculator.calculate((File) any())).thenReturn(CHECKSUM);
        when(fileTypeResolver.resolve((File) any())).thenReturn(FILE_TYPE);
        when(artworkRepository.save((Artwork) any())).then(returnsFirstArg());

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
    public void shouldDelete() throws Exception {
        
        File largeImageFile = new File(artworkFolder, PATH_LARGE);
        File smallImageFile = new File(artworkFolder, PATH_SMALL);
        Files.createParentDirs(largeImageFile);
        Files.createParentDirs(smallImageFile);
        Files.touch(largeImageFile);
        Files.touch(smallImageFile);

        Artwork artwork = artwork();
        when(artworkRepository.findOne((Long) any())).thenReturn(artwork);
        artworkStorage.delete(5L);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(artworkRepository).delete(artwork);
        
        assertThat(largeImageFile).doesNotExist();
        assertThat(smallImageFile).doesNotExist();
    }

    @Test
    public void shouldIgnoreNotExistingFilesWhenDeleting() throws Exception {
        Artwork artwork = artwork();
        when(artworkRepository.findOne((Long) any())).thenReturn(artwork);
        artworkStorage.delete(5L);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private void checkGetAndSaveArtwork(Supplier<ArtworkFiles> doGetAndSave) throws Exception {

        ArgumentCaptor<Artwork> savedArtwork = ArgumentCaptor.forClass(Artwork.class);

        ArtworkFiles artworkFiles = doGetAndSave.get();
        
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(artworkRepository).save(savedArtwork.capture());
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), 
                eq(SMALL_IMAGE_SIZE), eq(artworkFiles.getSmallFile()));
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), 
                eq(LARGE_IMAGE_SIZE), eq(artworkFiles.getLargeFile()));

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
        return Artwork.builder()
                .mimeType("image/png")
                .checksum("someChecksum")
                .largeImageSize(0L)
                .largeImagePath(PATH_LARGE)
                .smallImageSize(0L)
                .smallImagePath(PATH_SMALL)
                .sourceUri(sourceUri())
                .build();
    }
    
    private URI sourceUri() {
        return UriComponentsBuilder
                .fromUriString("file:sourceUri")
                .build().toUri();
    }
}
