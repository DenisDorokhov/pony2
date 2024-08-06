package net.dorokhov.pony2.core.library.service.filetree.domain;

import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony2.core.library.service.file.FileTypeResolver;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractCachingFileNode extends AbstractNode implements FileNode {

    private final FileTypeResolver fileTypeResolver;
    private final ChecksumCalculator checksumCalculator;

    private volatile FileType fileType = null;
    private volatile String checksum = null;

    private final Object fileTypeLock = new Object();
    private final Object checksumLock = new Object();

    protected AbstractCachingFileNode(
            File file, FolderNode parentFolder,
            FileTypeResolver fileTypeResolver, ChecksumCalculator checksumCalculator
    ) {
        super(file, parentFolder);
        this.fileTypeResolver = checkNotNull(fileTypeResolver);
        this.checksumCalculator = checkNotNull(checksumCalculator);
    }

    @Override
    public FileType getFileType() throws IOException {
        if (fileType == null) {
            synchronized (fileTypeLock) {
                if (fileType == null) {
                    fileType = fileTypeResolver.resolve(file);
                }
            }
        }
        return fileType;
    }

    @Override
    public String getChecksum() throws IOException {
        if (checksum == null) {
            synchronized (checksumLock) {
                if (checksum == null) {
                    checksum = checksumCalculator.calculate(file);
                }
            }
        }
        return checksum;
    }
}
