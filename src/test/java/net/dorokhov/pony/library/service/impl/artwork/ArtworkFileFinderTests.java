package net.dorokhov.pony.library.service.impl.artwork;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.library.service.impl.filetree.domain.AudioNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.FolderNode;
import net.dorokhov.pony.library.service.impl.filetree.domain.ImageNode;
import net.dorokhov.pony.library.service.impl.image.domain.ImageSize;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ArtworkFileFinderTests {

    private static final double RATIO_MIN = 0.8;
    private static final double RATIO_MAX = 1.2;
    private static final String[] FILE_NAMES = {"cover"};
    private static final String[] FOLDER_NAMES = {"artwork"};

    private ArtworkFileFinder artworkFileFinder;

    @Before
    public void setUp() throws Exception {
        artworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, FOLDER_NAMES);
    }

    @Test
    public void notExistingArtwork() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);

        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void findInCurrentFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(image.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableList.of(image));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void findInParentFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        AudioNode audio = mockAudio(new File("root/child/song.mp3"), childFolder);

        given(image.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableList.of(image));
        given(rootFolder.getChildFolders(false)).willReturn(ImmutableList.of(childFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void findInChildFolder() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        ImageNode image = mockImage(new File("root/child/image.png"), childFolder);

        given(image.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(childFolder.getChildImages(false)).willReturn(ImmutableList.of(image));
        given(rootFolder.getChildFolders(false)).willReturn(ImmutableList.of(childFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void returnNothingWhenFailing() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(image.getImageSize()).willThrow(new IOException());
        given(rootFolder.getChildImages(false)).willReturn(ImmutableList.of(image));

        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void respectRatio() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        given(rootFolder.getChildImages(false)).willReturn(ImmutableList.of(image));

        given(image.getImageSize()).willReturn(ImageSize.of(79, 100));
        assertThat(artworkFileFinder.findArtwork(audio)).isNull();

        given(image.getImageSize()).willReturn(ImageSize.of(121, 100));
        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void lookForGivenImageNamesFirst() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode otherImage = mockImage(new File("root/badCover.png"), rootFolder);
        ImageNode namedImage = mockImage(new File("root/cover.png"), rootFolder);

        given(otherImage.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(namedImage.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(rootFolder.getChildImages(false)).willReturn(ImmutableList.of(otherImage, namedImage));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(namedImage);

        artworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, new String[]{"other"}, FOLDER_NAMES);
        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(otherImage);
    }

    @Test
    public void lookForGivenFolderNamesFirst() throws Exception {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode otherFolder = mockFolder(new File("root/other"), rootFolder);
        ImageNode otherFolderImage = mockImage(new File("root/other/image.png"), otherFolder);
        FolderNode namedFolder = mockFolder(new File("root/artwork"), rootFolder);
        ImageNode namedFolderImage = mockImage(new File("root/artwork/image.png"), namedFolder);

        given(otherFolderImage.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(namedFolderImage.getImageSize()).willReturn(ImageSize.of(100, 100));
        given(otherFolder.getChildImages(false)).willReturn(ImmutableList.of(otherFolderImage));
        given(namedFolder.getChildImages(false)).willReturn(ImmutableList.of(namedFolderImage));
        given(rootFolder.getChildFolders(false)).willReturn(ImmutableList.of(otherFolder, namedFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(namedFolderImage);

        artworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, new String[]{"other"});
        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(otherFolderImage);
    }

    private FolderNode mockFolder(File file, FolderNode parentFolder) {
        FolderNode folderNode = mock(FolderNode.class);
        given(folderNode.getFile()).willReturn(file);
        given(folderNode.getParentFolder()).willReturn(parentFolder);
        return folderNode;
    }

    private ImageNode mockImage(File file, FolderNode parentFolder) {
        ImageNode imageNode = mock(ImageNode.class);
        given(imageNode.getFile()).willReturn(file);
        given(imageNode.getParentFolder()).willReturn(parentFolder);
        return imageNode;
    }

    private AudioNode mockAudio(File file, FolderNode parentFolder) {
        AudioNode audioNode = mock(AudioNode.class);
        given(audioNode.getFile()).willReturn(file);
        given(audioNode.getParentFolder()).willReturn(parentFolder);
        return audioNode;
    }
}
