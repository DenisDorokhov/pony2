package net.dorokhov.pony3.core.library.service.artwork;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.Node;
import net.dorokhov.pony3.core.library.service.image.domain.ImageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.util.*;

@Component
public class ArtworkFileFinder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final double artworkSizeRatioMin;
    private final double artworkSizeRatioMax;

    private final Set<String> artworkFileNames;
    private final Set<String> artworkFolderNames;

    public ArtworkFileFinder(
            @Value("${pony.artwork.size-ratio.min}") double artworkSizeRatioMin,
            @Value("${pony.artwork.size-ratio.max}") double artworkSizeRatioMax,
            @Value("${pony.artwork.file-names}") String[] artworkFileNames,
            @Value("${pony.artwork.folder-names}") String[] artworkFolderNames
    ) {
        this.artworkSizeRatioMin = artworkSizeRatioMin;
        this.artworkSizeRatioMax = artworkSizeRatioMax;
        this.artworkFileNames = ImmutableSet.copyOf(artworkFileNames);
        this.artworkFolderNames = ImmutableSet.copyOf(artworkFolderNames);
    }

    @Nullable
    public ImageNode findArtwork(AudioNode audioNode) {
        return Optional.ofNullable(fetchArtworkFromParentFolder(audioNode))
                .orElseGet(() -> fetchArtworkFromParentFolder(audioNode.getParentFolder()));
    }

    @Nullable
    private ImageNode fetchArtworkFromParentFolder(@Nullable Node node) {
        if (node != null) {
            FolderNode folderNode = node.getParentFolder();
            if (folderNode != null) {
                return fetchArtworkFromFolderTree(folderNode);
            }
        }
        return null;
    }

    @Nullable
    private ImageNode fetchArtworkFromFolderTree(FolderNode folderNode) {
        ImageNode artwork = fetchArtworkFromFolder(folderNode);
        if (artwork != null) {
            return artwork;
        } else {
            List<FolderNode> childFolders = folderNode.getChildFolders();
            return childFolders.stream()
                    .filter(this::isFolderArtwork)
                    .map(this::fetchArtworkFromFolder)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() ->
                            childFolders.stream()
                                    .map(this::fetchArtworkFromFolder)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null)
                    );
        }
    }

    @Nullable
    private ImageNode fetchArtworkFromFolder(FolderNode folderNode) {
        List<ImageNode> candidatesBySize = new ArrayList<>();
        return folderNode.getChildImages().stream()
                .filter(this::isImageArtworkBySize)
                .sorted(Comparator.comparing(image -> image.getFile().getName()))
                .peek(candidatesBySize::add)
                .filter(this::isImageArtworkByName)
                .findFirst()
                .orElseGet(() ->
                        candidatesBySize.stream()
                                .findFirst()
                                .orElse(null)
                );
    }

    private boolean isImageArtworkBySize(ImageNode imageNode) {
        ImageSize size;
        try {
            size = imageNode.getImageSize();
        } catch (Exception e) {
            logger.warn("Could not get size of image '{}'.", imageNode.getFile().getName(), e);
            return false;
        }
        double sizeRatio = size.getWidth() / (double) size.getHeight();
        return sizeRatio <= artworkSizeRatioMax && sizeRatio >= artworkSizeRatioMin;
    }

    private boolean isImageArtworkByName(ImageNode imageNode) {
        String name = Files.getNameWithoutExtension(imageNode.getFile().getName()).toLowerCase();
        return artworkFileNames.contains(name);
    }

    private boolean isFolderArtwork(FolderNode folderNode) {
        String name = Files.getNameWithoutExtension(folderNode.getFile().getName()).toLowerCase();
        return artworkFolderNames.contains(name);
    }
}
