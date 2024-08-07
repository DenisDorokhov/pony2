package net.dorokhov.pony2.core.library.service.filetree.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class MutableFolderNode extends AbstractNode implements FolderNode {

    private final List<ImageNode> childImages = new ArrayList<>();
    private final List<AudioNode> childAudios = new ArrayList<>();
    private final List<MutableFolderNode> childFolders = new ArrayList<>();

    private boolean ignored = false;

    public MutableFolderNode(File file, MutableFolderNode parentFolder) {
        super(file, parentFolder);
    }

    @Override
    public List<ImageNode> getChildImages() {
        return unmodifiableList(childImages);
    }

    @Override
    public List<AudioNode> getChildAudios() {
        return unmodifiableList(childAudios);
    }

    @Override
    public List<FolderNode> getChildFolders() {
        return unmodifiableList(childFolders);
    }
    
    public List<ImageNode> getMutableChildImages() {
        return childImages;
    }
    
    public List<AudioNode> getMutableChildAudios() {
        return childAudios;
    }
    
    public List<MutableFolderNode> getMutableChildFolders() {
        return childFolders;
    }

    @Override
    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
}
