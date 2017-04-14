package net.dorokhov.pony.artwork;

import net.dorokhov.pony.entity.Artwork;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import net.dorokhov.pony.image.ImageSize;
import net.dorokhov.pony.image.ThumbnailGenerator;
import net.dorokhov.pony.repository.ArtworkRepository;
import net.dorokhov.pony.util.RandomString;
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
        this.artworkSizeSmall = new ImageSize(artworkSizeSmall[0], artworkSizeSmall[1]);
        this.artworkSizeLarge = new ImageSize(artworkSizeLarge[0], artworkSizeLarge[1]);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCountByTag(String tag) {
        return artworkRepository.countByTag(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCountByTagAndMinimalDate(String tag, LocalDateTime minimalDate) {
        return artworkRepository.countByTagAndDateGreaterThan(tag, minimalDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long getSizeByTag(String tag) {
        return artworkRepository.sumLargeImageSizeByTag(tag) + artworkRepository.sumSmallImageSizeByTag(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Artwork> getById(Long id) {
        return Optional.ofNullable(artworkRepository.findOne(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Artwork> getByTag(String tag, Pageable pageable) {
        return artworkRepository.findByTag(tag, pageable);
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
    public Artwork getOrSave(SaveByteSourceArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            byte[] content = command.getByteSource().read();
            return doGetOrSave(command, 
                    () -> checksumCalculator.calculate(content), 
                    () -> fileTypeResolver.resolve(content),
                    () -> new ByteArrayInputStream(content));
        }
    }

    @Override
    @Transactional
    public Artwork getOrSave(SaveFileArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            File file = command.getFile();
            return doGetOrSave(command,
                    rethrow(() -> checksumCalculator.calculate(file)),
                    rethrow(() -> fileTypeResolver.resolve(file)),
                    rethrow(() -> new FileInputStream(file)));
        }
    }

    @Override
    @Transactional
    public Artwork getOrSave(SaveImageNodeArtworkCommand command) throws IOException {
        synchronized (modificationLock) {
            return doGetOrSave(command,
                    rethrow(() -> command.getImageNode().getChecksum()),
                    rethrow(() -> command.getImageNode().getFileType()),
                    rethrow(() -> new FileInputStream(command.getImageNode().getFile())));
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
    
    private Artwork doGetOrSave(SaveArtworkCommand command,
                                Supplier<String> checksumSupplier, Supplier<FileType> fileTypeSupplier,
                                Supplier<InputStream> streamSupplier) throws IOException {

        String checksum = checksumSupplier.get();
        Artwork artwork = artworkRepository.findByTagAndChecksum(command.getTag().orElse(null), checksum);
        if (artwork != null) {
            return artwork;
        }

        FileType fileType = fileTypeSupplier.get();

        artwork = new Artwork();
        artwork.setMimeType(fileType.getMimeType());
        artwork.setChecksum(checksum);
        command.getTag().ifPresent(artwork::setTag);
        artwork.setMetaData(command.getMetaData());

        artwork = artworkRepository.save(artwork);

        String smallImagePath = buildSmallImagePath(fileType.getFileExtension(), artwork);
        String largeImagePath = buildLargeImagePath(fileType.getFileExtension(), artwork);
        artwork.setSmallImagePath(smallImagePath);
        artwork.setLargeImagePath(largeImagePath);

        File smallImageFile = new File(artworkFolder, smallImagePath);
        File largeImageFile = new File(artworkFolder, largeImagePath);
        thumbnailGenerator.generateThumbnail(streamSupplier.get(), artworkSizeSmall, smallImageFile);
        thumbnailGenerator.generateThumbnail(streamSupplier.get(), artworkSizeLarge, largeImageFile);
        
        artwork.setSmallImageSize(smallImageFile.length());
        artwork.setLargeImageSize(largeImageFile.length());

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
    
    private String buildLargeImagePath(String extension, Artwork artwork) {
        return buildImagePath("large", extension, artwork);
    }
    
    private String buildSmallImagePath(String extension, Artwork artwork) {
        return buildImagePath("small", extension, artwork);
    }
    
    private String buildImagePath(String suffix, String extension, Artwork artwork) {
        StringBuilder builder = new StringBuilder();
        builder.append(RandomString.generate(2)).append("/");
        builder.append(RandomString.generate(2)).append("/");
        builder.append(artwork.getId()).append(".").append(suffix).append(".").append(extension);
        return builder.toString();
    }
}
