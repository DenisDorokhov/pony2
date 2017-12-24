package net.dorokhov.pony.core.library.service.filetree.domain;

import net.dorokhov.pony.core.library.service.filetree.domain.AudioNode;
import net.dorokhov.pony.core.library.service.filetree.domain.FolderNode;
import net.dorokhov.pony.core.library.service.filetree.domain.ImageNode;
import net.dorokhov.pony.core.library.service.filetree.domain.MutableFolderNode;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class MutableFolderNodeTest {

    @Test
    public void shouldReturnImmutableChildImages() throws Exception {
        MutableFolderNode folderNode = new MutableFolderNode(mock(File.class), null);
        folderNode.getMutableChildImages().add(mock(ImageNode.class));
        assertThat(folderNode.getChildImages()).hasSize(1);
        assertThatThrownBy(() -> folderNode.getChildImages().add(mock(ImageNode.class)));
    }

    @Test
    public void shouldReturnImmutableChildAudios() throws Exception {
        MutableFolderNode folderNode = new MutableFolderNode(mock(File.class), null);
        folderNode.getMutableChildAudios().add(mock(AudioNode.class));
        assertThat(folderNode.getChildAudios()).hasSize(1);
        assertThatThrownBy(() -> folderNode.getChildAudios().add(mock(AudioNode.class)));
    }

    @Test
    public void shouldReturnImmutableChildFolders() throws Exception {
        MutableFolderNode folderNode = new MutableFolderNode(mock(File.class), null);
        folderNode.getMutableChildFolders().add(mock(MutableFolderNode.class));
        assertThat(folderNode.getChildFolders()).hasSize(1);
        assertThatThrownBy(() -> folderNode.getChildFolders().add(mock(FolderNode.class)));
    }
}