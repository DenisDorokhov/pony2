package net.dorokhov.pony3.core.library.service.filetree;

import net.dorokhov.pony3.api.library.domain.FileType;
import net.dorokhov.pony3.core.library.service.AudioTagger;
import net.dorokhov.pony3.core.library.service.file.ChecksumCalculator;
import net.dorokhov.pony3.core.library.service.file.FileTypeResolver;
import net.dorokhov.pony3.core.library.service.filetree.domain.*;
import net.dorokhov.pony3.core.library.service.image.ImageSizeReader;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class FileTreeScanner {

    private final FileTypeResolver fileTypeResolver;
    private final ImageSizeReader imageSizeReader;
    private final ChecksumCalculator checksumCalculator;
    private final AudioTagger audioTagger;

    public FileTreeScanner(
            FileTypeResolver fileTypeResolver,
            ImageSizeReader imageSizeReader,
            ChecksumCalculator checksumCalculator,
            AudioTagger audioTagger
    ) {
        this.fileTypeResolver = fileTypeResolver;
        this.imageSizeReader = imageSizeReader;
        this.checksumCalculator = checksumCalculator;
        this.audioTagger = audioTagger;
    }

    public FileNode scanFile(File file, List<File> rootFolders) throws IOException {
        checkArgument(file.exists(), "Existing file expected.");
        checkArgument(file.isFile(), "Normal file expected.");
        for (File rootFolder : rootFolders) {
            if (file.getAbsolutePath().startsWith(rootFolder.getAbsolutePath())) {
                FileNode fileNode = new LazyFolderNode(file.getParentFile(), rootFolder, this::resolveFileNode).findFileNode(file);
                if (fileNode != null) {
                    return fileNode;
                }
            }
        }
        throw new FileNotFoundException(String.format("File '%s' has unknown type or not found in specified folders %s.",
                file, rootFolders));
    }

    public FolderNode scanFolder(File folder) throws IOException {
        checkArgument(folder.exists(), "Existing file expected.");
        checkArgument(folder.isDirectory(), "Directory expected.");
        Visitor visitor = new Visitor();
        Files.walkFileTree(folder.toPath(), visitor);
        return visitor.getRoot();
    }

    @Nullable
    private FileNode resolveFileNode(File file, FolderNode parentFolderNode) {
        FileType fileType = fileTypeResolver.resolve(file);
        if (fileType.isImage()) {
            return new CachingImageNode(file, parentFolderNode, fileTypeResolver, checksumCalculator, imageSizeReader);
        } else if (fileType.isAudio()) {
            return new CachingAudioNode(file, parentFolderNode, fileTypeResolver, checksumCalculator, audioTagger);
        }
        return null;
    }

    private class Visitor extends SimpleFileVisitor<Path> {

        private final Stack<MutableFolderNode> folderStack = new Stack<>();

        private FolderNode root;

        public FolderNode getRoot() {
            return root;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            MutableFolderNode parentFolder = root != null ? folderStack.peek() : null;
            MutableFolderNode folderNode = new MutableFolderNode(dir.toFile(), parentFolder);
            if (parentFolder != null) {
                parentFolder.getMutableChildFolders().add(folderNode);
            }
            folderStack.push(folderNode);
            if (root == null) {
                root = folderNode;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            MutableFolderNode parentFolder = folderStack.peek();
            FileNode fileNode = resolveFileNode(file.toFile(), parentFolder);
            if (fileNode instanceof ImageNode) {
                parentFolder.getMutableChildImages().add((ImageNode) fileNode);
            } else if (fileNode instanceof AudioNode) {
                parentFolder.getMutableChildAudios().add((AudioNode) fileNode);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            folderStack.pop();
            return FileVisitResult.CONTINUE;
        }
    }
}
