package net.dorokhov.pony2.core.library.service.filetree.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface FolderNode extends Node {

    List<ImageNode> getChildImages();
    List<AudioNode> getChildAudios();
    List<FolderNode> getChildFolders();

    default boolean isIgnored() {
        return false;
    }

    default List<ImageNode> getNotIgnoredChildImagesRecursively() {
        List<ImageNode> result = new ArrayList<>();
        fetchChildrenRecursively(this, result, FolderNode::getChildImages);
        return result;
    }
    
    default List<AudioNode> getNotIgnoredChildAudiosRecursively() {
        List<AudioNode> result = new ArrayList<>();
        fetchChildrenRecursively(this, result, FolderNode::getChildAudios);
        return result;
    }

    private static <T> void fetchChildrenRecursively(FolderNode folder, List<T> result, Function<FolderNode, List<T>> childrenProvider) {
        if (!folder.isIgnored()) {
            result.addAll(childrenProvider.apply(folder));
            folder.getChildFolders().forEach(nextFolder -> fetchChildrenRecursively(nextFolder, result, childrenProvider));
        }
    }
}
