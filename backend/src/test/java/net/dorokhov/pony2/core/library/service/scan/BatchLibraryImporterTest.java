package net.dorokhov.pony2.core.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData;
import net.dorokhov.pony2.api.library.domain.WritableAudioData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.core.library.ProgressObserverFixture;
import net.dorokhov.pony2.core.library.service.AudioTagger;
import net.dorokhov.pony2.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryImportPlanner.Plan;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryImporter.ImportResult;
import net.dorokhov.pony2.core.library.service.scan.BatchLibraryImporter.WriteAndImportCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony2.core.library.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony2.test.ReadableAudioDataFixtures.readableAudioData;
import static net.dorokhov.pony2.test.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchLibraryImporterTest {

    @InjectMocks
    private BatchLibraryImporter batchLibraryImporter;

    @Mock
    private BatchLibraryImportPlanner batchLibraryImportPlanner;
    @Mock
    private AudioTagger audioTagger;
    @Mock
    private LibraryImporter libraryImporter;
    @Mock
    private LibraryArtworkFinder libraryArtworkFinder;
    @Mock
    @SuppressWarnings("unused")
    private LogService logService;

    @Spy
    @SuppressWarnings("unused")
    private final Executor executor = new SyncTaskExecutor();
    @Spy
    @SuppressWarnings("unused")
    private final PlatformTransactionManager transactionManager = transactionManager();

    @Test
    public void shouldReadAndImport() throws IOException {
        
        AudioNode audioNodeToImport1 = audioNode();
        AudioNode audioNodeToImport2 = audioNode();
        AudioNode audioNodeToSkip1 = audioNode();
        AudioNode audioNodeToSkip2 = audioNode();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(new Plan(
                ImmutableList.of(audioNodeToImport1, audioNodeToImport2), 
                ImmutableList.of(audioNodeToSkip1, audioNodeToSkip2)
        ));
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());
        when(libraryImporter.importArtwork(any())).thenReturn(song());
        
        ProgressObserverFixture observer = new ProgressObserverFixture();
        ImportResult importResult = batchLibraryImporter.readAndImport(ImmutableList.of(
                audioNodeToImport1, audioNodeToImport2, audioNodeToSkip1, audioNodeToSkip2
        ), observer);
        
        assertThat(importResult.getImportedSongs()).hasSize(4);
        assertThat(importResult.getFailedFiles()).hasSize(0);
        verify(libraryImporter, times(2)).importAudioData(any(), any());
        verify(libraryImporter, times(2)).importArtwork(any());
        
        assertThat(observer.size()).isEqualTo(2);
        observer.assertThatAt(0, 1, 2);
        observer.assertThatAt(1, 2, 2);
    }

    @Test
    public void shouldNotImportWhenReadingFailed() throws IOException {
        
        AudioNode audioNodeToImport1 = audioNodeThrowing();
        AudioNode audioNodeToImport2 = audioNodeThrowing();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(
                new Plan(ImmutableList.of(audioNodeToImport1, audioNodeToImport2), emptyList()));

        ProgressObserverFixture observer = new ProgressObserverFixture();
        ImportResult importResult = batchLibraryImporter.readAndImport(
                ImmutableList.of(audioNodeToImport1, audioNodeToImport2), observer);

        assertThat(importResult.getImportedSongs()).hasSize(0);
        assertThat(importResult.getFailedFiles()).containsExactly(audioNodeToImport1.getFile(), audioNodeToImport2.getFile());
        verify(libraryImporter, never()).importAudioData(any(), any());
        verify(libraryImporter, never()).importArtwork(any());

        assertThat(observer.size()).isEqualTo(2);
        observer.assertThatAt(0, 1, 2);
        observer.assertThatAt(1, 2, 2);
    }

    @Test
    public void shouldWriteAndImport() throws IOException {

        WriteAndImportCommand command1 = new WriteAndImportCommand(audioNode(), new WritableAudioData());
        WriteAndImportCommand command2 = new WriteAndImportCommand(audioNode(), new WritableAudioData());
        when(audioTagger.write(any(), any())).thenReturn(readableAudioData());
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());

        ProgressObserverFixture observer = new ProgressObserverFixture();
        ImportResult importResult = batchLibraryImporter.writeAndImport(ImmutableList.of(command1, command2), observer);

        assertThat(importResult.getImportedSongs()).hasSize(2);
        assertThat(importResult.getFailedFiles()).hasSize(0);
        verify(libraryImporter, times(2)).importAudioData(any(), any());
        verify(libraryImporter, never()).importArtwork(any());

        assertThat(observer.size()).isEqualTo(2);
        observer.assertThatAt(0, 1, 2);
        observer.assertThatAt(1, 2, 2);
    }

    @Test
    public void shouldNotImportWhenWritingFailed() throws IOException {

        WriteAndImportCommand command1 = new WriteAndImportCommand(audioNode(), new WritableAudioData());
        WriteAndImportCommand command2 = new WriteAndImportCommand(audioNode(), new WritableAudioData());
        when(audioTagger.write(any(), any())).thenThrow(new IOException());

        ProgressObserverFixture observer = new ProgressObserverFixture();
        ImportResult importResult = batchLibraryImporter.writeAndImport(ImmutableList.of(command1, command2), observer);

        assertThat(importResult.getImportedSongs()).hasSize(0);
        assertThat(importResult.getFailedFiles()).containsExactly(command1.getAudioNode().getFile(), command2.getAudioNode().getFile());
        verify(libraryImporter, never()).importAudioData(any(), any());
        verify(libraryImporter, never()).importArtwork(any());

        assertThat(observer.size()).isEqualTo(2);
        observer.assertThatAt(0, 1, 2);
        observer.assertThatAt(1, 2, 2);
    }

    @Test
    public void shouldNotFailReadAndImportOnObserverException() throws IOException {

        AudioNode audioNode = audioNode();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(new Plan(ImmutableList.of(audioNode), emptyList()));
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());

        batchLibraryImporter.readAndImport(ImmutableList.of(audioNode), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldNotFailWriteAndImportOnObserverException() throws IOException {

        WriteAndImportCommand command = new WriteAndImportCommand(audioNode(), new WritableAudioData());
        when(audioTagger.write(any(), any())).thenReturn(readableAudioData());
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());

        batchLibraryImporter.writeAndImport(ImmutableList.of(command), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    private AudioNode audioNode() throws IOException {
        AudioNode audioNode = mock(AudioNode.class);
        lenient().when(audioNode.getFile()).thenReturn(new File("someFile"));
        lenient().when(audioNode.getAudioData()).thenReturn(mock(ReadableAudioData.class));
        return audioNode;
    }

    private AudioNode audioNodeThrowing() throws IOException {
        AudioNode audioNode = mock(AudioNode.class);
        lenient().when(audioNode.getFile()).thenReturn(new File("someFile"));
        lenient().when(audioNode.getAudioData()).thenThrow(new IOException());
        return audioNode;
    }
}