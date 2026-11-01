package net.dorokhov.pony2.core.library.service.image;

import com.google.common.io.Files;
import net.dorokhov.pony2.api.library.domain.ExternalImage;
import net.dorokhov.pony2.api.library.domain.ExternalImageFile;
import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.core.library.repository.ExternalImageRepository;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;

@Component
public class ExternalImageStorage {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExternalImageRepository externalImageRepository;
    private final FileTypeResolver fileTypeResolver;
    private final File externalImageFolder;
    private final OkHttpClient httpClient;

    public ExternalImageStorage(
            ExternalImageRepository externalImageRepository,
            FileTypeResolver fileTypeResolver,
            @Value("${pony.externalImage.path}") File externalImageFolder,
            @Value("${pony.proxy.url}") URI proxyUrl
    ) {
        this.externalImageRepository = externalImageRepository;
        this.fileTypeResolver = fileTypeResolver;
        this.externalImageFolder = externalImageFolder;
        checkArgument("http".equalsIgnoreCase(proxyUrl.getScheme()) && proxyUrl.getHost() != null,
                "Proxy URL must specify an HTTP proxy host.");
        int proxyPort = proxyUrl.getPort() != -1 ? proxyUrl.getPort() : 80;
        httpClient = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyUrl.getHost(), proxyPort)))
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<ExternalImageFile> getImageFile(String externalImageId) {
        return externalImageRepository.findById(externalImageId)
                .map(externalImage -> new ExternalImageFile(externalImage, new File(externalImageFolder, externalImage.getPath())));
    }

    @Transactional(rollbackFor = IOException.class)
    public ExternalImageFile downloadAndSave(URI sourceUri) throws IOException {
        Request request = new Request.Builder().url(sourceUri.toString()).build();
        byte[] content;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Could not download external image from '" + sourceUri + "': HTTP " + response.code());
            }
            content = response.body().bytes();
        }
        FileType fileType = fileTypeResolver.resolve(content);
        if (!fileType.isImage()) {
            throw new IOException("Downloaded content is not an image: '" + sourceUri + "'.");
        }
        String uuid = UUID.randomUUID().toString();
        String path = uuid.substring(0, 2) + "/" + uuid.substring(2, 4) + "/" + uuid + "." + fileType.getFileExtension();
        File file = new File(externalImageFolder, path);
        registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteFile(file);
                }
            }
        });
        try {
            Files.createParentDirs(file);
            Files.write(content, file);
        } catch (IOException e) {
            deleteFile(file);
            throw e;
        }
        ExternalImage externalImage = externalImageRepository.save(new ExternalImage()
                .setSourceUri(sourceUri)
                .setPath(path)
                .setMimeType(fileType.getMimeType())
                .setSize(content.length));
        return new ExternalImageFile(externalImage, file);
    }

    private void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            logger.warn("Could not delete external image file: '{}'.", file.getAbsolutePath());
        }
    }

    @Transactional
    public void delete(String externalImageId) {
        externalImageRepository.findById(externalImageId).ifPresent(externalImage -> {
            File file = new File(externalImageFolder, externalImage.getPath());
            externalImageRepository.delete(externalImage);
            registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFile(file);
                }
            });
        });
    }
}
