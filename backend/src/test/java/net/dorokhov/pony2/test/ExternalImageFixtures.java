package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.ExternalImage;
import net.dorokhov.pony2.api.library.domain.ExternalImageFile;
import net.dorokhov.pony2.core.library.repository.ExternalImageRepository;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.UUID;

public final class ExternalImageFixtures {

    private ExternalImageFixtures() {
    }

    public static ExternalImage externalImage() {
        return new ExternalImage()
                .setSourceUri(URI.create("https://example.com/image.png"))
                .setPath(UUID.randomUUID() + ".png")
                .setMimeType("image/png")
                .setSize(0);
    }

    public static ExternalImageFile saveExternalImage(ExternalImageRepository repository, File externalImageFolder, String resourceName)
            throws IOException {
        ExternalImage externalImage = externalImage().setSourceUri(URI.create("https://example.com/" + resourceName));
        File file = new File(externalImageFolder, externalImage.getPath());
        Files.createDirectories(file.toPath().getParent());
        Files.copy(new ClassPathResource(resourceName).getFile().toPath(), file.toPath());
        return new ExternalImageFile(repository.save(externalImage.setSize(file.length())), file);
    }
}
