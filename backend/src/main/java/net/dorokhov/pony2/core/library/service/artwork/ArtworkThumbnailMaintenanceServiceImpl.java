package net.dorokhov.pony2.core.library.service.artwork;

import net.dorokhov.pony2.api.library.domain.Artwork;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData;
import net.dorokhov.pony2.api.library.service.ArtworkThumbnailMaintenanceService;
import net.dorokhov.pony2.core.library.repository.ArtworkRepository;
import net.dorokhov.pony2.core.library.service.AudioTagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import static net.dorokhov.pony2.common.RethrowingLambdas.rethrow;
import static net.dorokhov.pony2.core.library.LibraryConfig.ARTWORK_THUMBNAIL_REGENERATION_EXECUTOR;

@Service
public class ArtworkThumbnailMaintenanceServiceImpl implements ArtworkThumbnailMaintenanceService {

    private final static int THUMBNAIL_REGENERATION_PAGE_SIZE = 100;
    private final static int THUMBNAIL_REGENERATION_PROGRESS_INTERVAL = 100;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // ReentrantLock doesn't fit here because we release in a different thread.
    private final Semaphore thumbnailRegenerationSemaphore = new Semaphore(1);

    private final ArtworkRepository artworkRepository;
    private final ArtworkStorage artworkStorage;
    private final AudioTagger audioTagger;
    private final Executor thumbnailRegenerationExecutor;

    public ArtworkThumbnailMaintenanceServiceImpl(
            ArtworkRepository artworkRepository,
            ArtworkStorage artworkStorage,
            AudioTagger audioTagger,
            @Qualifier(ARTWORK_THUMBNAIL_REGENERATION_EXECUTOR) Executor thumbnailRegenerationExecutor
    ) {
        this.artworkRepository = artworkRepository;
        this.artworkStorage = artworkStorage;
        this.audioTagger = audioTagger;
        this.thumbnailRegenerationExecutor = thumbnailRegenerationExecutor;
    }

    @Override
    public void reGenerateThumbnailsAsync() {
        if (!thumbnailRegenerationSemaphore.tryAcquire()) {
            logger.warn("Re-generating artwork thumbnails is already running.");
            return;
        }
        try {
            thumbnailRegenerationExecutor.execute(() -> {
                try {
                    doReGenerateThumbnails();
                } finally {
                    thumbnailRegenerationSemaphore.release();
                }
            });
        } catch (RuntimeException e) {
            thumbnailRegenerationSemaphore.release();
            throw e;
        }
    }

    private void doReGenerateThumbnails() {

        logger.info("Re-generating artwork thumbnails...");
        long totalCount = artworkRepository.count();
        if (totalCount == 0) {
            logger.info("Re-generating artwork thumbnails done: no artworks found.");
            return;
        }
        logger.info("Found {} artworks for thumbnail regeneration...", totalCount);

        long processedCount = 0;
        long reGeneratedCount = 0;
        long failedCount = 0;

        Pageable pageable = PageRequest.of(0, THUMBNAIL_REGENERATION_PAGE_SIZE, Sort.by("id"));
        while (pageable != null) {
            Page<Artwork> artworks = artworkRepository.findAll(pageable);
            for (Artwork artwork : artworks) {
                try {
                    reGenerateThumbnails(artwork);
                    reGeneratedCount++;
                } catch (Exception e) {
                    logger.error("Could not re-generate artwork thumbnails for artwork '{}'.", artwork.getSourceUri(), e);
                    failedCount++;
                }
                processedCount++;
                if (processedCount % THUMBNAIL_REGENERATION_PROGRESS_INTERVAL == 0) {
                    String percentageCompleted = String.format(Locale.ROOT, "%.2f", (double) processedCount / totalCount * 100);
                    logger.info("Re-generating artwork thumbnails progress: {}% completed, {} re-generated, {} failed, {} left.",
                            percentageCompleted, reGeneratedCount, failedCount, totalCount - processedCount);
                }
            }
            pageable = artworks.hasNext() ? artworks.nextPageable() : null;
        }

        logger.info("Re-generating artwork thumbnails done: {} processed, {} re-generated, {} failed.",
                processedCount, reGeneratedCount, failedCount);
    }

    private void reGenerateThumbnails(Artwork artwork) throws Exception {

        URI sourceUri = artwork.getSourceUri();
        Supplier<InputStream> streamSupplier;
        if (Artwork.SOURCE_URI_SCHEME_FILE.equals(sourceUri.getScheme())) {
            streamSupplier = rethrow(() -> new FileInputStream(sourceUri.getPath()));
        } else if (Artwork.SOURCE_URI_SCHEME_EMBEDDED.equals(sourceUri.getScheme())) {
            ReadableAudioData audioData = audioTagger.read(new File(sourceUri.getPath()));
            ReadableAudioData.EmbeddedArtwork embeddedArtwork = audioData.getEmbeddedArtwork();
            if (embeddedArtwork == null) {
                throw new IOException("Could not find embedded artwork for: " + sourceUri);
            }
            streamSupplier = rethrow(() -> new ByteArrayInputStream(embeddedArtwork.getBinaryData().read()));
        } else {
            throw new IOException("Unsupported artwork source URI scheme: " + sourceUri.getScheme());
        }

        artworkStorage.reGenerateThumbnails(artwork, streamSupplier);
    }
}
