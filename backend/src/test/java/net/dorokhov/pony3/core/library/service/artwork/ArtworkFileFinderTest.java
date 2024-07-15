package net.dorokhov.pony3.core.library.service.artwork;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony3.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony3.core.library.service.image.domain.ImageSize;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtworkFileFinderTest {

    private static final double RATIO_MIN = 0.8;
    private static final double RATIO_MAX = 1.2;
    private static final String[] FILE_NAMES = {"cover"};
    private static final String[] FOLDER_NAMES = {"artwork"};

    private final ArtworkFileFinder artworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, FOLDER_NAMES);

    @Test
    public void shouldNotFindNotExistingArtwork() {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);

        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void shouldFindArtworkInCurrentFolder() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        when(image.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(rootFolder.getChildImages()).thenReturn(ImmutableList.of(image));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void shouldFindArtworkInParentFolder() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        AudioNode audio = mockAudio(new File("root/child/song.mp3"), childFolder);

        when(image.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(rootFolder.getChildImages()).thenReturn(ImmutableList.of(image));
        when(rootFolder.getChildFolders()).thenReturn(ImmutableList.of(childFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void shouldFindArtworkInChildFolder() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        ImageNode image = mockImage(new File("root/child/image.png"), childFolder);

        when(image.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(childFolder.getChildImages()).thenReturn(ImmutableList.of(image));
        when(rootFolder.getChildFolders()).thenReturn(ImmutableList.of(childFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(image);
    }

    @Test
    public void shouldReturnNothingWhenFailing() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        when(image.getImageSize()).thenThrow(new IOException());
        when(rootFolder.getChildImages()).thenReturn(ImmutableList.of(image));

        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void shouldRespectRatio() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode image = mockImage(new File("root/image.png"), rootFolder);

        when(rootFolder.getChildImages()).thenReturn(ImmutableList.of(image));

        when(image.getImageSize()).thenReturn(ImageSize.of(79, 100));
        assertThat(artworkFileFinder.findArtwork(audio)).isNull();

        when(image.getImageSize()).thenReturn(ImageSize.of(121, 100));
        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    @Test
    public void shouldLookForGivenImageNamesFirst() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        ImageNode otherImage = mockImage(new File("root/badCover.png"), rootFolder);
        ImageNode namedImage = mockImage(new File("root/cover.png"), rootFolder);

        when(otherImage.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(namedImage.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(rootFolder.getChildImages()).thenReturn(ImmutableList.of(otherImage, namedImage));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(namedImage);

        ArtworkFileFinder otherArtworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, new String[]{"other"}, FOLDER_NAMES);
        assertThat(otherArtworkFileFinder.findArtwork(audio)).isSameAs(otherImage);
    }

    @Test
    public void shouldLookForGivenFolderNamesFirst() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode otherFolder = mockFolder(new File("root/other"), rootFolder);
        ImageNode otherFolderImage = mockImage(new File("root/other/image.png"), otherFolder);
        FolderNode namedFolder = mockFolder(new File("root/artwork"), rootFolder);
        ImageNode namedFolderImage = mockImage(new File("root/artwork/image.png"), namedFolder);

        when(otherFolderImage.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(namedFolderImage.getImageSize()).thenReturn(ImageSize.of(100, 100));
        when(otherFolder.getChildImages()).thenReturn(ImmutableList.of(otherFolderImage));
        when(namedFolder.getChildImages()).thenReturn(ImmutableList.of(namedFolderImage));
        when(rootFolder.getChildFolders()).thenReturn(ImmutableList.of(otherFolder, namedFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isSameAs(namedFolderImage);

        ArtworkFileFinder otherArtworkFileFinder = new ArtworkFileFinder(RATIO_MIN, RATIO_MAX, FILE_NAMES, new String[]{"other"});
        assertThat(otherArtworkFileFinder.findArtwork(audio)).isSameAs(otherFolderImage);
    }

    @Test
    public void shouldNotFailIfNoArtworkIsFoundInChildFolder() throws IOException {

        FolderNode rootFolder = mockFolder(new File("root"), null);
        AudioNode audio = mockAudio(new File("root/song.mp3"), rootFolder);
        FolderNode childFolder = mockFolder(new File("root/child"), rootFolder);
        ImageNode image = mockImage(new File("root/child/image.png"), childFolder);

        when(image.getImageSize()).thenReturn(ImageSize.of(50, 100));
        when(childFolder.getChildImages()).thenReturn(ImmutableList.of(image));
        when(rootFolder.getChildFolders()).thenReturn(ImmutableList.of(childFolder));

        assertThat(artworkFileFinder.findArtwork(audio)).isNull();
    }

    private FolderNode mockFolder(File file, FolderNode parentFolder) {
        FolderNode folderNode = mock(FolderNode.class);
        when(folderNode.getFile()).thenReturn(file);
        when(folderNode.getParentFolder()).thenReturn(parentFolder);
        return folderNode;
    }

    private ImageNode mockImage(File file, FolderNode parentFolder) {
        ImageNode imageNode = mock(ImageNode.class);
        when(imageNode.getFile()).thenReturn(file);
        when(imageNode.getParentFolder()).thenReturn(parentFolder);
        return imageNode;
    }

    private AudioNode mockAudio(File file, FolderNode parentFolder) {
        AudioNode audioNode = mock(AudioNode.class);
        when(audioNode.getFile()).thenReturn(file);
        when(audioNode.getParentFolder()).thenReturn(parentFolder);
        return audioNode;
    }
}
