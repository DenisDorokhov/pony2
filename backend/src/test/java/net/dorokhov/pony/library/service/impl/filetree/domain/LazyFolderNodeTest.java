package net.dorokhov.pony.library.service.impl.filetree.domain;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LazyFolderNodeTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Mock
    private LazyFolderNode.FileNodeResolver fileNodeResolver;
    
    private File folder1;
    private File folder1_1;
    private File folder1_2;
    
    private File image1_1;
    private File image1_2;
    
    private File audio1_1;
    private File audio1_2;

    @Before
    public void setUp() throws Exception {
        
        folder1 = new File(tempFolder.getRoot(), "folder1");
        folder1.mkdir();
        
        folder1_1 = new File(folder1, "folder1_1");
        folder1_1.mkdir();
        
        folder1_2 = new File(folder1, "folder1_2");
        folder1_2.mkdir();
        
        image1_1 = new File(folder1, "image1_1");
        image1_1.createNewFile();
        
        image1_2 = new File(folder1, "image1_2");
        image1_2.createNewFile();
        
        audio1_1 = new File(folder1, "audio1_1");
        audio1_1.createNewFile();
        
        audio1_2 = new File(folder1, "audio1_2");
        audio1_2.createNewFile();
    }

    @Test
    public void shouldNotGoOutOfRootFolder() throws Exception {
        LazyFolderNode folderNode = new LazyFolderNode(folder1_1, tempFolder.getRoot(), fileNodeResolver);
        assertThat(folderNode.getParentFolder()).isNotNull();
        assertThat(folderNode.getParentFolder().getFile()).isEqualTo(folder1);
        assertThat(folderNode.getParentFolder().getParentFolder()).isNotNull();
        assertThat(folderNode.getParentFolder().getParentFolder().getFile()).isEqualTo(tempFolder.getRoot());
        assertThat(folderNode.getParentFolder().getParentFolder().getParentFolder()).isNull();
    }

    @Test
    public void shouldCacheParentFolder() throws Exception {
        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.getRoot(), fileNodeResolver);
        FolderNode parentFolder = folderNode.getParentFolder();
        assertThat(folderNode.getParentFolder()).isSameAs(parentFolder);
    }

    @Test
    public void shouldCacheChildNodes() throws Exception {
        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.getRoot(), fileNodeResolver);
        List<Node> childNodes = folderNode.getChildNodes();
        assertThat(childNodes.stream().filter(node -> node instanceof FolderNode).count()).isEqualTo(2);
        assertThat(childNodes.stream().map(Node::getFile)).containsExactlyInAnyOrder(folder1_1, folder1_2);
        assertThat(folderNode.getChildNodes()).isSameAs(childNodes);
    }

    @Test
    public void shouldNotFailWhenFileNodeResolverThrowsIOException() throws Exception {
        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.getRoot(), fileNodeResolver);
        when(fileNodeResolver.resolveFileNode(any(), any())).thenThrow(new IOException());
        List<Node> childNodes = folderNode.getChildNodes();
        assertThat(childNodes.stream().map(Node::getFile)).containsExactlyInAnyOrder(folder1_1, folder1_2);
    }

    @Test
    public void shouldNotFailWhenFileIsNotDirectory() throws Exception {
        LazyFolderNode folderNode = new LazyFolderNode(image1_1, tempFolder.getRoot(), fileNodeResolver);
        List<Node> childNodes = folderNode.getChildNodes();
        assertThat(childNodes).isEmpty();
    }

    @Test
    public void shouldGetChildrenByType() throws Exception {
        
        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.getRoot(), fileNodeResolver);
        when(fileNodeResolver.resolveFileNode(any(), any())).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            if (file.getName().startsWith("image")) {
                ImageNode imageNode = mock(ImageNode.class);
                when(imageNode.getFile()).thenReturn(invocation.getArgument(0));
                return imageNode;
            } else if (file.getName().startsWith("audio")) {
                AudioNode audioNode = mock(AudioNode.class);
                when(audioNode.getFile()).thenReturn(invocation.getArgument(0));
                return audioNode;
            }
            return null;
        });
        
        assertThat(folderNode.getChildImages().stream().map(Node::getFile)).containsExactlyInAnyOrder(image1_1, image1_2);
        assertThat(folderNode.getChildAudios().stream().map(Node::getFile)).containsExactlyInAnyOrder(audio1_1, audio1_2);
        assertThat(folderNode.getChildFolders().stream().map(Node::getFile)).containsExactlyInAnyOrder(folder1_1, folder1_2);
    }
}