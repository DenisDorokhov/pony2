package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.fixture.ScanResultFixtures;
import net.dorokhov.pony.fixture.SongFixtures;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.FileTreeScanner;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FolderNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.impl.scan.ScanResultCalculator.AudioFileProcessingResult;
import net.dorokhov.pony.log.service.LogService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony.library.domain.ScanType.EDIT;
import static net.dorokhov.pony.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LibraryScannerTest {

    @InjectMocks
    private LibraryScanner libraryScanner;

    @Mock
    private LogService logService;
    @Mock
    private SongRepository songRepository;
    @Mock
    private FileTreeScanner fileTreeScanner;
    @Mock
    private ScanResultCalculator scanResultCalculator;
    @Mock
    private LibraryCleaner libraryCleaner;
    @Mock
    private LibraryImporter libraryImporter;
    @Mock
    private LibraryArtworkFinder libraryArtworkFinder;
    @Spy
    private Executor executor = new SimpleAsyncTaskExecutor();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shouldScan() throws Exception {

        FolderNode folderNode = mock(FolderNode.class);
        given(fileTreeScanner.scanFolder(any())).willReturn(folderNode);

        AudioNode audioNode1 = mock(AudioNode.class);
        AudioNode audioNode2 = mock(AudioNode.class);
        given(folderNode.getChildAudios(true)).willReturn(ImmutableList.of(audioNode1, audioNode2));

        ImageNode imageNode1 = mock(ImageNode.class);
        ImageNode imageNode2 = mock(ImageNode.class);
        given(folderNode.getChildImages(true)).willReturn(ImmutableList.of(imageNode1, imageNode2));

        ScanResult scanResultFixture = ScanResultFixtures.get(FULL);
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult -> {
                assertThat(audioFileProcessingResult.getScanType()).isEqualTo(FULL);
                assertThat(audioFileProcessingResult.getTargetFiles()).containsExactly(tempFolder.getRoot());
                assertThat(audioFileProcessingResult.getFailedFiles()).isEmpty();
                assertThat(audioFileProcessingResult.getProcessedAudioFileCount()).isEqualTo(2);
            });
            return scanResultFixture;
        });

        ScanProgressObserverImpl scanProgressObserver = new ScanProgressObserverImpl();
        libraryScanner.addProgressObserver(scanProgressObserver);

        doAnswer(invocation -> {
            ItemProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 2);
            return null;
        }).when(libraryCleaner).cleanSongs(any(), any());
        doAnswer(invocation -> {
            ItemProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 2);
            return null;
        }).when(libraryCleaner).cleanArtworks(any(), any());
        doAnswer(invocation -> {
            ItemProgressObserver observer = invocation.getArgument(0);
            observer.onProgress(1, 2);
            return null;
        }).when(libraryArtworkFinder).findArtworks(any());

        assertThat(libraryScanner.getStatus()).satisfies(scanStatus -> {
            assertThat(scanStatus.isRunning()).isFalse();
            assertThat(scanStatus.getProgress()).isNull();
        });
        assertThat(libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()))).isSameAs(scanResultFixture);
        assertThat(libraryScanner.getStatus()).satisfies(scanStatus -> {
            assertThat(scanStatus.isRunning()).isFalse();
            assertThat(scanStatus.getProgress()).isNull();
        });

        verify(libraryCleaner).cleanSongs(eq(ImmutableList.of(audioNode1, audioNode2)), any());
        verify(libraryCleaner).cleanArtworks(eq(ImmutableList.of(imageNode1, imageNode2)), any());
        verify(libraryImporter).importSong(audioNode1);
        verify(libraryImporter).importSong(audioNode2);

        assertThat(scanProgressObserver.size()).isEqualTo(12);
        AtomicReference<ScanStatus> lastScanStatusReference = new AtomicReference<>();
        scanProgressObserver.assertThatStartedAt(0, scanStatus -> {
            assertThat(scanStatus.isRunning()).isTrue();
            assertThat(scanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_PREPARING));
            lastScanStatusReference.set(scanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(1, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_SEARCHING_MEDIA));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(2, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_CLEANING_SONGS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(3, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.5, ScanStatus.Progress.Step.FULL_CLEANING_SONGS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(4, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_CLEANING_ARTWORKS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(5, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.5, ScanStatus.Progress.Step.FULL_CLEANING_ARTWORKS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(6, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_IMPORTING));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(7, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.5, ScanStatus.Progress.Step.FULL_IMPORTING));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(8, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 1.0, ScanStatus.Progress.Step.FULL_IMPORTING));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(9, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_SEARCHING_ARTWORKS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(10, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.5, ScanStatus.Progress.Step.FULL_SEARCHING_ARTWORKS));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatCompleteAt(11, (lastScanStatus, scanResult) -> {
            assertThat(lastScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(scanResult).isSameAs(scanResultFixture);
        });

        libraryScanner.removeProgressObserver(scanProgressObserver);
        libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()));

        assertThat(scanProgressObserver.size()).isEqualTo(12);
    }

    @Test
    public void shouldEdit() throws Exception {

        File file1 = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        AudioNode audioNode1 = mock(AudioNode.class);
        given(audioNode1.getFile()).willReturn(file1);
        AudioNode audioNode2 = mock(AudioNode.class);
        given(audioNode2.getFile()).willReturn(file2);
        given(fileTreeScanner.scanFile(file1)).willReturn(audioNode1);
        given(songRepository.findOne(1L)).willReturn(SongFixtures.builder()
                .path(file1.getAbsolutePath())
                .build());
        given(fileTreeScanner.scanFile(file2)).willReturn(audioNode2);
        given(songRepository.findOne(2L)).willReturn(SongFixtures.builder()
                .path(file2.getAbsolutePath())
                .build());

        ScanResult scanResultFixture = ScanResultFixtures.get(EDIT);
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult -> {
                assertThat(audioFileProcessingResult.getScanType()).isEqualTo(EDIT);
                assertThat(audioFileProcessingResult.getTargetFiles()).containsExactly(file1, file2);
                assertThat(audioFileProcessingResult.getFailedFiles()).isEmpty();
                assertThat(audioFileProcessingResult.getProcessedAudioFileCount()).isEqualTo(2);
            });
            return scanResultFixture;
        });

        ScanProgressObserverImpl scanProgressObserver = new ScanProgressObserverImpl();
        libraryScanner.addProgressObserver(scanProgressObserver);

        doAnswer(invocation -> {
            ItemProgressObserver observer = invocation.getArgument(0);
            observer.onProgress(1, 2);
            return null;
        }).when(libraryArtworkFinder).findArtworks(any());

        assertThat(libraryScanner.getStatus()).satisfies(scanStatus -> {
            assertThat(scanStatus.isRunning()).isFalse();
            assertThat(scanStatus.getProgress()).isNull();
        });
        assertThat(libraryScanner.edit(ImmutableList.of(
                new EditCommand(1L, WritableAudioData.builder().build()),
                new EditCommand(2L, WritableAudioData.builder().build())
        ))).isSameAs(scanResultFixture);
        assertThat(libraryScanner.getStatus()).satisfies(scanStatus -> {
            assertThat(scanStatus.isRunning()).isFalse();
            assertThat(scanStatus.getProgress()).isNull();
        });

        verify(libraryImporter).writeAndImportSong(eq(audioNode1), any());
        verify(libraryImporter).writeAndImportSong(eq(audioNode2), any());

        assertThat(scanProgressObserver.size()).isEqualTo(7);
        AtomicReference<ScanStatus> lastScanStatusReference = new AtomicReference<>();
        scanProgressObserver.assertThatStartedAt(0, scanStatus -> {
            assertThat(scanStatus.isRunning()).isTrue();
            assertThat(scanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.0, ScanStatus.Progress.Step.EDIT_PREPARING, file1, file2));
            lastScanStatusReference.set(scanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(1, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.0, ScanStatus.Progress.Step.EDIT_WRITING, file1, file2));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(2, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.5, ScanStatus.Progress.Step.EDIT_WRITING, file1, file2));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(3, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 1.0, ScanStatus.Progress.Step.EDIT_WRITING, file1, file2));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(4, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.0, ScanStatus.Progress.Step.EDIT_SEARCHING_ARTWORKS, file1, file2));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatProgressedAt(5, (oldScanStatus, newScanStatus) -> {
            assertThat(oldScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(newScanStatus.isRunning()).isTrue();
            assertThat(newScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.5, ScanStatus.Progress.Step.EDIT_SEARCHING_ARTWORKS, file1, file2));
            lastScanStatusReference.set(newScanStatus);
        });
        scanProgressObserver.assertThatCompleteAt(6, (lastScanStatus, scanResult) -> {
            assertThat(lastScanStatus).isSameAs(lastScanStatusReference.get());
            assertThat(scanResult).isSameAs(scanResultFixture);
        });

        libraryScanner.removeProgressObserver(scanProgressObserver);
        libraryScanner.edit(ImmutableList.of(
                new EditCommand(1L, WritableAudioData.builder().build()),
                new EditCommand(2L, WritableAudioData.builder().build())
        ));

        assertThat(scanProgressObserver.size()).isEqualTo(7);
    }

    @Test
    public void shouldFailScanIfFileNotFound() throws Exception {
        File file = new File("notExistingFile");
        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(file)))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void shouldFailScanIfFileIsNotDirectory() throws Exception {
        File file = tempFolder.newFile("someFile");
        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(file)))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldFailScanIfAlreadyScanning() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            latch.countDown();
            Thread.sleep(100);
            return null;
        });

        //noinspection CodeBlock2Expr
        executor.execute(rethrow(() -> {
            libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()));
        }));
        latch.await();

        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(tempFolder.getRoot())))
                .isInstanceOf(ConcurrentScanException.class);
    }

    @Test
    public void shouldFailScanOnUnexpectedException() throws Exception {
        
        Exception calculationException = new RuntimeException();
        given(scanResultCalculator.calculateAndSave(any())).willThrow(calculationException);

        ScanProgressObserverImpl scanProgressObserver = new ScanProgressObserverImpl();
        libraryScanner.addProgressObserver(scanProgressObserver);
        
        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(tempFolder.getRoot())))
                .isSameAs(calculationException);

        verify(logService).error(any(), any(), any());

        assertThat(scanProgressObserver.size()).isEqualTo(2);
        scanProgressObserver.assertThatStartedAt(0, scanStatus -> {
            assertThat(scanStatus.isRunning()).isTrue();
            assertThat(scanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_PREPARING));
        });
        scanProgressObserver.assertThatFailedAt(1, (lastScanStatus, e) -> {
            assertThat(lastScanStatus.isRunning()).isTrue();
            assertThat(lastScanStatus.getProgress()).satisfies(progress ->
                    checkScanProgress(progress, 0.0, ScanStatus.Progress.Step.FULL_PREPARING));
            assertThat(e).isSameAs(calculationException);
        });
    }

    @Test
    public void shouldLogScanErrorIfImportFailed() throws Exception {

        FolderNode folderNode = mock(FolderNode.class);
        AudioNode audioNode = mock(AudioNode.class);
        given(folderNode.getChildAudios(true)).willReturn(ImmutableList.of(audioNode));
        given(folderNode.getChildImages(true)).willReturn(ImmutableList.of());
        File file = new File(tempFolder.getRoot(), "someFile");
        given(audioNode.getFile()).willReturn(file);

        given(fileTreeScanner.scanFolder(any())).willReturn(folderNode);
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult ->
                    assertThat(audioFileProcessingResult.getFailedFiles()).containsExactly(file));
            return null;
        });
        given(libraryImporter.importSong(any())).willThrow(new IOException());

        libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()));

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditIfSongNotFound() throws Exception {
        given(songRepository.findOne(any())).willReturn(null);
        EditCommand command = new EditCommand(1L, WritableAudioData.builder().build());
        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command)))
                .isInstanceOf(SongNotFoundException.class);
    }

    @Test
    public void shouldFailEditIfFileNotFound() throws Exception {
        given(songRepository.findOne(any())).willReturn(SongFixtures.get());
        EditCommand command = new EditCommand(1L, WritableAudioData.builder().build());
        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command)))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void shouldFailEditIfFileIsNotSong() throws Exception {
        given(fileTreeScanner.scanFile(any())).willReturn(mock(ImageNode.class));
        given(songRepository.findOne(any())).willReturn(SongFixtures.builder()
                .path(tempFolder.newFile().getAbsolutePath())
                .build());
        EditCommand command = new EditCommand(1L, WritableAudioData.builder().build());
        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command)))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldFailEditIfAlreadyScanning() throws Exception {

        AudioNode audioNode = mock(AudioNode.class);
        given(fileTreeScanner.scanFile(any())).willReturn(audioNode);
        given(songRepository.findOne(any())).willReturn(SongFixtures.builder()
                .path(tempFolder.newFile().getAbsolutePath())
                .build());

        CountDownLatch latch = new CountDownLatch(1);
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            latch.countDown();
            Thread.sleep(100);
            return null;
        });

        EditCommand command = new EditCommand(1L, WritableAudioData.builder().build());
        //noinspection CodeBlock2Expr
        executor.execute(rethrow(() -> {
            libraryScanner.edit(ImmutableList.of(command));
        }));
        latch.await();

        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command)))
                .isInstanceOf(ConcurrentScanException.class);
    }

    @Test
    public void shouldFailEditOnUnexpectedException() throws Exception {

        AudioNode audioNode = mock(AudioNode.class);
        given(fileTreeScanner.scanFile(any())).willReturn(audioNode);
        File file = tempFolder.newFile();
        given(songRepository.findOne(any())).willReturn(SongFixtures.builder()
                .path(file.getAbsolutePath())
                .build());

        Exception calculationException = new RuntimeException();
        given(scanResultCalculator.calculateAndSave(any())).willThrow(calculationException);

        ScanProgressObserverImpl scanProgressObserver = new ScanProgressObserverImpl();
        libraryScanner.addProgressObserver(scanProgressObserver);

        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(
                new EditCommand(1L, WritableAudioData.builder().build())
        ))).isSameAs(calculationException);

        verify(logService).error(any(), any(), any());

        assertThat(scanProgressObserver.size()).isEqualTo(2);
        scanProgressObserver.assertThatStartedAt(0, scanStatus -> {
            assertThat(scanStatus.isRunning()).isTrue();
            assertThat(scanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.0, ScanStatus.Progress.Step.EDIT_PREPARING, file));
        });
        scanProgressObserver.assertThatFailedAt(1, (lastScanStatus, e) -> {
            assertThat(lastScanStatus.isRunning()).isTrue();
            assertThat(lastScanStatus.getProgress()).satisfies(progress ->
                    checkEditProgress(progress, 0.0, ScanStatus.Progress.Step.EDIT_PREPARING, file));
            assertThat(e).isSameAs(calculationException);
        });
    }

    @Test
    public void shouldLogEditErrorIfImportFailed() throws Exception {

        AudioNode audioNode = mock(AudioNode.class);
        File file = tempFolder.newFile();
        given(audioNode.getFile()).willReturn(file);
        given(fileTreeScanner.scanFile(any())).willReturn(audioNode);
        given(songRepository.findOne(any())).willReturn(SongFixtures.builder()
                .path(tempFolder.newFile().getAbsolutePath())
                .build());
        given(scanResultCalculator.calculateAndSave(any())).willAnswer(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult ->
                    assertThat(audioFileProcessingResult.getFailedFiles()).containsExactly(file));
            return null;
        });
        given(libraryImporter.writeAndImportSong(any(), any())).willThrow(new IOException());

        EditCommand command = new EditCommand(1L, WritableAudioData.builder().build());
        libraryScanner.edit(ImmutableList.of(command));
        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldNotFailWhenObserverThrowsException() throws Exception {
        
        ScanProgressObserver scanProgressObserver = new ScanProgressObserver() {
            @Override
            public void onScanStarted(ScanStatus scanStatus) {
                throw new RuntimeException();
            }

            @Override
            public void onScanProgress(ScanStatus lastScanStatus, ScanStatus newScanStatus) {
                throw new RuntimeException();
            }

            @Override
            public void onScanFailed(ScanStatus lastScanStatus, Exception e) {
                throw new RuntimeException();
            }

            @Override
            public void onScanComplete(ScanStatus lastScanStatus, ScanResult scanResult) {
                throw new RuntimeException();
            }
        };
        
        libraryScanner.addProgressObserver(scanProgressObserver);
        libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()));
    }

    private void checkScanProgress(ScanStatus.Progress progress, double value, ScanStatus.Progress.Step step) {
        assertThat(progress.getFiles()).containsExactly(tempFolder.getRoot());
        assertThat(progress.getValue()).isEqualTo(value);
        assertThat(progress.getStep()).isEqualTo(step);
    }

    private void checkEditProgress(ScanStatus.Progress progress, double value, ScanStatus.Progress.Step step, File... files) {
        assertThat(progress.getFiles()).containsExactly(files);
        assertThat(progress.getValue()).isEqualTo(value);
        assertThat(progress.getStep()).isEqualTo(step);
    }

    private static class ScanProgressObserverImpl implements ScanProgressObserver {

        private final List<Object> calls = new ArrayList<>();

        @Override
        public void onScanStarted(ScanStatus scanStatus) {
            calls.add(scanStatus);
        }

        @Override
        public void onScanProgress(ScanStatus lastScanStatus, ScanStatus newScanStatus) {
            calls.add(new AbstractMap.SimpleEntry<>(lastScanStatus, newScanStatus));
        }

        @Override
        public void onScanFailed(ScanStatus lastScanStatus, Exception e) {
            calls.add(new AbstractMap.SimpleEntry<>(lastScanStatus, e));
        }

        @Override
        public void onScanComplete(ScanStatus lastScanStatus, ScanResult scanResult) {
            calls.add(new AbstractMap.SimpleEntry<>(lastScanStatus, scanResult));
        }

        public int size() {
            return calls.size();
        }

        public void assertThatStartedAt(int index, Consumer<ScanStatus> handler) {
            assertThat(calls.get(index)).isInstanceOfSatisfying(ScanStatus.class, handler);
        }

        public void assertThatProgressedAt(int index, BiConsumer<ScanStatus, ScanStatus> handler) {
            assertThat(calls.get(index)).isInstanceOfSatisfying(Map.Entry.class, entry -> {
                assertThat(entry.getKey()).isInstanceOf(ScanStatus.class);
                assertThat(entry.getValue()).isInstanceOf(ScanStatus.class);
                handler.accept((ScanStatus) entry.getKey(), (ScanStatus) entry.getValue());
            });
        }

        public void assertThatFailedAt(int index, BiConsumer<ScanStatus, Exception> handler) {
            assertThat(calls.get(index)).isInstanceOfSatisfying(Map.Entry.class, entry -> {
                assertThat(entry.getKey()).isInstanceOf(ScanStatus.class);
                assertThat(entry.getValue()).isInstanceOf(Exception.class);
                handler.accept((ScanStatus) entry.getKey(), (Exception) entry.getValue());
            });
        }

        public void assertThatCompleteAt(int index, BiConsumer<ScanStatus, ScanResult> handler) {
            assertThat(calls.get(index)).isInstanceOfSatisfying(Map.Entry.class, entry -> {
                assertThat(entry.getKey()).isInstanceOf(ScanStatus.class);
                assertThat(entry.getValue()).isInstanceOf(ScanResult.class);
                handler.accept((ScanStatus) entry.getKey(), (ScanResult) entry.getValue());
            });
        }
    }
}