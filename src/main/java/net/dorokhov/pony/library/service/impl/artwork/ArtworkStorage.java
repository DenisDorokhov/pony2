package net.dorokhov.pony.library.service.impl.artwork;

import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.repository.ArtworkRepository;
import net.dorokhov.pony.library.service.impl.artwork.command.ByteSourceArtworkCommand;
import net.dorokhov.pony.library.service.impl.artwork.command.FileArtworkCommand;
import net.dorokhov.pony.library.service.impl.artwork.command.ImageNodeArtworkCommand;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import net.dorokhov.pony.library.service.impl.image.ThumbnailGenerator;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Component
public class ArtworkStorage {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final ArtworkRepository artworkRepository;
    private final FileTypeResolver fileTypeResolver;
    private final ChecksumCalculator checksumCalculator;
    private final ThumbnailGenerator thumbnailGenerator;
    
    private final File artworkFolder;
    private final ImageSize artworkSizeSmall;
    private final ImageSize artworkSizeLarge;
    
    private final Object modificationLock = new Object();

    public ArtworkStorage(ArtworkRepository artworkRepository,
                          FileTypeResolver fileTypeResolver,
                          ChecksumCalculator checksumCalculator,
                          ThumbnailGenerator thumbnailGenerator,
                          @Value("${pony.artwork.path}") File artworkFolder,
                          @Value("${pony.artwork.size.small}") int[] artworkSizeSmall,
                          @Value("${pony.artwork.size.large}") int[] artworkSizeLarge) {
        this.artworkRepository = artworkRepository;
        this.fileTypeResolver = fileTypeResolver;
        this.checksumCalculator = checksumCalculator;
        this.thumbnailGenerator = thumbnailGenerator;
        this.artworkFolder = artworkFolder;
        this.artworkSizeSmall = ImageSize.of(artworkSizeSmall[0], artworkSizeSmall[1]);
        this.artworkSizeLarge = ImageSize.of(artworkSizeLarge[0], artworkSizeLarge[1]);
    }

    @Transactional(readOnly = true)
    @Nullable
    public File getLargeImageFile(Long artworkId) {
        Artwork artwork = artworkRepository.findOne(artworkId);
        if (artwork == null) {
            return null;
        }
        return new File(artworkFolder, artwork.getLargeImagePath());
    }

    @Transactional(readOnly = true)
    @Nullable
    public File getSmallImageFile(Long artworkId) {
        Artwork artwork = artworkRepository.findOne(artworkId);
        if (artwork == null) {
            return null;
        }
        return new File(artworkFolder, artwork.getSmallImagePath());
    }

    @Transactional
    public Artwork getOrSave(ByteSourceArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            byte[] content = command.getByteSource().read();
            return doGetOrSave(command.getSourceUri(), 
                    () -> checksumCalculator.calculate(content), 
                    () -> fileTypeResolver.resolve(content),
                    () -> new ByteArrayInputStream(content));
        }
    }

    @Transactional
    public Artwork getOrSave(FileArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            File file = command.getFile();
            return doGetOrSave(command.getSourceUri(),
                    rethrow(() -> checksumCalculator.calculate(file)),
                    rethrow(() -> fileTypeResolver.resolve(file)),
                    rethrow(() -> new FileInputStream(file)));
        }
    }

    @Transactional
    public Artwork getOrSave(ImageNodeArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            return doGetOrSave(command.getSourceUri(),
                    rethrow(() -> command.getImageNode().getChecksum()),
                    rethrow(() -> command.getImageNode().getFileType()),
                    rethrow(() -> new FileInputStream(command.getImageNode().getFile())));
        }
    }

    @Transactional
    public void delete(Long id) {
        Artwork artwork = artworkRepository.findOne(id);
        if (artwork != null) {
            File largeFile = new File(artworkFolder, artwork.getLargeImagePath());
            File smallFile = new File(artworkFolder, artwork.getSmallImagePath());
            artworkRepository.delete(artwork);
            registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    if (!largeFile.delete()) {
                        logger.warn("Could not delete artwork large image file: '{}'.", largeFile.getAbsolutePath());
                    }
                    if (!smallFile.delete()) {
                        logger.warn("Could not delete artwork small image file: '{}'.", smallFile.getAbsolutePath());
                    }
                }
            });
        }
    }
    
    private Artwork doGetOrSave(URI sourceUri,
                                Supplier<String> checksumSupplier, 
                                Supplier<FileType> fileTypeSupplier,
                                Supplier<InputStream> streamSupplier) throws IOException {

        String checksum = checksumSupplier.get();
        Artwork artwork = artworkRepository.findByChecksum(checksum);
        if (artwork != null) {
            return artwork;
        }

        FileType fileType = fileTypeSupplier.get();

        String uuid = UUID.randomUUID().toString();
        String smallImagePath = buildImagePath(uuid, "small", fileType.getFileExtension());
        String largeImagePath = buildImagePath(uuid, "large", fileType.getFileExtension());

        File smallImageFile = new File(artworkFolder, smallImagePath);
        File largeImageFile = new File(artworkFolder, largeImagePath);
        thumbnailGenerator.generateThumbnail(streamSupplier.get(), artworkSizeSmall, smallImageFile);
        thumbnailGenerator.generateThumbnail(streamSupplier.get(), artworkSizeLarge, largeImageFile);

        artwork = artworkRepository.save(Artwork.builder()
                .mimeType(fileType.getMimeType())
                .checksum(checksum)
                .sourceUri(sourceUri)
                .smallImagePath(smallImagePath)
                .largeImagePath(largeImagePath)
                .smallImageSize(smallImageFile.length())
                .largeImageSize(largeImageFile.length())
                .build());

        registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    if (!smallImageFile.delete()) {
                        logger.error("Could not delete small image file after rollback: '{}'.", smallImageFile.getAbsolutePath());
                    }
                    if (!largeImageFile.delete()) {
                        logger.error("Could not delete large image file after rollback: '{}'.", largeImageFile.getAbsolutePath());
                    }
                }
            }
        });

        return artwork;
    }
    
    private String buildImagePath(String name, String suffix, String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append(name.substring(0, 2)).append("/");
        builder.append(name.substring(2, 4)).append("/");
        builder.append(name).append(".").append(suffix).append(".").append(extension);
        return builder.toString();
    }
}
