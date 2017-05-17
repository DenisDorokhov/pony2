package net.dorokhov.pony.library.service.impl.scan;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.domain.ScanResult;
import net.dorokhov.pony.library.domain.ScanStatus;
import net.dorokhov.pony.library.domain.ScanStatus.Progress.Step;
import net.dorokhov.pony.library.domain.ScanType;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.repository.SongRepository;
import net.dorokhov.pony.library.service.command.EditCommand;
import net.dorokhov.pony.library.service.exception.ConcurrentScanException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;
import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;
import net.dorokhov.pony.library.service.impl.filetree.FileTreeScanner;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FileNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FolderNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.impl.scan.ScanResultCalculator.AudioFileProcessingResult;
import net.dorokhov.pony.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony.library.domain.ScanStatus.Progress.Step.*;
import static net.dorokhov.pony.library.service.impl.LibraryConfig.SCAN_EXECUTOR;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

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
    private final Executor executor;

    private final Lock scanLock = new ReentrantLock();

    private final AtomicReference<ScanStatus> scanStatusReference = new AtomicReference<>();

    private final Set<ScanProgressObserver> progressObservers = Collections.synchronizedSet(new LinkedHashSet<ScanProgressObserver>());

    public LibraryScanner(LogService logService,
                          SongRepository songRepository,
                          FileTreeScanner fileTreeScanner,
                          ScanResultCalculator scanResultCalculator,
                          LibraryCleaner libraryCleaner, LibraryImporter libraryImporter,
                          LibraryArtworkFinder libraryArtworkFinder,
                          @Qualifier(SCAN_EXECUTOR) Executor executor) {
        this.logService = logService;
        this.songRepository = songRepository;
        this.fileTreeScanner = fileTreeScanner;
        this.scanResultCalculator = scanResultCalculator;
        this.libraryCleaner = libraryCleaner;
        this.libraryImporter = libraryImporter;
        this.libraryArtworkFinder = libraryArtworkFinder;
        this.executor = executor;
    }

    public void addProgressObserver(ScanProgressObserver scanProgressObserver) {
        progressObservers.add(checkNotNull(scanProgressObserver));
    }

    public void removeProgressObserver(ScanProgressObserver scanProgressObserver) {
        progressObservers.remove(checkNotNull(scanProgressObserver));
    }

    public ScanStatus getStatus() {
        ScanStatus scanStatus = scanStatusReference.get();
        return scanStatus != null ? scanStatus : new ScanStatus(false, null);
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public ScanResult scan(List<File> targetFolders) throws ConcurrentScanException, IOException {

        for (File folder : targetFolders) {
            if (!folder.exists()) {
                throw new FileNotFoundException(folder.getAbsolutePath());
            }
            if (!folder.isDirectory()) {
                throw new IOException(String.format("The supplied file '%s' is not a directory.", folder.getAbsolutePath()));
            }
        }

        if (!scanLock.tryLock()) {
            throw new ConcurrentScanException();
        }
        try {
            ScanStatus scanStatus = new ScanStatus(true, new ScanStatus.Progress(FULL_PREPARING, targetFolders, 0.0));
            scanStatusReference.set(scanStatus);
            logService.info(logger, "Scanning library {}...", targetFolders);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanStarted(scanStatus));
            ScanResult scanResult = doScan(targetFolders);
            logService.info(logger, "Scan of {} has been finished with result {}.", targetFolders, scanResult);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanComplete(scanStatusReference.get(), scanResult));
            return scanResult;
        } catch (Exception e) {
            logService.error(logger, "Scan failed.", e);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanFailed(scanStatusReference.get(), e));
            throw e;
        } finally {
            scanStatusReference.set(null);
            scanLock.unlock();
        }
    }

    @Transactional(propagation = NOT_SUPPORTED)
    public ScanResult edit(List<EditCommand> commands) throws SongNotFoundException, ConcurrentScanException, IOException {

        List<File> targetFiles = new ArrayList<>();
        List<AudioNodeEditCommand> editCommands = new ArrayList<>();
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
                editCommands.add(new AudioNodeEditCommand((AudioNode) fileNode, command.getAudioData()));
            } else {
                throw new IOException(String.format("File '%s' is not a song.", songFile.getAbsolutePath()));
            }
        }

        if (!scanLock.tryLock()) {
            throw new ConcurrentScanException();
        }
        try {
            ScanStatus scanStatus = new ScanStatus(true, new ScanStatus.Progress(EDIT_PREPARING, targetFiles, 0.0));
            scanStatusReference.set(scanStatus);
            logService.info(logger, "Editing files {}...", targetFiles);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanStarted(scanStatus));
            ScanResult scanResult = doEdit(editCommands);
            logService.info(logger, "Edit of {} has been finished with result {}.", targetFiles, scanResult);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanComplete(scanStatusReference.get(), scanResult));
            return scanResult;
        } catch (Exception e) {
            logService.error(logger, "Edit failed.", e);
            executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanFailed(scanStatusReference.get(), e));
            throw e;
        } finally {
            scanStatusReference.set(null);
            scanLock.unlock();
        }
    }

    private ScanResult doScan(List<File> targetFolders) {
        return scanResultCalculator.calculateAndSave(rethrow(() -> performScanSteps(targetFolders)));
    }

    private ScanResult doEdit(List<AudioNodeEditCommand> commands) {
        return scanResultCalculator.calculateAndSave(rethrow(() -> performEditSteps(commands)));
    }

    private AudioFileProcessingResult performScanSteps(List<File> targetFolders) throws IOException {

        logService.info(logger, "Searching media files...");
        progressScan(FULL_SEARCHING_MEDIA, 0.0);
        List<AudioNode> audioNodes = new ArrayList<>();
        List<ImageNode> imageNodes = new ArrayList<>();
        for (File file : targetFolders) {
            FolderNode folderNode = fileTreeScanner.scanFolder(file);
            audioNodes.addAll(folderNode.getChildAudios(true));
            imageNodes.addAll(folderNode.getChildImages(true));
        }

        logService.info(logger, "Cleaning songs...");
        progressScan(FULL_CLEANING_SONGS, 0.0);
        libraryCleaner.cleanSongs(audioNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_SONGS, (double) itemsComplete / itemsTotal));

        logService.info(logger, "Cleaning artworks...");
        progressScan(FULL_CLEANING_ARTWORKS, 0.0);
        libraryCleaner.cleanArtworks(imageNodes, (itemsComplete, itemsTotal) ->
                progressScan(FULL_CLEANING_ARTWORKS, (double) itemsComplete / itemsTotal));

        logService.info(logger, "Importing songs...");
        progressScan(FULL_IMPORTING, 0.0);
        AtomicInteger processedCount = new AtomicInteger();
        List<File> failedFiles = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(audioNodes.size());
        for (AudioNode audioNode : audioNodes) {
            executor.execute(() -> {
                try {
                    libraryImporter.importSong(audioNode);
                } catch (IOException e) {
                    logService.error(logger, "Could not import audio file '{}'.", audioNode.getFile().getAbsolutePath(), e);
                    failedFiles.add(audioNode.getFile());
                } finally {
                    progressScan(FULL_IMPORTING, (double) processedCount.incrementAndGet() / audioNodes.size());
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn("Scan has been interrupted.");
        }

        logService.info(logger, "Searching artworks...");
        progressScan(FULL_SEARCHING_ARTWORKS, 0.0);
        libraryArtworkFinder.findArtworks((itemsComplete, itemsTotal) ->
                progressScan(FULL_SEARCHING_ARTWORKS, (double) itemsComplete / itemsTotal));

        return new AudioFileProcessingResultImpl(ScanType.FULL, targetFolders, failedFiles, audioNodes.size());
    }

    private AudioFileProcessingResult performEditSteps(List<AudioNodeEditCommand> commands) {

        logService.info(logger, "Writing songs...");
        progressScan(EDIT_WRITING, 0.0);
        AtomicInteger processedCount = new AtomicInteger();
        List<File> failedFiles = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(commands.size());
        for (AudioNodeEditCommand command : commands) {
            executor.execute(() -> {
                try {
                    libraryImporter.writeAndImportSong(command.getAudioNode(), command.getAudioData());
                } catch (IOException e) {
                    logService.error(logger, "Could not edit audio file '{}'.", command.getAudioNode().getFile().getAbsolutePath(), e);
                    failedFiles.add(command.getAudioNode().getFile());
                } finally {
                    progressScan(EDIT_WRITING, (double) processedCount.incrementAndGet() / commands.size());
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn("Edit has been interrupted.");
        }

        logService.info(logger, "Searching artworks...");
        progressScan(EDIT_SEARCHING_ARTWORKS, 0.0);
        libraryArtworkFinder.findArtworks((itemsComplete, itemsTotal) ->
                progressScan(EDIT_SEARCHING_ARTWORKS, (double) itemsComplete / itemsTotal));

        return new AudioFileProcessingResultImpl(ScanType.EDIT,
                commands.stream()
                        .map(AudioNodeEditCommand::getAudioNode)
                        .map(AudioNode::getFile)
                        .collect(Collectors.toList()),
                failedFiles, commands.size());
    }

    @SuppressWarnings("ConstantConditions")
    private ScanStatus progressScan(Step step, double progressValue) {
        ScanStatus oldScanStatus = scanStatusReference.getAndUpdate(oldValue ->
                new ScanStatus(true,
                        new ScanStatus.Progress(
                                step,
                                oldValue.getProgress().getFiles(),
                                progressValue)));
        ScanStatus newScanStatus = scanStatusReference.get();
        executeOnProgressObservers(scanProgressObserver -> scanProgressObserver.onScanProgress(oldScanStatus, newScanStatus));
        return newScanStatus;
    }

    private void executeOnProgressObservers(Consumer<ScanProgressObserver> handler) {
        for (ScanProgressObserver observer : new ArrayList<>(progressObservers)) {
            try {
                handler.accept(observer);
            } catch (Exception e) {
                logger.error("Could not call progress observer {}.", observer, e);
            }
        }
    }

    private static final class AudioNodeEditCommand {

        private final AudioNode audioNode;
        private final WritableAudioData audioData;

        public AudioNodeEditCommand(AudioNode audioNode, WritableAudioData audioData) {
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
