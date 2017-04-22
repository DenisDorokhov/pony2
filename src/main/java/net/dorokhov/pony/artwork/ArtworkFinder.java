package net.dorokhov.pony.artwork;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import net.dorokhov.pony.filetree.domain.AudioNode;
import net.dorokhov.pony.filetree.domain.FolderNode;
import net.dorokhov.pony.filetree.domain.ImageNode;
import net.dorokhov.pony.image.domain.ImageSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ArtworkFinder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final double artworkSizeRatioMin;
    private final double artworkSizeRatioMax;

    private final Set<String> artworkFileNames;
    private final Set<String> artworkFolderNames;

    public ArtworkFinder(@Value("${pony.artwork.size-ratio.min}") double artworkSizeRatioMin, 
                         @Value("${pony.artwork.size-ratio.max}") double artworkSizeRatioMax,
                         @Value("${pony.artwork.file-names}") String[] artworkFileNames, 
                         @Value("${pony.artwork.folder-names}") String[] artworkFolderNames) {
        this.artworkSizeRatioMin = artworkSizeRatioMin;
        this.artworkSizeRatioMax = artworkSizeRatioMax;
        this.artworkFileNames = ImmutableSet.copyOf(artworkFileNames);
        this.artworkFolderNames = ImmutableSet.copyOf(artworkFolderNames);
    }

    public Optional<ImageNode> findArtwork(AudioNode audioNode) {
        Optional<FolderNode> nextFolderNode = audioNode.getParentFolder();
        while (nextFolderNode.isPresent()) {
            FolderNode folderNode = nextFolderNode.get();
            Optional<ImageNode> artwork = doFetchArtwork(folderNode);
            if (artwork.isPresent()) {
                return artwork;
            } else {
                nextFolderNode = folderNode.getParentFolder();
            }
        }
        return Optional.empty();
    }

    private Optional<ImageNode> doFetchArtwork(FolderNode folderNode) {
        Optional<ImageNode> artwork = fetchArtworkFromFolder(folderNode);
        if (artwork.isPresent()) {
            return artwork;
        } else {
            List<FolderNode> childFolders = folderNode.getChildFolders(false);
            artwork = childFolders.stream()
                    .filter(this::isFolderArtwork)
                    .map(this::fetchArtworkFromFolder)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            if (artwork.isPresent()) {
                return artwork;
            } else {
                return childFolders.stream()
                        .map(this::fetchArtworkFromFolder)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();
            }
        }
    }

    private Optional<ImageNode> fetchArtworkFromFolder(FolderNode folderNode) {
        List<ImageNode> candidatesBySize = new ArrayList<>();
        Optional<ImageNode> artwork = folderNode.getChildImages(false).stream()
                .filter(this::isImageArtworkBySize)
                .sorted(Comparator.comparing(image -> image.getFile().getName()))
                .peek(candidatesBySize::add)
                .filter(this::isImageArtworkByName)
                .findFirst();
        if (artwork.isPresent()) {
            return artwork;
        } else {
            return candidatesBySize.stream().findFirst();
        }
    }

    private boolean isImageArtworkBySize(ImageNode imageNode) {
        ImageSize size;
        try {
            size = imageNode.getImageSize();
        } catch (Exception e) {
            log.warn("Could not get size of image '{}'.", imageNode.getFile().getName(), e);
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
