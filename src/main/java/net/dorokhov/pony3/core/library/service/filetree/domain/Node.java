package net.dorokhov.pony3.core.library.service.filetree.domain;

import jakarta.annotation.Nullable;
import java.io.File;

public interface Node {

    File getFile();

    @Nullable
    FolderNode getParentFolder();
}
