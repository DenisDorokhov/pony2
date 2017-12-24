package net.dorokhov.pony.library.service.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.fixture.ProgressObserverFixture;
import net.dorokhov.pony.library.service.AudioTagger;
import net.dorokhov.pony.api.library.domain.WritableAudioData;
import net.dorokhov.pony.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.scan.BatchLibraryImportPlanner.Plan;
import net.dorokhov.pony.library.service.scan.BatchLibraryImporter.ImportResult;
import net.dorokhov.pony.library.service.scan.BatchLibraryImporter.WriteAndImportCommand;
import net.dorokhov.pony.api.log.service.LogService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import static java.util.Collections.emptyList;
import static net.dorokhov.pony.fixture.PlatformTransactionManagerFixtures.transactionManager;
import static net.dorokhov.pony.fixture.ReadableAudioDataFixtures.readableAudioData;
import static net.dorokhov.pony.fixture.SongFixtures.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
    @SuppressWarnings("unused")
    private LogService logService;

    @Spy
    @SuppressWarnings("unused")
    private final Executor executor = new SyncTaskExecutor();
    @Spy
    @SuppressWarnings("unused")
    private final PlatformTransactionManager transactionManager = transactionManager();

    @Test
    public void shouldReadAndImport() throws Exception {
        
        AudioNode audioNodeToImport1 = audioNode();
        AudioNode audioNodeToImport2 = audioNode();
        AudioNode audioNodeToSkip1 = audioNode();
        AudioNode audioNodeToSkip2 = audioNode();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(new Plan(
                ImmutableList.of(audioNodeToImport1, audioNodeToImport2), 
                ImmutableList.of(audioNodeToSkip1, audioNodeToSkip2)
        ));
        when(audioTagger.read(any())).thenReturn(readableAudioData());
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
    public void shouldNotImportWhenReadingFailed() throws Exception {
        
        AudioNode audioNodeToImport1 = audioNode();
        AudioNode audioNodeToImport2 = audioNode();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(
                new Plan(ImmutableList.of(audioNodeToImport1, audioNodeToImport2), emptyList()));
        when(audioTagger.read(any())).thenThrow(new IOException());

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
    public void shouldWriteAndImport() throws Exception {

        WriteAndImportCommand command1 = new WriteAndImportCommand(audioNode(), writableAudioData());
        WriteAndImportCommand command2 = new WriteAndImportCommand(audioNode(), writableAudioData());
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
    public void shouldNotImportWhenWritingFailed() throws Exception {

        WriteAndImportCommand command1 = new WriteAndImportCommand(audioNode(), writableAudioData());
        WriteAndImportCommand command2 = new WriteAndImportCommand(audioNode(), writableAudioData());
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
    public void shouldNotFailReadAndImportOnObserverException() throws Exception {

        AudioNode audioNode = audioNode();
        when(batchLibraryImportPlanner.plan(any())).thenReturn(new Plan(ImmutableList.of(audioNode), emptyList()));
        when(audioTagger.read(any())).thenReturn(readableAudioData());
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());

        batchLibraryImporter.readAndImport(ImmutableList.of(audioNode), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    @Test
    public void shouldNotFailWriteAndImportOnObserverException() throws Exception {

        WriteAndImportCommand command = new WriteAndImportCommand(audioNode(), writableAudioData());
        when(audioTagger.write(any(), any())).thenReturn(readableAudioData());
        when(libraryImporter.importAudioData(any(), any())).thenReturn(song());

        batchLibraryImporter.writeAndImport(ImmutableList.of(command), (itemsComplete, itemsTotal) -> {
            throw new RuntimeException();
        });
    }

    private AudioNode audioNode() {
        AudioNode audioNode = mock(AudioNode.class);
        when(audioNode.getFile()).thenReturn(new File("someFile"));
        return audioNode;
    }
    
    private WritableAudioData writableAudioData() {
        return WritableAudioData.builder().build();
    }
}