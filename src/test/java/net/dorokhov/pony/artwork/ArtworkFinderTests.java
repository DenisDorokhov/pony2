package net.dorokhov.pony.artwork;

import com.google.common.collect.ImmutableSet;
import net.dorokhov.pony.filetree.AudioNode;
import net.dorokhov.pony.filetree.FolderNode;
import net.dorokhov.pony.filetree.ImageNode;
import net.dorokhov.pony.image.ImageSize;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ArtworkFinderTests {

    private static final double RATIO_MIN = 0.8;
    private static final double RATIO_MAX = 1.2;
    private static final String[] FILE_NAMES = {"cover"};
    private static final String[] FOLDER_NAMES = {"artwork"};

    private ArtworkFinder artworkFinder;

    @Before
    public void setUp() throws Exception {
        artworkFinder = new ArtworkFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, FOLDER_NAMES);
    }

    @Test
    public void notExistingArtwork() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).isEmpty();
    }

    @Test
    public void findInCurrentFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(image.getImageSize()).willReturn(new ImageSize(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableSet.of(image));

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(image);
    }

    @Test
    public void findInParentFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        AudioNode audio = mockAudio(new File("root/child/song.mp3"), childFolder);

        given(image.getImageSize()).willReturn(new ImageSize(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableSet.of(image));
        given(rootFolder.getChildFolders(false)).willReturn(ImmutableSet.of(childFolder));

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(image);
    }

    @Test
    public void findInChildFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        ImageNode image = mockImage(new File("root/child/image.png"), childFolder);

        given(image.getImageSize()).willReturn(new ImageSize(100, 100));
        given(childFolder.getChildImages(false)).willReturn(ImmutableSet.of(image));
        given(rootFolder.getChildFolders(false)).willReturn(ImmutableSet.of(childFolder));

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(image);
    }

    @Test
    public void returnNothingWhenFailing() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(image.getImageSize()).willThrow(new IOException());
        given(rootFolder.getChildImages(false)).willReturn(ImmutableSet.of(image));

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).isEmpty();
    }

    @Test
    public void respectRatio() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(rootFolder.getChildImages(false)).willReturn(ImmutableSet.of(image));

        Optional<ImageNode> artwork;

        given(image.getImageSize()).willReturn(new ImageSize(79, 100));
        artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).isEmpty();

        given(image.getImageSize()).willReturn(new ImageSize(121, 100));
        artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).isEmpty();
    }

    @Test
    public void lookForGivenImageNamesFirst() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode otherImage = mockImage(new File("root/badCover.png"), rootFolder);
        ImageNode namedImage = mockImage(new File("root/cover.png"), rootFolder);

        given(otherImage.getImageSize()).willReturn(new ImageSize(100, 100));
        given(namedImage.getImageSize()).willReturn(new ImageSize(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableSet.of(otherImage, namedImage));

        Optional<ImageNode> artwork;
        
        artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(namedImage);

        artworkFinder = new ArtworkFinder(RATIO_MIN, RATIO_MAX, new String[]{"other"}, FOLDER_NAMES);
        artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(otherImage);
    }

    @Test
    public void lookForGivenFolderNamesFirst() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode otherFolder = mockFolder(new File("root/other"), rootFolder);
        ImageNode otherFolderImage = mockImage(new File("root/other/image.png"), otherFolder);
        FolderNode namedFolder = mockFolder(new File("root/artwork"), rootFolder);
        ImageNode namedFolderImage = mockImage(new File("root/artwork/image.png"), namedFolder);

        given(otherFolderImage.getImageSize()).willReturn(new ImageSize(100, 100));
        given(namedFolderImage.getImageSize()).willReturn(new ImageSize(100, 100));
        given(otherFolder.getChildImages(false)).willReturn(ImmutableSet.of(otherFolderImage));
        given(namedFolder.getChildImages(false)).willReturn(ImmutableSet.of(namedFolderImage));
        given(rootFolder.getChildFolders(false)).willReturn(new LinkedHashSet<>(Arrays.asList(otherFolder, namedFolder)));

        Optional<ImageNode> artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(namedFolderImage);

        artworkFinder = new ArtworkFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, new String[]{"other"});
        artwork = artworkFinder.findArtwork(audio);
        assertThat(artwork).hasValue(otherFolderImage);
    }

    private FolderNode mockFolder(File file, FolderNode parentFolder) {
        FolderNode folderNode = mock(FolderNode.class);
        given(folderNode.getFile()).willReturn(file);
        given(folderNode.getParentFolder()).willReturn(Optional.ofNullable(parentFolder));
        return folderNode;
    }

    private ImageNode mockImage(File file, FolderNode parentFolder) {
        ImageNode imageNode = mock(ImageNode.class);
        given(imageNode.getFile()).willReturn(file);
        given(imageNode.getParentFolder()).willReturn(Optional.ofNullable(parentFolder));
        return imageNode;
    }

    private AudioNode mockAudio(File file, FolderNode parentFolder) {
        AudioNode audioNode = mock(AudioNode.class);
        given(audioNode.getFile()).willReturn(file);
        given(audioNode.getParentFolder()).willReturn(Optional.ofNullable(parentFolder));
        return audioNode;
    }
}
