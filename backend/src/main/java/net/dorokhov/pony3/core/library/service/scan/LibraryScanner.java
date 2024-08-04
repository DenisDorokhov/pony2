package net.dorokhov.pony3.core.library.service.scan;

import com.google.common.collect.Lists;
import jakarta.annotation.Nullable;
import net.dorokhov.pony3.api.library.domain.ScanProgress;
import net.dorokhov.pony3.api.library.domain.ScanProgress.Step;
import net.dorokhov.pony3.api.library.domain.ScanResult;
import net.dorokhov.pony3.api.library.domain.ScanType;
import net.dorokhov.pony3.api.library.service.command.EditCommand;
import net.dorokhov.pony3.api.log.service.LogService;
import net.dorokhov.pony3.core.library.service.filetree.FileTreeScanner;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.FileNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony3.core.library.service.scan.BatchLibraryImporter.WriteAndImportCommand;
import net.dorokhov.pony3.core.library.service.scan.ScanResultCalculator.AudioFileProcessingResult;
import net.dorokhov.pony3.core.library.service.scan.exception.SongNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;
import static net.dorokhov.pony3.api.library.domain.ScanProgress.Step.*;
import static net.dorokhov.pony3.common.RethrowingLambdas.rethrow;

@Component
public class LibraryScanner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LogService logService;
    private final FileTreeScanner fileTreeScanner;
    private final ScanResultCalculator scanResultCalculator;
    private final BatchLibraryCleaner batchLibraryCleaner;
    private final BatchLibraryImporter batchLibraryImporter;
    private final BatchLibraryArtworkFinder batchLibraryArtworkFinder;
    private final int importChunkSize;

    public LibraryScanner(
            LogService logService,
            FileTreeScanner fileTreeScanner,
            ScanResultCalculator scanResultCalculator,
            BatchLibraryCleaner batchLibraryCleaner,
            BatchLibraryImporter batchLibraryImporter,
            BatchLibraryArtworkFinder batchLibraryArtworkFinder,
            @Value("${pony.scan.importChunkSize}") int importChunkSize
    ) {
        this.logService = logService;
        this.fileTreeScanner = fileTreeScanner;
        this.scanResultCalculator = scanResultCalculator;
        this.batchLibraryCleaner = batchLibraryCleaner;
        this.batchLibraryImporter = batchLibraryImporter;
        this.batchLibraryArtworkFinder = batchLibraryArtworkFinder;
        this.importChunkSize = importChunkSize;
    }

    public ScanResult scan(List<File> targetFolders, @Nullable Consumer<ScanProgress> observer) throws IOException {

        for (File folder : targetFolders) {
            if (!folder.exists()) {
                throw new FileNotFoundException(folder.getAbsolutePath());
            }
            if (!folder.isDirectory()) {
                throw new IOException(String.format("The supplied file '%s' is not a directory.", folder.getAbsolutePath()));
            }
        }

        try {
            logService.info(logger, "Scanning library {}...", targetFolders);
            notifyProgressObserver(new ScanProgress(FULL_PREPARING, targetFolders, null), observer);
            ScanResult scanResult = doScan(targetFolders, observer);
            logService.info(logger, "Scan of {} has been finished with result '{}'.", targetFolders, scanResult);
            return scanResult;
        } catch (Exception e) {
            logService.error(logger, "Scan failed.", e);
            throw e;
        }
    }

    public ScanResult edit(List<EditCommand> commands, List<File> targetFolders, @Nullable Consumer<ScanProgress> observer) throws SongNotFoundException, IOException {

        List<File> targetFiles = new ArrayList<>();
        List<WriteAndImportCommand> writeAndImportCommands = new ArrayList<>();
        for (EditCommand command : commands) {
            File songFile = new File(command.getSongFilePath());
            if (!songFile.exists()) {
                throw new FileNotFoundException(songFile.getAbsolutePath());
            }
            FileNode fileNode = fileTreeScanner.scanFile(songFile, targetFolders);
            if (fileNode instanceof AudioNode) {
                targetFiles.add(songFile);
                writeAndImportCommands.add(new WriteAndImportCommand((AudioNode) fileNode, command.getAudioData()));
            } else {
                throw new IOException(String.format("Audio file '%s' could not be resolved.", songFile.getAbsolutePath()));
            }
        }

        try {
            logService.info(logger, "Editing files {}...", targetFiles);
            notifyProgressObserver(new ScanProgress(EDIT_PREPARING, targetFiles, null), observer);
            ScanResult scanResult = doEdit(writeAndImportCommands, observer);
            logService.info(logger, "Edit of {} has been finished with result '{}'.", targetFiles, scanResult);
            return scanResult;
        } catch (Exception e) {
            logService.error(logger, "Edit failed.", e);
            throw e;
        }
    }

    private ScanResult doScan(List<File> targetFolders, @Nullable Consumer<ScanProgress> observer) {
        return scanResultCalculator.calculateAndSave(rethrow(() -> performScanSteps(targetFolders, observer)));
    }

    private ScanResult doEdit(List<WriteAndImportCommand> commands, @Nullable Consumer<ScanProgress> observer) {
        return scanResultCalculator.calculateAndSave(rethrow(() -> performEditSteps(commands, observer)));
    }

    private AudioFileProcessingResult performScanSteps(List<File> targetFolders, @Nullable Consumer<ScanProgress> observer) throws IOException {

        logService.info(logger, "Searching media files...");
        progressScan(FULL_SEARCHING_MEDIA, targetFolders, null, observer);
        List<AudioNode> audioNodes = new ArrayList<>();
        List<ImageNode> imageNodes = new ArrayList<>();
        for (File file : targetFolders) {
            FolderNode folderNode = fileTreeScanner.scanFolder(file);
            audioNodes.addAll(folderNode.getNotIgnoredChildAudiosRecursively());
            imageNodes.addAll(folderNode.getNotIgnoredChildImagesRecursively());
        }
        audioNodes.sort(Comparator.comparing(audioNode -> audioNode.getFile().getAbsolutePath()));
        imageNodes.sort(Comparator.comparing(imageNode -> imageNode.getFile().getAbsolutePath()));

        logService.info(logger, "Cleaning songs...");
        progressScan(FULL_CLEANING_SONGS, targetFolders, null, observer);
        batchLibraryCleaner.cleanSongs(audioNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_SONGS, targetFolders,
                        ScanProgress.Value.of(itemsComplete, itemsTotal), observer));

        logService.info(logger, "Cleaning artworks...");
        progressScan(FULL_CLEANING_ARTWORKS, targetFolders, null, observer);
        batchLibraryCleaner.cleanArtworks(imageNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_ARTWORKS, targetFolders,
                        ScanProgress.Value.of(itemsComplete, itemsTotal), observer));

        logService.info(logger, "Importing songs...");
        progressScan(FULL_IMPORTING, targetFolders, null, observer);
        int processedCount = 0;
        List<File> failedFiles = new ArrayList<>();
        for (List<AudioNode> chunk : Lists.partition(audioNodes, importChunkSize)) {
            int finalProcessedCount = processedCount;
            BatchLibraryImporter.ImportResult result = batchLibraryImporter.readAndImport(chunk, (itemsComplete, itemsTotal) ->
                    progressScan(FULL_IMPORTING, targetFolders,
                            ScanProgress.Value.of(finalProcessedCount + itemsComplete, audioNodes.size()), observer));
            failedFiles.addAll(result.getFailedFiles());
            processedCount += chunk.size();
        }

        logService.info(logger, "Searching artworks...");
        progressScan(FULL_SEARCHING_ARTWORKS, targetFolders, null, observer);
        batchLibraryArtworkFinder.findAllArtworks((itemsComplete, itemsTotal) ->
                progressScan(FULL_SEARCHING_ARTWORKS, targetFolders,
                        ScanProgress.Value.of(itemsComplete, itemsTotal), observer));

        return new AudioFileProcessingResultImpl(ScanType.FULL, failedFiles, audioNodes.size());
    }

    private AudioFileProcessingResult performEditSteps(List<WriteAndImportCommand> commands, @Nullable Consumer<ScanProgress> observer) {

        List<File> targetFiles = commands.stream()
                .map(WriteAndImportCommand::getAudioNode)
                .map(AudioNode::getFile)
                .toList();

        logService.info(logger, "Writing songs...");
        progressScan(EDIT_WRITING, targetFiles, null, observer);
        int processedCount = 0;
        List<File> failedFiles = new ArrayList<>();
        for (List<WriteAndImportCommand> chunk : Lists.partition(commands, importChunkSize)) {
            int finalProcessedCount = processedCount;
            BatchLibraryImporter.ImportResult result = batchLibraryImporter.writeAndImport(chunk, (itemsComplete, itemsTotal) ->
                    progressScan(EDIT_WRITING, targetFiles,
                            ScanProgress.Value.of(finalProcessedCount + itemsComplete, commands.size()), observer));
            failedFiles.addAll(result.getFailedFiles());
            processedCount += chunk.size();
        }

        logService.info(logger, "Searching artworks...");
        progressScan(EDIT_SEARCHING_ARTWORKS, targetFiles, null, observer);
        batchLibraryArtworkFinder.findAllArtworks((itemsComplete, itemsTotal) ->
                progressScan(EDIT_SEARCHING_ARTWORKS, targetFiles,
                        ScanProgress.Value.of(itemsComplete, itemsTotal), observer));

        return new AudioFileProcessingResultImpl(ScanType.EDIT, failedFiles, commands.size());
    }

    private void progressScan(Step step, List<File> files, ScanProgress.Value progressValue, @Nullable Consumer<ScanProgress> observer) {
        notifyProgressObserver(new ScanProgress(step, files, progressValue), observer);
    }

    private void notifyProgressObserver(ScanProgress scanProgress, @Nullable Consumer<ScanProgress> handler) {
        if (handler != null) {
            try {
                handler.accept(scanProgress);
            } catch (Exception e) {
                logger.error("Could not call progress observer.", e);
            }
        }
    }

    private static class AudioFileProcessingResultImpl implements AudioFileProcessingResult {

        private final ScanType scanType;
        private final List<File> failedFiles;
        private final int processedAudioFileCount;

        public AudioFileProcessingResultImpl(
                ScanType scanType,
                List<File> failedFiles,
                int processedAudioFileCount
        ) {
            this.scanType = checkNotNull(scanType);
            this.failedFiles = unmodifiableList(failedFiles);
            this.processedAudioFileCount = processedAudioFileCount;
        }

        @Override
        public ScanType getScanType() {
            return scanType;
        }

        @Override
        public List<File> getFailedFiles() {
            return failedFiles;
        }

        @Override
        public int getProcessedAudioFileCount() {
            return processedAudioFileCount;
        }
    }
}
