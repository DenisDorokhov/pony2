package net.dorokhov.pony.artwork;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import net.dorokhov.pony.entity.Artwork;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import net.dorokhov.pony.filetree.ImageNode;
import net.dorokhov.pony.image.ImageSize;
import net.dorokhov.pony.image.ThumbnailGenerator;
import net.dorokhov.pony.repository.ArtworkRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static net.dorokhov.pony.util.RethrowingLambdas.rethrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArtworkServiceImplTests {
    
    private static final String PATH_LARGE = "foo.png";
    private static final String PATH_SMALL = "bar.png";

    private static final Resource RESOURCE = new ClassPathResource("image.png");
    private static final String CHECKSUM = "fc3adeae14ecc5f77d6dde58d40b1559";
    private static final FileType FILE_TYPE = new FileType("image/png", "png");
    
    private static final ImageSize LARGE_IMAGE_SIZE = new ImageSize(50, 50);
    private static final ImageSize SMALL_IMAGE_SIZE = new ImageSize(20, 20);
    
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private FileTypeResolver fileTypeResolver;
    @Mock
    private ChecksumCalculator checksumCalculator;
    @Mock
    private ThumbnailGenerator thumbnailGenerator;

    private ArtworkServiceImpl artworkService;
    
    private File artworkFolder;

    @Before
    public void setUp() throws Exception {
        
        artworkFolder = Files.createTempDir();
        artworkService = new ArtworkServiceImpl(artworkRepository, 
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
    public void getCountByTag() throws Exception {
        given(artworkRepository.countByTag(any())).willReturn(10L);
        assertThat(artworkService.getCountByTag("tag")).isEqualTo(10L);
    }

    @Test
    public void getCountByTagAndMinimalData() throws Exception {
        given(artworkRepository.countByTagAndDateGreaterThan(any(), any())).willReturn(100L);
        assertThat(artworkService.getCountByTagAndMinimalDate("tag", LocalDateTime.now())).isEqualTo(100L);
    }

    @Test
    public void getSizeByTag() throws Exception {
        given(artworkRepository.sumLargeImageSizeByTag(any())).willReturn(1000L);
        assertThat(artworkService.getSizeByTag("tag")).isEqualTo(1000L);
    }

    @Test
    public void getById() throws Exception {
        Artwork artwork = new Artwork();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkService.getById(2L)).hasValueSatisfying(fetchedArtwork -> 
                assertThat(fetchedArtwork).isSameAs(artwork));
    }

    @Test
    public void getByTag() throws Exception {
        Page<Artwork> page = new PageImpl<>(ImmutableList.of(new Artwork(), new Artwork()));
        given(artworkRepository.findByTag(any(), any())).willReturn(page);
        assertThat(artworkService.getByTag("tag", new PageRequest(0, 10))).isSameAs(page);
    }

    @Test
    public void getLargeImageFile() throws Exception {
        Artwork artwork = Artwork.builder().largeImagePath(PATH_LARGE).build();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkService.getLargeImageFile(3L)).hasValueSatisfying(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder, PATH_LARGE).getAbsolutePath()));
    }

    @Test
    public void getSmallImageFile() throws Exception {
        Artwork artwork = Artwork.builder().smallImagePath(PATH_SMALL).build();
        given(artworkRepository.findOne(any())).willReturn(artwork);
        assertThat(artworkService.getSmallImageFile(4L)).hasValueSatisfying(file -> 
                assertThat(file.getAbsolutePath()).isEqualTo(new File(artworkFolder, PATH_SMALL).getAbsolutePath()));
    }

    @Test
    public void getOrSaveByteSourceArtwork() throws Exception {
        
        byte[] bytes = Files.toByteArray(RESOURCE.getFile());
        
        ByteSourceArtworkDraft draft = new ByteSourceArtworkDraft(
                ByteSource.wrap(bytes), "tag",
                ImmutableMap.of("k1", "v1", "k2", "v2"));

        checkGetAndSaveArtwork(rethrow(() -> artworkService.getOrSave(draft)));
    }

    @Test
    public void getOrSaveFileArtwork() throws Exception {
        
        File file = RESOURCE.getFile();

        FileArtworkDraft draft = new FileArtworkDraft(
                file, "tag",
                ImmutableMap.of("k1", "v1", "k2", "v2"));

        checkGetAndSaveArtwork(rethrow(() -> artworkService.getOrSave(draft)));
    }

    @Test
    public void getOrSaveImageNodeArtwork() throws Exception {
        
        ImageNode imageNode = mock(ImageNode.class);
        
        given(imageNode.getFile()).willReturn(RESOURCE.getFile());
        given(imageNode.getFileType()).willReturn(new FileType("image/png", "png"));
        given(imageNode.getChecksum()).willReturn(CHECKSUM);

        ImageNodeArtworkDraft draft = new ImageNodeArtworkDraft(
                imageNode, "tag",
                ImmutableMap.of("k1", "v1", "k2", "v2"));

        checkGetAndSaveArtwork(rethrow(() -> artworkService.getOrSave(draft)));
    }

    @Test
    public void deleteCreatedFilesOnRollback() throws Exception {

        File file = RESOURCE.getFile();

        FileArtworkDraft draft = new FileArtworkDraft(file, "tag");

        Artwork artwork = artworkService.getOrSave(draft);
        
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

        Artwork artwork = Artwork.builder()
                .largeImagePath(PATH_LARGE)
                .smallImagePath(PATH_SMALL)
                .build();

        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkService.delete(5L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(artworkRepository).delete(artwork);
        
        assertThat(largeImageFile).doesNotExist();
        assertThat(smallImageFile).doesNotExist();
        assertThat(artworkFolder).exists();
    }

    @Test
    public void whenDeletingIgnoreNotExistingFiles() throws Exception {
        
        Artwork artwork = Artwork.builder()
                .largeImagePath(PATH_LARGE)
                .smallImagePath(PATH_SMALL)
                .build();
        
        given(artworkRepository.findOne(any())).willReturn(artwork);
        artworkService.delete(5L);
        
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

        given(artworkRepository.findByTagAndChecksum(any(), any())).willReturn(artwork);
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
        assertThat(savedArtwork.getTag()).hasValue("tag");
        assertThat(savedArtwork.getMetaData()).containsExactly(entry("k1", "v1"), entry("k2", "v2"));
    }
}
