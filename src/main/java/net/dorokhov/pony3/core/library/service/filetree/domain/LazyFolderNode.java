package net.dorokhov.pony3.core.library.service.filetree.domain;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

public class LazyFolderNode implements FolderNode {
    
    public interface FileNodeResolver {
        @Nullable
        FileNode resolveFileNode(File file, FolderNode parentFolder) throws IOException;
    }

    private final File file;
    private final File rootFolder;
    private final FileNodeResolver fileNodeResolver;

    private volatile FolderNode parentFolder;
    private volatile List<Node> childNodes = null;

    private final Object parentFolderLock = new Object();
    private final Object childNodesLock = new Object();

    public LazyFolderNode(File file, File rootFolder, FileNodeResolver fileNodeResolver) {
        this(file, rootFolder, fileNodeResolver, null);
    }

    private LazyFolderNode(File file, File rootFolder, FileNodeResolver fileNodeResolver, FolderNode parentFolder) {
        this.file = checkNotNull(file);
        this.rootFolder = checkNotNull(rootFolder);
        this.fileNodeResolver = checkNotNull(fileNodeResolver);
        this.parentFolder = parentFolder;
    }

    @Nullable
    public FileNode findFileNode(File file) {
        return getChildNodes().stream()
                .filter(node -> node instanceof FileNode)
                .map(node -> (FileNode) node)
                .filter(node -> node.getFile().equals(file))
                .findFirst()
                .orElse(null);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Nullable
    @Override
    public FolderNode getParentFolder() {
        if (rootFolder.equals(file)) {
            return null;
        }
        if (parentFolder == null) {
            synchronized (parentFolderLock) {
                if (parentFolder == null) {
                    parentFolder = new LazyFolderNode(file.getParentFile(), rootFolder, fileNodeResolver);
                }
            }
        }
        return parentFolder;
    }

    @Override
    public List<ImageNode> getChildImages() {
        return getChildNodes().stream()
                .filter(node -> node instanceof ImageNode)
                .map(node -> (ImageNode) node)
                .collect(Collectors.toList());
    }

    @Override
    public List<AudioNode> getChildAudios() {
        return getChildNodes().stream()
                .filter(node -> node instanceof AudioNode)
                .map(node -> (AudioNode) node)
                .collect(Collectors.toList());
    }

    @Override
    public List<FolderNode> getChildFolders() {
        return getChildNodes().stream()
                .filter(node -> node instanceof FolderNode)
                .map(node -> (FolderNode) node)
                .collect(Collectors.toList());
    }

    public List<Node> getChildNodes() {
        if (childNodes == null) {
            synchronized (childNodesLock) {
                if (childNodes == null) {
                    childNodes = fetchChildNodes();
                }
            }
        }
        return childNodes;
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
            LazyFolderNode that = (LazyFolderNode) obj;
            return file.equals(that.file);
        }
        return false;
    }
    
    private List<Node> fetchChildNodes() {
        File[] childFiles = file.listFiles();
        if (childFiles != null) {
            return Arrays.stream(childFiles).flatMap(nextFile -> {
                if (nextFile.isDirectory()) {
                    return Stream.of(new LazyFolderNode(nextFile, rootFolder, fileNodeResolver, this));
                } else {
                    FileNode fileNode;
                    try {
                        fileNode = fileNodeResolver.resolveFileNode(nextFile, this);
                    } catch (IOException e) {
                        fileNode = null;
                    }
                    return fileNode != null ? Stream.of(fileNode) : Stream.empty();
                }
            }).collect(ImmutableList.toImmutableList());
        } else {
            return emptyList();
        }
    }
}
