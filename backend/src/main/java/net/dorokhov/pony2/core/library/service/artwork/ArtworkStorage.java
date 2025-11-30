package net.dorokhov.pony2.core.library.service.artwork;

import com.google.common.io.Files;
import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.core.library.repository.ArtworkRepository;
import net.dorokhov.pony2.core.library.service.artwork.command.ByteSourceArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.artwork.command.ImageNodeArtworkStorageCommand;
import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.image.ThumbnailGenerator;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

import java.io.*;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Component
public class ArtworkStorage {

    private static final String THUMBNAIL_FORMAT = "png";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtworkRepository artworkRepository;
    private final FileTypeResolver fileTypeResolver;
    private final ChecksumCalculator checksumCalculator;
    private final ThumbnailGenerator thumbnailGenerator;

    private final File artworkFolder;
    private final ImageSize artworkSizeSmall;
    private final ImageSize artworkSizeLarge;

    private final Object modificationLock = new Object();

    public ArtworkStorage(
            ArtworkRepository artworkRepository,
            FileTypeResolver fileTypeResolver,
            ChecksumCalculator checksumCalculator,
            ThumbnailGenerator thumbnailGenerator,
            @Value("${pony.artwork.path}") File artworkFolder,
            @Value("${pony.artwork.size.small}") int[] artworkSizeSmall,
            @Value("${pony.artwork.size.large}") int[] artworkSizeLarge
    ) {

        this.artworkRepository = artworkRepository;
        this.fileTypeResolver = fileTypeResolver;
        this.checksumCalculator = checksumCalculator;
        this.thumbnailGenerator = thumbnailGenerator;

        this.artworkFolder = artworkFolder;
        this.artworkSizeSmall = ImageSize.of(artworkSizeSmall[0], artworkSizeSmall[1]);
        this.artworkSizeLarge = ImageSize.of(artworkSizeLarge[0], artworkSizeLarge[1]);
    }

    @Transactional(readOnly = true)
    public Optional<ArtworkFiles> getArtworkFile(String artworkId) {
        return artworkRepository.findById(artworkId)
                .map(this::artworkToArtworkFiles);
    }

    @Transactional
    public ArtworkFiles getOrSave(ByteSourceArtworkStorageCommand command) throws IOException {
        synchronized (modificationLock) {
            byte[] content = command.getByteSource().read();
            return doGetOrSave(command.getSourceUri(),
                    () -> checksumCalculator.calculate(content),
                    () -> fileTypeResolver.resolve(content),
                    () -> new ByteArrayInputStream(content));
        }
    }

    @Transactional
    public ArtworkFiles getOrSave(FileArtworkStorageCommand command) throws IOException {
        synchronized (modificationLock) {
            File file = command.getFile();
            return doGetOrSave(command.getSourceUri(),
                    rethrow(() -> checksumCalculator.calculate(file)),
                    rethrow(() -> fileTypeResolver.resolve(file)),
                    rethrow(() -> new FileInputStream(file)));
        }
    }

    @Transactional
    public ArtworkFiles getOrSave(ImageNodeArtworkStorageCommand command) throws IOException {
        synchronized (modificationLock) {
            return doGetOrSave(command.getSourceUri(),
                    rethrow(() -> command.getImageNode().getChecksum()),
                    rethrow(() -> command.getImageNode().getFileType()),
                    rethrow(() -> new FileInputStream(command.getImageNode().getFile())));
        }
    }

    @Transactional
    public void delete(String id) {
        artworkRepository.findById(id).ifPresent(artwork -> {
            File largeFile = new File(artworkFolder, artwork.getLargeImagePath());
            File smallFile = new File(artworkFolder, artwork.getSmallImagePath());
            artworkRepository.delete(artwork);
            registerSynchronization(new TransactionSynchronization() {
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
        });
    }

    private ArtworkFiles artworkToArtworkFiles(Artwork artwork) {
        return new ArtworkFiles(artwork,
                new File(artworkFolder, artwork.getSmallImagePath()),
                new File(artworkFolder, artwork.getLargeImagePath()));
    }

    private ArtworkFiles doGetOrSave(URI sourceUri,
                                     Supplier<String> checksumSupplier,
                                     Supplier<FileType> fileTypeSupplier,
                                     Supplier<InputStream> streamSupplier) throws IOException {

        String checksum = checksumSupplier.get();
        Artwork artwork = artworkRepository.findByChecksumAndSourceUriScheme(checksum, sourceUri.getScheme());
        if (artwork != null) {
            return artworkToArtworkFiles(artwork);
        }

        FileType fileType = fileTypeSupplier.get();

        String uuid = UUID.randomUUID().toString();
        String smallImagePath = buildImagePath(uuid, "small", THUMBNAIL_FORMAT);
        String largeImagePath = buildImagePath(uuid, "large", THUMBNAIL_FORMAT);

        File smallImageFile = new File(artworkFolder, smallImagePath);
        File largeImageFile = new File(artworkFolder, largeImagePath);
        Files.createParentDirs(smallImageFile);
        Files.createParentDirs(largeImageFile);

        try (InputStream stream = streamSupplier.get()) {
            try {
                thumbnailGenerator.generateThumbnail(stream, artworkSizeSmall, THUMBNAIL_FORMAT, smallImageFile);
            } catch (Exception e) {
                throw new IOException("Could not generate small thumbnail for: " + sourceUri, e);
            }
        }
        try (InputStream stream = streamSupplier.get()) {
            try {
                thumbnailGenerator.generateThumbnail(stream, artworkSizeLarge, THUMBNAIL_FORMAT, largeImageFile);
            } catch (Exception e) {
                throw new IOException("Could not generate large thumbnail for: " + sourceUri, e);
            }
        }

        artwork = artworkRepository.save(new Artwork()
                .setMimeType(fileType.getMimeType())
                .setChecksum(checksum)
                .setSourceUri(sourceUri)
                .setSmallImagePath(smallImagePath)
                .setLargeImagePath(largeImagePath)
                .setSmallImageSize(smallImageFile.length())
                .setLargeImageSize(largeImageFile.length())
        );

        registerSynchronization(new TransactionSynchronization() {
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

        return artworkToArtworkFiles(artwork);
    }

    @SuppressWarnings({"StringBufferReplaceableByString", "SameParameterValue"})
    private String buildImagePath(String name, String suffix, String extension) {
        StringBuilder builder = new StringBuilder();
        builder.append(name, 0, 2).append("/");
        builder.append(name, 2, 4).append("/");
        builder.append(name).append(".").append(suffix).append(".").append(extension);
        return builder.toString();
    }
}
