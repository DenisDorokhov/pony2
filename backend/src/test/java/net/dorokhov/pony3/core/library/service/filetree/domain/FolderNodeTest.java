package net.dorokhov.pony3.core.library.service.filetree.domain;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FolderNodeTest {

    @Test
    public void shouldGetNotIgnoredChildImagesRecursively() {

        FolderNode folder1_1_1 = mock(FolderNode.class);
        ImageNode image1_1_1_1 = mock(ImageNode.class);
        when(folder1_1_1.getChildImages()).thenReturn(ImmutableList.of(image1_1_1_1));
        when(folder1_1_1.getChildFolders()).thenReturn(emptyList());
        when(folder1_1_1.getNotIgnoredChildImagesRecursively()).thenCallRealMethod();

        FolderNode folder1_1 = mock(FolderNode.class);
        ImageNode image1_1_1 = mock(ImageNode.class);
        when(folder1_1.getChildImages()).thenReturn(ImmutableList.of(image1_1_1));
        when(folder1_1.getChildFolders()).thenReturn(ImmutableList.of(folder1_1_1));
        when(folder1_1.getNotIgnoredChildImagesRecursively()).thenCallRealMethod();

        FolderNode folder1_2 = mock(FolderNode.class);
        ImageNode image1_2_1 = mock(ImageNode.class);
        when(folder1_2.getChildImages()).thenReturn(ImmutableList.of(image1_2_1));
        when(folder1_2.getChildFolders()).thenReturn(emptyList());
        when(folder1_2.getNotIgnoredChildImagesRecursively()).thenCallRealMethod();

        FolderNode folder1 = mock(FolderNode.class);
        ImageNode image1_1 = mock(ImageNode.class);
        ImageNode image1_2 = mock(ImageNode.class);
        when(folder1.getChildImages()).thenReturn(ImmutableList.of(image1_1, image1_2));
        when(folder1.getChildFolders()).thenReturn(ImmutableList.of(folder1_1, folder1_2));
        when(folder1.getNotIgnoredChildImagesRecursively()).thenCallRealMethod();

        List<ImageNode> imageNodes = folder1.getNotIgnoredChildImagesRecursively();

        assertThat(imageNodes).containsExactlyInAnyOrder(image1_1_1_1, image1_1_1, image1_2_1, image1_1, image1_2);
    }

    @Test
    public void shouldGetNotIgnoredChildAudiosRecursively() {

        FolderNode folder1_1_1 = mock(FolderNode.class);
        AudioNode audio1_1_1_1 = mock(AudioNode.class);
        when(folder1_1_1.getChildAudios()).thenReturn(ImmutableList.of(audio1_1_1_1));
        when(folder1_1_1.getChildFolders()).thenReturn(emptyList());
        when(folder1_1_1.getNotIgnoredChildAudiosRecursively()).thenCallRealMethod();

        FolderNode folder1_1 = mock(FolderNode.class);
        AudioNode audio1_1_1 = mock(AudioNode.class);
        when(folder1_1.getChildAudios()).thenReturn(ImmutableList.of(audio1_1_1));
        when(folder1_1.getChildFolders()).thenReturn(ImmutableList.of(folder1_1_1));
        when(folder1_1.getNotIgnoredChildAudiosRecursively()).thenCallRealMethod();

        FolderNode folder1_2 = mock(FolderNode.class);
        AudioNode audio1_2_1 = mock(AudioNode.class);
        when(folder1_2.getChildAudios()).thenReturn(ImmutableList.of(audio1_2_1));
        when(folder1_2.getChildFolders()).thenReturn(emptyList());
        when(folder1_2.getNotIgnoredChildAudiosRecursively()).thenCallRealMethod();

        FolderNode folder1 = mock(FolderNode.class);
        AudioNode audio1_1 = mock(AudioNode.class);
        AudioNode audio1_2 = mock(AudioNode.class);
        when(folder1.getChildAudios()).thenReturn(ImmutableList.of(audio1_1, audio1_2));
        when(folder1.getChildFolders()).thenReturn(ImmutableList.of(folder1_1, folder1_2));
        when(folder1.getNotIgnoredChildAudiosRecursively()).thenCallRealMethod();

        List<AudioNode> AudioNodes = folder1.getNotIgnoredChildAudiosRecursively();

        assertThat(AudioNodes).containsExactlyInAnyOrder(audio1_1_1_1, audio1_1_1, audio1_2_1, audio1_1, audio1_2);
    }
}