package net.dorokhov.pony.library.service.impl.scan;

import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.impl.audio.AudioTagger;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.scan.BatchLibraryImportPlanner.Plan;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.*;
import static net.dorokhov.pony.library.LibraryConfig.LIBRARY_IMPORT_EXECUTOR;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class BatchLibraryImporter {

    public static class ImportResult {

        private final List<Song> importedSongs;
        private final List<File> failedFiles;

        public ImportResult(List<Song> importedSongs, List<File> failedFiles) {
            this.importedSongs = unmodifiableList(importedSongs);
            this.failedFiles = unmodifiableList(failedFiles);
        }

        public List<Song> getImportedSongs() {
            return importedSongs;
        }

        public List<File> getFailedFiles() {
            return failedFiles;
        }
    }

    public static class WriteAndImportCommand {

        private final AudioNode audioNode;
        private final WritableAudioData audioData;

        public WriteAndImportCommand(AudioNode audioNode, WritableAudioData audioData) {
            this.audioNode = checkNotNull(audioNode);
            this.audioData = checkNotNull(audioData);
        }

        public AudioNode getAudioNode() {
            return audioNode;
        }

        public WritableAudioData getAudioData() {
            return audioData;
        }
    }
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final BatchLibraryImportPlanner batchLibraryImportPlanner;
    private final AudioTagger audioTagger;
    private final LibraryImporter libraryImporter;
    private final LogService logService;
    private final Executor executor;
    
    private final TransactionTemplate transactionTemplate;
    
    private final Object progressLock = new Object();

    public BatchLibraryImporter(BatchLibraryImportPlanner batchLibraryImportPlanner,
                                AudioTagger audioTagger,
                                LibraryImporter libraryImporter,
                                LogService logService,
                                @Qualifier(LIBRARY_IMPORT_EXECUTOR) Executor executor,
                                PlatformTransactionManager transactionManager) {
        
        this.batchLibraryImportPlanner = batchLibraryImportPlanner;
        this.audioTagger = audioTagger;
        this.libraryImporter = libraryImporter;
        this.logService = logService;
        this.executor = executor;

        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public ImportResult readAndImport(List<AudioNode> audioNodes, ProgressObserver observer) {

        Plan plan = batchLibraryImportPlanner.plan(audioNodes);

        List<ImportTask> importTasks = new ArrayList<>();
        List<File> failedFiles = synchronizedList(new ArrayList<>());

        int itemsTotal = plan.getAudioNodesToImport().size();
        CountDownLatch latch = new CountDownLatch(itemsTotal);
        for (AudioNode audioNode : plan.getAudioNodesToImport()) {
            executor.execute(() -> {
                try {
                    importTasks.add(new ImportTask(audioNode, audioTagger.read(audioNode.getFile())));
                } catch (IOException e) {
                    logService.error(logger, "Could not read audio data from '{}'.",
                            audioNode.getFile().getAbsolutePath(), e);
                    failedFiles.add(audioNode.getFile());
                } finally {
                    synchronized (progressLock) {
                        latch.countDown();
                        notifyObserver(observer, itemsTotal - latch.getCount(), itemsTotal);
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn("Batch audio data reading has been interrupted.", e);
            throw new RuntimeException("Batch audio data writing has been interrupted.", e);
        }
        
        return doImport(importTasks, plan.getAudioNodesToSkip(), failedFiles);
    }

    public ImportResult writeAndImport(List<WriteAndImportCommand> commands, ProgressObserver observer) {

        List<ImportTask> importTasks = new ArrayList<>();
        List<File> failedFiles = synchronizedList(new ArrayList<>());
        
        int itemsTotal = commands.size();
        CountDownLatch latch = new CountDownLatch(itemsTotal);
        for (WriteAndImportCommand command : commands) {
            executor.execute(() -> {
                try {
                    importTasks.add(new ImportTask(command.getAudioNode(),
                            audioTagger.write(command.getAudioNode().getFile(), command.getAudioData())));
                } catch (IOException e) {
                    logService.error(logger, "Could not write audio data to '{}': '{}'.",
                            command.getAudioNode().getFile().getAbsolutePath(), command.getAudioData(), e);
                    failedFiles.add(command.getAudioNode().getFile());
                } finally {
                    synchronized (progressLock) {
                        latch.countDown();
                        notifyObserver(observer, itemsTotal - latch.getCount(), itemsTotal);
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn("Batch audio data writing has been interrupted.", e);
            throw new RuntimeException("Batch audio data writing has been interrupted.", e);
        }
        
        return doImport(importTasks, emptyList(), failedFiles);
    }

    private void notifyObserver(ProgressObserver observer, long itemsComplete, long itemsTotal) {
        try {
            observer.onProgress(itemsComplete, itemsTotal);
        } catch (Exception e) {
            logger.error("Could not call progress observer {}.", observer, e);
        }
    }
    
    private ImportResult doImport(List<ImportTask> importAudioDataTasks, 
                                  List<AudioNode> importArtworkTasks, 
                                  List<File> failedFiles) {
        return transactionTemplate.execute(status -> {
            List<Song> importedSongs = new ArrayList<>();
            for (ImportTask importTask : importAudioDataTasks) {
                importedSongs.add(libraryImporter.importAudioData(importTask.getAudioNode(), importTask.getAudioData()));
            }
            for (AudioNode audioNode : importArtworkTasks) {
                importedSongs.add(libraryImporter.importArtwork(audioNode));
            }
            return new ImportResult(importedSongs, failedFiles);
        });
    }

    private static class ImportTask {

        private final AudioNode audioNode;
        private final ReadableAudioData audioData;

        public ImportTask(AudioNode audioNode, ReadableAudioData audioData) {
            this.audioNode = checkNotNull(audioNode);
            this.audioData = checkNotNull(audioData);
        }

        public AudioNode getAudioNode() {
            return audioNode;
        }

        public ReadableAudioData getAudioData() {
            return audioData;
        }
    }
}
