package net.dorokhov.pony2.core.library.service.filetree.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jakarta.annotation.Nullable;
import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractNode implements Node {

    protected final File file;
    protected final FolderNode parentFolder;

    public AbstractNode(File file, FolderNode parentFolder) {
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
        if (obj instanceof Node that) {
            return file.equals(that.getFile());
        }
        return false;
    }

    @Override
    public String toString() {
        return file.toString();
    }
}
