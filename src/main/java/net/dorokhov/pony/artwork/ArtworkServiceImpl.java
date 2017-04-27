package net.dorokhov.pony.artwork;

import net.dorokhov.pony.artwork.domain.ArtworkDraft;
import net.dorokhov.pony.artwork.domain.ByteSourceArtworkDraft;
import net.dorokhov.pony.artwork.domain.FileArtworkDraft;
import net.dorokhov.pony.artwork.domain.ImageNodeArtworkDraft;
import net.dorokhov.pony.entity.Artwork;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.domain.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import net.dorokhov.pony.image.domain.ImageSize;
import net.dorokhov.pony.image.ThumbnailGenerator;
import net.dorokhov.pony.repository.ArtworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static net.dorokhov.pony.util.RethrowingLambdas.rethrow;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ArtworkRepository artworkRepository;
    private final FileTypeResolver fileTypeResolver;
    private final ChecksumCalculator checksumCalculator;
    private final ThumbnailGenerator thumbnailGenerator;
    
    private final File artworkFolder;
    private final ImageSize artworkSizeSmall;
    private final ImageSize artworkSizeLarge;
    
    private final Object modificationLock = new Object();

    public ArtworkServiceImpl(ArtworkRepository artworkRepository,
                              FileTypeResolver fileTypeResolver, ChecksumCalculator checksumCalculator,
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

    @Override
    @Transactional(readOnly = true)
    public long getCount() {
        return artworkRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCountByMinimalDate(LocalDateTime minimalDate) {
        return artworkRepository.countByDateGreaterThan(minimalDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalSize() {
        return artworkRepository.sumLargeImageSize() + artworkRepository.sumSmallImageSize();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Artwork> getById(Long id) {
        return Optional.ofNullable(artworkRepository.findOne(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Artwork> getAll(Pageable pageable) {
        return artworkRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<File> getLargeImageFile(Long artworkId) {
        Optional<Artwork> artwork = getById(artworkId);
        return artwork.map(a -> new File(artworkFolder, a.getLargeImagePath()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<File> getSmallImageFile(Long artworkId) {
        Optional<Artwork> artwork = getById(artworkId);
        return artwork.map(a -> new File(artworkFolder, a.getSmallImagePath()));
    }

    @Override
    @Transactional
    public Artwork getOrSave(ByteSourceArtworkDraft draft) throws IOException {
        synchronized (modificationLock) {
            byte[] content = draft.getByteSource().read();
            return doGetOrSave(draft, 
                    () -> checksumCalculator.calculate(content), 
                    () -> fileTypeResolver.resolve(content),
                    () -> new ByteArrayInputStream(content));
        }
    }

    @Override
    @Transactional
    public Artwork getOrSave(FileArtworkDraft draft) throws IOException {
        synchronized (modificationLock) {
            File file = draft.getFile();
            return doGetOrSave(draft,
                    rethrow(() -> checksumCalculator.calculate(file)),
                    rethrow(() -> fileTypeResolver.resolve(file)),
                    rethrow(() -> new FileInputStream(file)));
        }
    }

    @Override
    @Transactional
    public Artwork getOrSave(ImageNodeArtworkDraft draft) throws IOException {
        synchronized (modificationLock) {
            return doGetOrSave(draft,
                    rethrow(() -> draft.getImageNode().getChecksum()),
                    rethrow(() -> draft.getImageNode().getFileType()),
                    rethrow(() -> new FileInputStream(draft.getImageNode().getFile())));
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id).ifPresent(artwork -> {
            File largeFile = new File(artworkFolder, artwork.getLargeImagePath());
            File smallFile = new File(artworkFolder, artwork.getSmallImagePath());
            artworkRepository.delete(artwork);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    if (!largeFile.delete()) {
                        log.warn("Could not delete artwork large image file: '{}'.", largeFile.getAbsolutePath());
                    }
                    if (!smallFile.delete()) {
                        log.warn("Could not delete artwork small image file: '{}'.", smallFile.getAbsolutePath());
                    }
                }
            });
        });
    }
    
    private Artwork doGetOrSave(ArtworkDraft draft,
                                Supplier<String> checksumSupplier, Supplier<FileType> fileTypeSupplier,
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
                .sourceUri(draft.getSourceUri())
                .smallImagePath(smallImagePath)
                .largeImagePath(largeImagePath)
                .smallImageSize(smallImageFile.length())
                .largeImageSize(largeImageFile.length())
                .build());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    if (!smallImageFile.delete()) {
                        log.error("Could not delete small image file after rollback: '{}'.", smallImageFile.getAbsolutePath());
                    }
                    if (!largeImageFile.delete()) {
                        log.error("Could not delete large image file after rollback: '{}'.", largeImageFile.getAbsolutePath());
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
