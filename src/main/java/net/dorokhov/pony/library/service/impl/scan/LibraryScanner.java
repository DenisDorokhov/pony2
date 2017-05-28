package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.dorokhov.pony.library.domain.ScanProgress;
import net.dorokhov.pony.library.domain.ScanProgress.Step;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.filetree.FileTreeScanner;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FileNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FolderNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.impl.scan.LibraryImporter.WriteAndImportCommand;
import net.dorokhov.pony.library.service.impl.scan.ScanResultCalculator.AudioFileProcessingResult;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony.library.domain.ScanProgress.Step.*;

@Component
public class LibraryScanner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LogService logService;
    private final SongRepository songRepository;
    private final FileTreeScanner fileTreeScanner;
    private final ScanResultCalculator scanResultCalculator;
    private final LibraryCleaner libraryCleaner;
    private final LibraryImporter libraryImporter;
    private final LibraryArtworkFinder libraryArtworkFinder;
    private final int importChunkSize;

    public LibraryScanner(LogService logService,
                          SongRepository songRepository,
                          FileTreeScanner fileTreeScanner,
                          ScanResultCalculator scanResultCalculator,
                          LibraryCleaner libraryCleaner, LibraryImporter libraryImporter,
                          LibraryArtworkFinder libraryArtworkFinder,
                          @Value("${pony.scan.importChunkSize}") int importChunkSize) {
        this.logService = logService;
        this.songRepository = songRepository;
        this.fileTreeScanner = fileTreeScanner;
        this.scanResultCalculator = scanResultCalculator;
        this.libraryCleaner = libraryCleaner;
        this.libraryImporter = libraryImporter;
        this.libraryArtworkFinder = libraryArtworkFinder;
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
            notifyProgressObserver(new ScanProgress(FULL_PREPARING, targetFolders, 0.0), observer);
            ScanResult scanResult = doScan(targetFolders, observer);
            logService.info(logger, "Scan of {} has been finished with result {}.", targetFolders, scanResult);
            return scanResult;
        } catch (Exception e) {
            logService.error(logger, "Scan failed.", e);
            throw e;
        }
    }

    public ScanResult edit(List<EditCommand> commands, @Nullable Consumer<ScanProgress> observer) throws SongNotFoundException, IOException {

        List<File> targetFiles = new ArrayList<>();
        List<WriteAndImportCommand> writeAndImportCommands = new ArrayList<>();
        for (EditCommand command : commands) {
            Song song = songRepository.findOne(command.getSongId());
            if (song == null) {
                throw new SongNotFoundException(command.getSongId());
            }
            File songFile = song.getFile();
            if (!songFile.exists()) {
                throw new FileNotFoundException(songFile.getAbsolutePath());
            }
            FileNode fileNode = fileTreeScanner.scanFile(songFile);
            if (fileNode instanceof AudioNode) {
                targetFiles.add(songFile);
                writeAndImportCommands.add(new WriteAndImportCommand((AudioNode) fileNode, command.getAudioData()));
            } else {
                throw new IOException(String.format("File '%s' is not a song.", songFile.getAbsolutePath()));
            }
        }

        try {
            logService.info(logger, "Editing files {}...", targetFiles);
            notifyProgressObserver(new ScanProgress(EDIT_PREPARING, targetFiles, 0.0), observer);
            ScanResult scanResult = doEdit(writeAndImportCommands, observer);
            logService.info(logger, "Edit of {} has been finished with result {}.", targetFiles, scanResult);
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
        progressScan(FULL_SEARCHING_MEDIA, targetFolders, 0.0, observer);
        List<AudioNode> audioNodes = new ArrayList<>();
        List<ImageNode> imageNodes = new ArrayList<>();
        for (File file : targetFolders) {
            FolderNode folderNode = fileTreeScanner.scanFolder(file);
            audioNodes.addAll(folderNode.getChildAudios(true));
            imageNodes.addAll(folderNode.getChildImages(true));
        }

        logService.info(logger, "Cleaning songs...");
        progressScan(FULL_CLEANING_SONGS, targetFolders, 0.0, observer);
        libraryCleaner.cleanSongs(audioNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_SONGS, targetFolders, (double) itemsComplete / itemsTotal, observer));

        logService.info(logger, "Cleaning artworks...");
        progressScan(FULL_CLEANING_ARTWORKS, targetFolders, 0.0, observer);
        libraryCleaner.cleanArtworks(imageNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_ARTWORKS, targetFolders, (double) itemsComplete / itemsTotal, observer));

        logService.info(logger, "Importing songs...");
        progressScan(FULL_IMPORTING, targetFolders, 0.0, observer);
        int processedCount = 0;
        List<File> failedFiles = new ArrayList<>();
        for (List<AudioNode> chunk : Lists.partition(audioNodes, importChunkSize)) {
            int finalProcessedCount = processedCount;
            LibraryImporter.ImportResult result = libraryImporter.readAndImport(chunk, (itemsComplete, itemsTotal) ->
                    progressScan(FULL_IMPORTING, targetFolders, (double) (finalProcessedCount + itemsComplete) / audioNodes.size(), observer));
            failedFiles.addAll(result.getFailedImports().stream()
                    .map(AudioNode::getFile)
                    .collect(Collectors.toList()));
            processedCount += chunk.size();
        }

        logService.info(logger, "Searching artworks...");
        progressScan(FULL_SEARCHING_ARTWORKS, targetFolders, 0.0, observer);
        libraryArtworkFinder.findArtworks((itemsComplete, itemsTotal) ->
                progressScan(FULL_SEARCHING_ARTWORKS, targetFolders, (double) itemsComplete / itemsTotal, observer));

        return new AudioFileProcessingResultImpl(ScanType.FULL, targetFolders, failedFiles, audioNodes.size());
    }

    private AudioFileProcessingResult performEditSteps(List<WriteAndImportCommand> commands, @Nullable Consumer<ScanProgress> observer) {

        List<File> targetFiles = commands.stream()
                .map(WriteAndImportCommand::getAudioNode)
                .map(AudioNode::getFile)
                .collect(Collectors.toList());

        logService.info(logger, "Writing songs...");
        progressScan(EDIT_WRITING, targetFiles, 0.0, observer);
        int processedCount = 0;
        List<File> failedFiles = new ArrayList<>();
        for (List<WriteAndImportCommand> chunk : Lists.partition(commands, importChunkSize)) {
            int finalProcessedCount = processedCount;
            LibraryImporter.ImportResult result = libraryImporter.writeAndImport(chunk, (itemsComplete, itemsTotal) ->
                    progressScan(EDIT_WRITING, targetFiles, (double) (finalProcessedCount + itemsComplete) / commands.size(), observer));
            failedFiles.addAll(result.getFailedImports().stream()
                    .map(AudioNode::getFile)
                    .collect(Collectors.toList()));
            processedCount += chunk.size();
        }

        logService.info(logger, "Searching artworks...");
        progressScan(EDIT_SEARCHING_ARTWORKS, targetFiles, 0.0, observer);
        libraryArtworkFinder.findArtworks((itemsComplete, itemsTotal) ->
                progressScan(EDIT_SEARCHING_ARTWORKS, targetFiles, (double) itemsComplete / itemsTotal, observer));

        return new AudioFileProcessingResultImpl(ScanType.EDIT, targetFiles, failedFiles, commands.size());
    }

    private void progressScan(Step step, List<File> files, double progressValue, @Nullable Consumer<ScanProgress> observer) {
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
        private final List<File> targetFiles;
        private final List<File> failedFiles;
        private final int processedAudioFileCount;

        public AudioFileProcessingResultImpl(ScanType scanType,
                                             List<File> targetFiles,
                                             List<File> failedFiles,
                                             int processedAudioFileCount) {
            this.scanType = checkNotNull(scanType);
            this.targetFiles = ImmutableList.copyOf(targetFiles);
            this.failedFiles = ImmutableList.copyOf(failedFiles);
            this.processedAudioFileCount = processedAudioFileCount;
        }

        @Override
        public ScanType getScanType() {
            return scanType;
        }

        @Override
        public List<File> getTargetFiles() {
            return targetFiles;
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
