package net.dorokhov.pony2.core.library.service.filetree.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LazyFolderNodeTest {
    
    @TempDir
    public Path tempFolder;
    
    @Mock
    private LazyFolderNode.FileNodeResolver fileNodeResolver;
    
    private File folder1;
    private File folder1_1;
    private File folder1_2;
    
    private File image1_1;
    private File image1_2;
    
    private File audio1_1;
    private File audio1_2;

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setUp() throws IOException {
        
        folder1 = tempFolder.resolve("folder1").toFile();
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
    public void shouldNotGoOutOfRootFolder() {

        LazyFolderNode folderNode = new LazyFolderNode(folder1_1, tempFolder.toFile(), fileNodeResolver);

        assertThat(folderNode.getParentFolder()).isNotNull();
        assertThat(folderNode.getParentFolder().getFile()).isEqualTo(folder1);
        assertThat(folderNode.getParentFolder().getParentFolder()).isNotNull();
        assertThat(folderNode.getParentFolder().getParentFolder().getFile()).isEqualTo(tempFolder.toFile());
        assertThat(folderNode.getParentFolder().getParentFolder().getParentFolder()).isNull();
    }

    @Test
    public void shouldCacheParentFolder() {

        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.toFile(), fileNodeResolver);

        FolderNode parentFolder = folderNode.getParentFolder();

        assertThat(folderNode.getParentFolder()).isSameAs(parentFolder);
    }

    @Test
    public void shouldCacheChildNodes() {

        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.toFile(), fileNodeResolver);

        List<Node> childNodes = folderNode.getChildNodes();

        assertThat(childNodes.stream().filter(node -> node instanceof FolderNode).count()).isEqualTo(2);
        assertThat(childNodes.stream().map(Node::getFile)).containsExactlyInAnyOrder(folder1_1, folder1_2);
        assertThat(folderNode.getChildNodes()).isSameAs(childNodes);
    }

    @Test
    public void shouldNotFailWhenFileNodeResolverThrowsIOException() throws IOException {

        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.toFile(), fileNodeResolver);
        when(fileNodeResolver.resolveFileNode(any(), any())).thenThrow(new IOException());

        List<Node> childNodes = folderNode.getChildNodes();

        assertThat(childNodes.stream().map(Node::getFile)).containsExactlyInAnyOrder(folder1_1, folder1_2);
    }

    @Test
    public void shouldNotFailWhenFileIsNotDirectory() {

        LazyFolderNode folderNode = new LazyFolderNode(image1_1, tempFolder.toFile(), fileNodeResolver);

        List<Node> childNodes = folderNode.getChildNodes();

        assertThat(childNodes).isEmpty();
    }

    @Test
    public void shouldGetChildrenByType() throws IOException {
        
        LazyFolderNode folderNode = new LazyFolderNode(folder1, tempFolder.toFile(), fileNodeResolver);
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