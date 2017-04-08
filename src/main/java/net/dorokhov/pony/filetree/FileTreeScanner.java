package net.dorokhov.pony.filetree;

import com.google.common.base.Preconditions;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
import net.dorokhov.pony.image.ImageSize;
import net.dorokhov.pony.image.ImageSizeReader;
import net.dorokhov.pony.util.RethrowingLambdas.ThrowingUnaryOperator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import static net.dorokhov.pony.util.RethrowingLambdas.rethrow;

@Component
public class FileTreeScanner {

    private final FileTypeResolver fileTypeResolver;
    private final ImageSizeReader imageSizeReader;
    private final ChecksumCalculator checksumCalculator;

    public FileTreeScanner(FileTypeResolver fileTypeResolver, ImageSizeReader imageSizeReader, ChecksumCalculator checksumCalculator) {
        this.fileTypeResolver = fileTypeResolver;
        this.imageSizeReader = imageSizeReader;
        this.checksumCalculator = checksumCalculator;
    }

    public Optional<FileNode> scanFile(File file) throws IOException {
        Preconditions.checkArgument(file.exists(), "Existing file expected.");
        Preconditions.checkArgument(file.isFile(), "Normal file expected.");
        FileType fileType = fileTypeResolver.resolve(file);
        if (fileType.isImage()) {
            return Optional.of(new ImageNodeImpl(file, null));
        } else if (fileType.isAudio()) {
            return Optional.of(new AudioNodeImpl(file, null)); 
        }
        return Optional.empty();
    }

    public FolderNode scanFolder(File folder) throws IOException {
        Preconditions.checkArgument(folder.exists(), "Existing file expected.");
        Preconditions.checkArgument(folder.isDirectory(), "Directory expected.");
        Visitor visitor = new Visitor();
        Files.walkFileTree(folder.toPath(), visitor);
        return visitor.getRoot();
    }

    private abstract class NodeImpl implements Node {

        protected final File file;
        protected final FolderNode parentFolder;

        public NodeImpl(File file, FolderNode parentFolder) {
            Preconditions.checkNotNull(file);
            this.file = file;
            this.parentFolder = parentFolder;
        }

        @Override
        public File getFile() {
            return file;
        }

        @Override
        public Optional<FolderNode> getParentFolder() {
            return Optional.ofNullable(parentFolder);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass().equals(obj.getClass())) {
                NodeImpl that = (NodeImpl) obj;
                return file.getAbsolutePath().equals(that.file.getAbsolutePath());
            }
            return false;
        }

        @Override
        public String toString() {
            return "NodeImpl{" +
                    "file=" + file.getAbsolutePath() +
                    '}';
        }
    }

    private class FolderNodeImpl extends NodeImpl implements FolderNode {

        private final Set<ImageNode> childImages = new HashSet<>();
        private final Set<AudioNode> childAudios = new HashSet<>();
        private final Set<FolderNodeImpl> childFolders = new HashSet<>();

        public FolderNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public Set<ImageNode> getChildImages(boolean recursive) {
            Set<ImageNode> result = new HashSet<>();
            doGetChildImages(result, recursive);
            return result;
        }

        @Override
        public Set<AudioNode> getChildAudios(boolean recursive) {
            Set<AudioNode> result = new HashSet<>();
            doGetChildAudios(result, recursive);
            return result;
        }

        @Override
        public Set<FolderNode> getChildFolders(boolean recursive) {
            Set<FolderNode> result = new HashSet<>();
            doGetChildFolders(result, recursive);
            return result;
        }

        private Set<ImageNode> getChildImagesMutable() {
            return childImages;
        }

        private Set<AudioNode> getChildAudiosMutable() {
            return childAudios;
        }

        private Set<FolderNodeImpl> getChildFoldersMutable() {
            return childFolders;
        }

        private void doGetChildImages(Set<ImageNode> result, boolean recursive) {
            result.addAll(childImages);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildImages(result, true));
            }
        }

        private void doGetChildAudios(Set<AudioNode> result, boolean recursive) {
            result.addAll(childAudios);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildAudios(result, true));
            }
        }

        private void doGetChildFolders(Set<FolderNode> result, boolean recursive) {
            result.addAll(childFolders);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildFolders(result, true));
            }
        }
    }

    private abstract class FileNodeImpl extends NodeImpl implements FileNode {

        private final AtomicReference<FileType> mimeType = new AtomicReference<>();
        private final AtomicReference<String> checksum = new AtomicReference<>();

        protected FileNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public FileType getType() throws IOException {
            try {
                return mimeType.updateAndGet(rethrow((ThrowingUnaryOperator<FileType>) type -> {
                    if (type == null) {
                        return fileTypeResolver.resolve(file);
                    } else {
                        return type;
                    }
                }));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public String getChecksum() throws IOException {
            try {
                return checksum.updateAndGet(rethrow((ThrowingUnaryOperator<String>) checksum -> {
                    if (checksum == null) {
                        return checksumCalculator.calculate(file);
                    } else {
                        return checksum;
                    }
                }));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private class ImageNodeImpl extends FileNodeImpl implements ImageNode {

        private AtomicReference<ImageSize> imageSize = new AtomicReference<>();

        private ImageNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public ImageSize getImageSize() throws IOException {
            try {
                return imageSize.updateAndGet(rethrow((ThrowingUnaryOperator<ImageSize>) imageSize -> {
                    if (imageSize == null) {
                        return imageSizeReader.read(file);
                    } else {
                        return imageSize;
                    }
                }));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    private class AudioNodeImpl extends FileNodeImpl implements AudioNode {
        private AudioNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }
    }
    
    private class Visitor extends SimpleFileVisitor<Path> {
        
        private final Stack<FolderNodeImpl> folderStack = new Stack<>();
        
        private FolderNode root;

        public FolderNode getRoot() {
            return root;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            FolderNodeImpl parentFolder = root != null ? folderStack.peek() : null;
            FolderNodeImpl folderNode = new FolderNodeImpl(dir.toFile(), parentFolder);
            if (parentFolder != null) {
                parentFolder.getChildFoldersMutable().add(folderNode);
            }
            folderStack.push(folderNode);
            if (root == null) {
                root = folderNode;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            FolderNodeImpl parentFolder = folderStack.peek();
            scanFile(file.toFile()).ifPresent(fileNode -> {
                if (fileNode instanceof ImageNode) {
                    parentFolder.getChildImagesMutable().add((ImageNode) fileNode);
                } else if (fileNode instanceof AudioNode) {
                    parentFolder.getChildAudiosMutable().add((AudioNode) fileNode);
                } else {
                    throw new IllegalStateException(String.format("Unknown file type '%s'.", fileNode.getClass().getSimpleName()));
                }
            });
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            folderStack.pop();
            return FileVisitResult.CONTINUE;
        }
    }
}
