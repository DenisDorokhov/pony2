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

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.support.TransactionSynchronizationManager.*;

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
    public final TemporaryFolder artworkFolder = new TemporaryFolder();
    
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private FileTypeResolver fileTypeResolver;
    @Mock
    private ChecksumCalculator checksumCalculator;
    @Mock
    private ThumbnailGenerator thumbnailGenerator;

    private ArtworkStorage artworkStorage;

    @Before
    public void setUp() throws Exception {
        artworkStorage = new ArtworkStorage(artworkRepository, 
                fileTypeResolver, checksumCalculator, 
                thumbnailGenerator, 
                artworkFolder.getRoot(), 
                new int[]{SMALL_IMAGE_SIZE.getWidth(), SMALL_IMAGE_SIZE.getHeight()}, 
                new int[]{LARGE_IMAGE_SIZE.getWidth(), LARGE_IMAGE_SIZE.getHeight()});
        initSynchronization();
    }

    @After
    public void tearDown() throws Exception {
        clearSynchronization();
    }

    @Test
    public void shouldGetLargeImageFile() throws Exception {
        Artwork artwork = artwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkStorage.getLargeImageFile(1L)).satisfies(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder.getRoot(), PATH_LARGE).getAbsolutePath()));
    }

    @Test
    public void shouldGetSmallImageFile() throws Exception {
        Artwork artwork = artwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkStorage.getSmallImageFile(1L)).satisfies(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder.getRoot(), PATH_SMALL).getAbsolutePath()));
    }

    @Test
    public void shouldGetOrSaveByteSourceArtwork() throws Exception {

        given(checksumCalculator.calculate((byte[]) any())).willReturn(CHECKSUM);
        given(fileTypeResolver.resolve((byte[]) any())).willReturn(FILE_TYPE);
        given(artworkRepository.save((Artwork) any())).willAnswer(returnsFirstArg());
        
        byte[] bytes = Files.toByteArray(RESOURCE.getFile());
        ByteSourceArtworkCommand command = new ByteSourceArtworkCommand(sourceUri(), ByteSource.wrap(bytes));
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveFileArtwork() throws Exception {

        given(checksumCalculator.calculate((File) any())).willReturn(CHECKSUM);
        given(fileTypeResolver.resolve((File) any())).willReturn(FILE_TYPE);
        given(artworkRepository.save((Artwork) any())).willAnswer(returnsFirstArg());
        
        File file = RESOURCE.getFile();
        FileArtworkCommand command = new FileArtworkCommand(sourceUri(), file);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldGetOrSaveImageNodeArtwork() throws Exception {

        given(artworkRepository.save((Artwork) any())).willAnswer(returnsFirstArg());
        
        ImageNode imageNode = mock(ImageNode.class);
        given(imageNode.getFile()).willReturn(RESOURCE.getFile());
        given(imageNode.getFileType()).willReturn(FileType.of("image/png", "png"));
        given(imageNode.getChecksum()).willReturn(CHECKSUM);
        ImageNodeArtworkCommand command = new ImageNodeArtworkCommand(sourceUri(), imageNode);
        checkGetAndSaveArtwork(rethrow(() -> artworkStorage.getOrSave(command)));
    }

    @Test
    public void shouldDeleteCreatedFilesOnRollback() throws Exception {

        given(checksumCalculator.calculate((File) any())).willReturn(CHECKSUM);
        given(fileTypeResolver.resolve((File) any())).willReturn(FILE_TYPE);
        given(artworkRepository.save((Artwork) any())).willAnswer(returnsFirstArg());

        FileArtworkCommand command = new FileArtworkCommand(sourceUri(), RESOURCE.getFile());

        Artwork artwork = artworkStorage.getOrSave(command);
        
        File largeFile = new File(artworkFolder.getRoot(), artwork.getLargeImagePath());
        File smallFile = new File(artworkFolder.getRoot(), artwork.getSmallImagePath());
        Files.createParentDirs(largeFile);
        Files.createParentDirs(smallFile);
        Files.touch(largeFile);
        Files.touch(smallFile);
        
        getSynchronizations().forEach(transactionSynchronization -> 
                transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        
        assertThat(largeFile).doesNotExist();
        assertThat(smallFile).doesNotExist();
    }

    @Test
    public void shouldDelete() throws Exception {
        
        File largeImageFile = new File(artworkFolder.getRoot(), PATH_LARGE);
        File smallImageFile = new File(artworkFolder.getRoot(), PATH_SMALL);
        
        Files.touch(largeImageFile);
        Files.touch(smallImageFile);

        Artwork artwork = artwork();

        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkStorage.delete(5L);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(artworkRepository).delete(artwork);
        
        assertThat(largeImageFile).doesNotExist();
        assertThat(smallImageFile).doesNotExist();
    }

    @Test
    public void shouldIgnoreNotExistingFilesWhenDeleting() throws Exception {
        Artwork artwork = artwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkStorage.delete(5L);
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private void checkGetAndSaveArtwork(Supplier<Artwork> doGetAndSave) throws Exception {

        ArgumentCaptor<Artwork> savedArtwork = ArgumentCaptor.forClass(Artwork.class);

        Artwork artwork = doGetAndSave.get();
        
        getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(artworkRepository).save(savedArtwork.capture());
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), eq(SMALL_IMAGE_SIZE), eq(new File(artworkFolder.getRoot(), artwork.getSmallImagePath())));
        verify(thumbnailGenerator).generateThumbnail((InputStream) any(), eq(LARGE_IMAGE_SIZE), eq(new File(artworkFolder.getRoot(), artwork.getLargeImagePath())));

        assertThat(savedArtwork.getValue()).isSameAs(artwork);
        checkSavedArtwork(savedArtwork.getValue());

        given(artworkRepository.findByChecksum(any())).willReturn(artwork);
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
                .fromUriString("sourceUri")
                .build().toUri();
    }
}
