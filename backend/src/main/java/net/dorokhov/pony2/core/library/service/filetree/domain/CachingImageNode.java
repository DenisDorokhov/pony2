package net.dorokhov.pony2.core.library.service.filetree.domain;

import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony2.core.library.service.image.ImageSizeReader;
import net.dorokhov.pony2.core.library.service.image.domain.ImageSize;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class CachingImageNode extends AbstractCachingFileNode implements ImageNode {

    private final ImageSizeReader imageSizeReader;

    private volatile ImageSize imageSize = null;

    private final Object imageSizeLock = new Object();

    public CachingImageNode(
            File file, FolderNode parentFolder,
            FileTypeResolver fileTypeResolver, ChecksumCalculator checksumCalculator,
            ImageSizeReader imageSizeReader
    ) {
        super(file, parentFolder, fileTypeResolver, checksumCalculator);
        this.imageSizeReader = checkNotNull(imageSizeReader);
    }

    @Override
    public ImageSize getImageSize() throws IOException {
        if (imageSize == null) {
            synchronized (imageSizeLock) {
                if (imageSize == null) {
                    imageSize = imageSizeReader.read(file);
                }
            }
        }
        return imageSize;
    }
}
