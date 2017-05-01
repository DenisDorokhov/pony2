package net.dorokhov.pony.library.service.impl.filetree;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.dorokhov.pony.common.RethrowingLambdas.ThrowingUnaryOperator;
import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.service.impl.audio.AudioTagger;
import net.dorokhov.pony.library.service.impl.audio.domain.ReadableAudioData;
import net.dorokhov.pony.library.service.impl.file.ChecksumCalculator;
import net.dorokhov.pony.library.service.impl.file.FileTypeResolver;
import net.dorokhov.pony.library.service.impl.filetree.domain.*;
import net.dorokhov.pony.library.service.impl.image.ImageSizeReader;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.dorokhov.pony.common.RethrowingLambdas.rethrow;

@Component
public class FileTreeScanner {

    private final FileTypeResolver fileTypeResolver;
    private final ImageSizeReader imageSizeReader;
    private final ChecksumCalculator checksumCalculator;
    private final AudioTagger audioTagger;

    public FileTreeScanner(FileTypeResolver fileTypeResolver,
                           ImageSizeReader imageSizeReader,
                           ChecksumCalculator checksumCalculator, 
                           AudioTagger audioTagger) {
        this.fileTypeResolver = fileTypeResolver;
        this.imageSizeReader = imageSizeReader;
        this.checksumCalculator = checksumCalculator;
        this.audioTagger = audioTagger;
    }

    @Nullable
    public FileNode scanFile(File file) throws IOException {
        checkArgument(file.exists(), "Existing file expected.");
        checkArgument(file.isFile(), "Normal file expected.");
        FileType fileType = fileTypeResolver.resolve(file);
        if (fileType.isImage()) {
            return new ImageNodeImpl(file, null);
        } else if (fileType.isAudio()) {
            return new AudioNodeImpl(file, null); 
        }
        return null;
    }

    public FolderNode scanFolder(File folder) throws IOException {
        checkArgument(folder.exists(), "Existing file expected.");
        checkArgument(folder.isDirectory(), "Directory expected.");
        Visitor visitor = new Visitor();
        Files.walkFileTree(folder.toPath(), visitor);
        return visitor.getRoot();
    }

    private abstract class NodeImpl implements Node {

        protected final File file;
        protected final FolderNode parentFolder;

        public NodeImpl(File file, FolderNode parentFolder) {
            this.file = checkNotNull(file);
            this.parentFolder = parentFolder;
        }

        @Override
        public File getFile() {
            return file;
        }

        @Override
        @Nullable
        public FolderNode getParentFolder() {
            return parentFolder;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
        public boolean equals(@Nullable Object obj) {
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

        private final List<ImageNode> childImages = new ArrayList<>();
        private final List<AudioNode> childAudios = new ArrayList<>();
        private final List<FolderNodeImpl> childFolders = new ArrayList<>();

        public FolderNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public List<ImageNode> getChildImages(boolean recursive) {
            ImmutableList.Builder<ImageNode> builder = ImmutableList.builder();
            doGetChildImages(builder, recursive);
            return builder.build();
        }

        @Override
        public List<AudioNode> getChildAudios(boolean recursive) {
            ImmutableList.Builder<AudioNode> builder = ImmutableList.builder();
            doGetChildAudios(builder, recursive);
            return builder.build();
        }

        @Override
        public List<FolderNode> getChildFolders(boolean recursive) {
            ImmutableList.Builder<FolderNode> builder = ImmutableList.builder();
            doGetChildFolders(builder, recursive);
            return builder.build();
        }

        private List<ImageNode> getChildImagesMutable() {
            return childImages;
        }

        private List<AudioNode> getChildAudiosMutable() {
            return childAudios;
        }

        private List<FolderNodeImpl> getChildFoldersMutable() {
            return childFolders;
        }

        private void doGetChildImages(ImmutableList.Builder<ImageNode> result, boolean recursive) {
            result.addAll(childImages);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildImages(result, true));
            }
        }

        private void doGetChildAudios(ImmutableList.Builder<AudioNode> result, boolean recursive) {
            result.addAll(childAudios);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildAudios(result, true));
            }
        }

        private void doGetChildFolders(ImmutableList.Builder<FolderNode> result, boolean recursive) {
            result.addAll(childFolders);
            if (recursive) {
                childFolders.forEach(folder -> folder.doGetChildFolders(result, true));
            }
        }
    }

    private abstract class FileNodeImpl extends NodeImpl implements FileNode {

        private final AtomicReference<FileType> fileType = new AtomicReference<>();
        private final AtomicReference<String> checksum = new AtomicReference<>();

        protected FileNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public FileType getFileType() throws IOException {
            try {
                return fileType.updateAndGet(rethrow((ThrowingUnaryOperator<FileType>) type -> {
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
        
        private AtomicReference<ReadableAudioData> audioData = new AtomicReference<>();
        
        private AudioNodeImpl(File file, FolderNode parentFolder) {
            super(file, parentFolder);
        }

        @Override
        public ReadableAudioData getAudioData() throws IOException {
            try {
                return audioData.updateAndGet(rethrow((ThrowingUnaryOperator<ReadableAudioData>) audioData -> {
                    if (audioData == null) {
                        return audioTagger.read(file);
                    } else {
                        return audioData;
                    }
                }));
            } catch (Exception e) {
                throw new IOException(e);
            }
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
            FileNode fileNode = scanFile(file.toFile());
            if (fileNode instanceof ImageNode) {
                parentFolder.getChildImagesMutable().add((ImageNode) fileNode);
            } else if (fileNode instanceof AudioNode) {
                parentFolder.getChildAudiosMutable().add((AudioNode) fileNode);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            folderStack.pop();
            return FileVisitResult.CONTINUE;
        }
    }
}
