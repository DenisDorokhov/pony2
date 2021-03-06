package net.dorokhov.pony.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.api.library.domain.ScanProgress;
import net.dorokhov.pony.api.library.domain.ScanProgress.Value;
import net.dorokhov.pony.api.library.domain.ScanResult;
import net.dorokhov.pony.core.library.repository.SongRepository;
import net.dorokhov.pony.api.library.service.command.EditCommand;
import net.dorokhov.pony.api.library.domain.WritableAudioData;
import net.dorokhov.pony.core.library.service.filetree.FileTreeScanner;
import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.core.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.core.library.service.scan.BatchLibraryImporter.WriteAndImportCommand;
import net.dorokhov.pony.core.library.service.scan.ScanResultCalculator.AudioFileProcessingResult;
import net.dorokhov.pony.core.library.service.scan.exception.SongNotFoundException;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.test.ScanResultFixtures.scanResult;
import static net.dorokhov.pony.test.SongFixtures.song;
import static net.dorokhov.pony.test.SongFixtures.songBuilder;
import static net.dorokhov.pony.api.library.domain.ScanType.EDIT;
import static net.dorokhov.pony.api.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryScannerTest {

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
    private BatchLibraryCleaner batchLibraryCleaner;
    @Mock
    private BatchLibraryImporter batchLibraryImporter;
    @Mock
    private BatchLibraryArtworkFinder batchLibraryArtworkFinder;

    @Captor
    private ArgumentCaptor<List<WriteAndImportCommand>> writeAndImportCommandsCaptor;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        libraryScanner = new LibraryScanner(logService, songRepository, fileTreeScanner, 
                scanResultCalculator, batchLibraryCleaner, batchLibraryImporter, batchLibraryArtworkFinder, 1);
    }

    @Test
    public void shouldScan() throws IOException {

        FolderNode folderNode = mock(FolderNode.class);
        when(fileTreeScanner.scanFolder(any())).thenReturn(folderNode);

        AudioNode audioNode1 = mock(AudioNode.class);
        AudioNode audioNode2 = mock(AudioNode.class);
        when(audioNode1.getFile()).thenReturn(new File("audioNode1"));
        when(audioNode2.getFile()).thenReturn(new File("audioNode2"));
        when(folderNode.getChildAudiosRecursively()).thenReturn(ImmutableList.of(audioNode1, audioNode2));

        ImageNode imageNode1 = mock(ImageNode.class);
        ImageNode imageNode2 = mock(ImageNode.class);
        when(imageNode1.getFile()).thenReturn(new File("imageNode1"));
        when(imageNode2.getFile()).thenReturn(new File("imageNode2"));
        when(folderNode.getChildImagesRecursively()).thenReturn(ImmutableList.of(imageNode1, imageNode2));

        ScanResult scanResultFixture = scanResult(FULL);
        when(scanResultCalculator.calculateAndSave(any())).then(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult -> {
                assertThat(audioFileProcessingResult.getScanType()).isSameAs(FULL);
                assertThat(audioFileProcessingResult.getTargetFiles()).containsExactly(tempFolder.getRoot());
                assertThat(audioFileProcessingResult.getFailedFiles()).isEmpty();
                assertThat(audioFileProcessingResult.getProcessedAudioFileCount()).isEqualTo(2);
            });
            return scanResultFixture;
        });

        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 2);
            return null;
        }).when(batchLibraryCleaner).cleanSongs(any(), any());
        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 2);
            return null;
        }).when(batchLibraryCleaner).cleanArtworks(any(), any());
        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 1);
            return new BatchLibraryImporter.ImportResult(ImmutableList.of(song(), song()), emptyList());
        }).when(batchLibraryImporter).readAndImport(any(), any());
        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(0);
            observer.onProgress(1, 2);
            return null;
        }).when(batchLibraryArtworkFinder).findAllArtworks(any());

        ScanObserver scanObserver = new ScanObserver();
        assertThat(libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()), scanObserver::observe))
                .isSameAs(scanResultFixture);

        verify(batchLibraryCleaner).cleanSongs(eq(ImmutableList.of(audioNode1, audioNode2)), any());
        verify(batchLibraryCleaner).cleanArtworks(eq(ImmutableList.of(imageNode1, imageNode2)), any());
        verify(batchLibraryImporter).readAndImport(eq(ImmutableList.of(audioNode1)), any());
        verify(batchLibraryImporter).readAndImport(eq(ImmutableList.of(audioNode2)), any());

        assertThat(scanObserver.size()).isEqualTo(11);
        scanObserver.assertThatProgressAtIndexSatisfies(0, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_PREPARING));
        scanObserver.assertThatProgressAtIndexSatisfies(1, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_SEARCHING_MEDIA));
        scanObserver.assertThatProgressAtIndexSatisfies(2, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_CLEANING_SONGS));
        scanObserver.assertThatProgressAtIndexSatisfies(3, scanProgress ->
                checkScanProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.FULL_CLEANING_SONGS));
        scanObserver.assertThatProgressAtIndexSatisfies(4, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_CLEANING_ARTWORKS));
        scanObserver.assertThatProgressAtIndexSatisfies(5, scanProgress ->
                checkScanProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.FULL_CLEANING_ARTWORKS));
        scanObserver.assertThatProgressAtIndexSatisfies(6, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_IMPORTING));
        scanObserver.assertThatProgressAtIndexSatisfies(7, scanProgress ->
                checkScanProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.FULL_IMPORTING));
        scanObserver.assertThatProgressAtIndexSatisfies(8, scanProgress ->
                checkScanProgress(scanProgress, Value.of(2, 2), ScanProgress.Step.FULL_IMPORTING));
        scanObserver.assertThatProgressAtIndexSatisfies(9, scanProgress ->
                checkScanProgress(scanProgress, null, ScanProgress.Step.FULL_SEARCHING_ARTWORKS));
        scanObserver.assertThatProgressAtIndexSatisfies(10, scanProgress ->
                checkScanProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.FULL_SEARCHING_ARTWORKS));
    }

    @Test
    public void shouldEdit() throws IOException, SongNotFoundException {

        File file1 = tempFolder.newFile();
        File file2 = tempFolder.newFile();
        AudioNode audioNode1 = mock(AudioNode.class);
        when(audioNode1.getFile()).thenReturn(file1);
        AudioNode audioNode2 = mock(AudioNode.class);
        when(audioNode2.getFile()).thenReturn(file2);
        when(fileTreeScanner.scanFile(eq(file1), any())).thenReturn(audioNode1);
        when(songRepository.findOne("1")).thenReturn(songBuilder()
                .path(file1.getAbsolutePath())
                .build());
        when(fileTreeScanner.scanFile(eq(file2), any())).thenReturn(audioNode2);
        when(songRepository.findOne("2")).thenReturn(songBuilder()
                .path(file2.getAbsolutePath())
                .build());

        ScanResult scanResultFixture = scanResult(EDIT);
        when(scanResultCalculator.calculateAndSave(any())).then(invocation -> {
            Object result = ((Supplier) invocation.getArgument(0)).get();
            assertThat(result).isInstanceOfSatisfying(AudioFileProcessingResult.class, audioFileProcessingResult -> {
                assertThat(audioFileProcessingResult.getScanType()).isSameAs(EDIT);
                assertThat(audioFileProcessingResult.getTargetFiles()).containsExactly(file1, file2);
                assertThat(audioFileProcessingResult.getFailedFiles()).isEmpty();
                assertThat(audioFileProcessingResult.getProcessedAudioFileCount()).isEqualTo(2);
            });
            return scanResultFixture;
        });

        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(1);
            observer.onProgress(1, 1);
            return new BatchLibraryImporter.ImportResult(ImmutableList.of(song(), song()), emptyList());
        }).when(batchLibraryImporter).writeAndImport(any(), any());
        doAnswer(invocation -> {
            ProgressObserver observer = invocation.getArgument(0);
            observer.onProgress(1, 2);
            return null;
        }).when(batchLibraryArtworkFinder).findAllArtworks(any());

        ScanObserver scanObserver = new ScanObserver();
        assertThat(libraryScanner.edit(ImmutableList.of(
                new EditCommand("1", WritableAudioData.builder().build()),
                new EditCommand("2", WritableAudioData.builder().build())
        ), emptyList(), scanObserver::observe)).isSameAs(scanResultFixture);

        verify(batchLibraryImporter, times(2)).writeAndImport(writeAndImportCommandsCaptor.capture(), any());
        
        List<WriteAndImportCommand> commands1 = writeAndImportCommandsCaptor.getAllValues().get(0);
        List<WriteAndImportCommand> commands2 = writeAndImportCommandsCaptor.getAllValues().get(1);
        
        assertThat(commands1).hasSize(1);
        assertThat(commands1).first().satisfies(command -> assertThat(command.getAudioNode()).isSameAs(audioNode1));
        assertThat(commands2).hasSize(1);
        assertThat(commands2).first().satisfies(command -> assertThat(command.getAudioNode()).isSameAs(audioNode2));

        assertThat(scanObserver.size()).isEqualTo(6);
        scanObserver.assertThatProgressAtIndexSatisfies(0, scanProgress ->
                checkEditProgress(scanProgress, null, ScanProgress.Step.EDIT_PREPARING, file1, file2));
        scanObserver.assertThatProgressAtIndexSatisfies(1, scanProgress ->
                checkEditProgress(scanProgress, null, ScanProgress.Step.EDIT_WRITING, file1, file2));
        scanObserver.assertThatProgressAtIndexSatisfies(2, scanProgress ->
                checkEditProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.EDIT_WRITING, file1, file2));
        scanObserver.assertThatProgressAtIndexSatisfies(3, scanProgress ->
                checkEditProgress(scanProgress, Value.of(2, 2), ScanProgress.Step.EDIT_WRITING, file1, file2));
        scanObserver.assertThatProgressAtIndexSatisfies(4, scanProgress ->
                checkEditProgress(scanProgress, null, ScanProgress.Step.EDIT_SEARCHING_ARTWORKS, file1, file2));
        scanObserver.assertThatProgressAtIndexSatisfies(5, scanProgress ->
                checkEditProgress(scanProgress, Value.of(1, 2), ScanProgress.Step.EDIT_SEARCHING_ARTWORKS, file1, file2));
    }

    @Test
    public void shouldFailScanIfFileNotFound() {

        File file = new File("notExistingFile");

        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(file), null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void shouldFailScanIfFileIsNotDirectory() throws IOException {

        File file = tempFolder.newFile("someFile");

        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(file), null))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldFailScanOnUnexpectedException() {

        Exception calculationException = new RuntimeException();
        when(scanResultCalculator.calculateAndSave(any())).thenThrow(calculationException);

        ScanObserver scanObserver = new ScanObserver();
        assertThatThrownBy(() -> libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()), scanObserver::observe))
                .isSameAs(calculationException);

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldFailEditIfSongNotFound() {

        when(songRepository.findOne((String) any())).thenReturn(null);
        EditCommand command = new EditCommand("1", WritableAudioData.builder().build());

        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command), emptyList(), null))
                .isInstanceOfSatisfying(SongNotFoundException.class, e -> 
                        assertThat(e.getSongId()).isEqualTo("1"));
    }

    @Test
    public void shouldFailEditIfFileNotFound() {

        when(songRepository.findOne((String) any())).thenReturn(song());
        EditCommand command = new EditCommand("1", WritableAudioData.builder().build());

        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command), emptyList(), null))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void shouldFailEditIfFileIsNotSong() throws IOException {

        when(fileTreeScanner.scanFile(any(), any())).thenReturn(mock(ImageNode.class));
        when(songRepository.findOne((String) any())).thenReturn(songBuilder()
                .path(tempFolder.newFile().getAbsolutePath())
                .build());
        EditCommand command = new EditCommand("1", WritableAudioData.builder().build());

        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(command), emptyList(), null))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void shouldFailEditOnUnexpectedException() throws IOException {

        AudioNode audioNode = mock(AudioNode.class);
        when(fileTreeScanner.scanFile(any(), any())).thenReturn(audioNode);
        File file = tempFolder.newFile();
        when(songRepository.findOne((String) any())).thenReturn(songBuilder()
                .path(file.getAbsolutePath())
                .build());

        Exception calculationException = new RuntimeException();
        when(scanResultCalculator.calculateAndSave(any())).thenThrow(calculationException);

        ScanObserver scanObserver = new ScanObserver();
        assertThatThrownBy(() -> libraryScanner.edit(ImmutableList.of(
                new EditCommand("1", WritableAudioData.builder().build())
        ), emptyList(), scanObserver::observe)).isSameAs(calculationException);

        verify(logService).error(any(), any(), any());
    }

    @Test
    public void shouldNotFailWhenObserverThrowsException() throws IOException {
        libraryScanner.scan(ImmutableList.of(tempFolder.getRoot()), scanProgress -> {
            throw new RuntimeException();
        });
    }

    private void checkScanProgress(ScanProgress progress, Value value, ScanProgress.Step step) {
        assertThat(progress.getFiles()).containsExactly(tempFolder.getRoot());
        assertThat(progress.getValue()).isEqualTo(value);
        assertThat(progress.getStep()).isEqualTo(step);
    }

    private void checkEditProgress(ScanProgress progress, Value value, ScanProgress.Step step, File... files) {
        assertThat(progress.getFiles()).containsExactly(files);
        assertThat(progress.getValue()).isEqualTo(value);
        assertThat(progress.getStep()).isEqualTo(step);
    }

    private static class ScanObserver {

        private final List<ScanProgress> calls = new ArrayList<>();

        public void observe(ScanProgress scanProgress) {
            calls.add(scanProgress);
        }

        public int size() {
            return calls.size();
        }

        public void assertThatProgressAtIndexSatisfies(int index, Consumer<ScanProgress> handler) {
            assertThat(calls.get(index)).satisfies(handler);
        }
    }
}