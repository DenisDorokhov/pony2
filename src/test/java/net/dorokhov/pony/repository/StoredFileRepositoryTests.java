package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.StoredFile;
import net.dorokhov.pony.test.IntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class StoredFileRepositoryTests extends IntegrationTest {
    
    @Autowired
    private StoredFileRepository storedFileRepository;
    
    @Test
    public void shouldSave() {

        StoredFile storedFile = new StoredFile();
        
        storedFile.setName("foobar");
        storedFile.setMimeType("text/plain");
        storedFile.setChecksum("123");
        storedFile.setSize(123L);
        storedFile.setPath("/dev/null");
        storedFile.setTag("someTag");
        storedFile.getMetaData().put("k1", "v1");
        storedFile.getMetaData().put("k2", "v2");
        
        storedFileRepository.save(storedFile);
        
        storedFile = storedFileRepository.findOne(storedFile.getId());
        
        assertThat(storedFile).isNotNull();
        assertThat(storedFile.getName()).isEqualTo("foobar");
        assertThat(storedFile.getMimeType()).isEqualTo("text/plain");
        assertThat(storedFile.getChecksum()).isEqualTo("123");
        assertThat(storedFile.getSize()).isEqualTo(123L);
        assertThat(storedFile.getPath()).isEqualTo("/dev/null");
        assertThat(storedFile.getTag()).isEqualTo(Optional.of("someTag"));
        assertThat(storedFile.getMetaData()).containsOnly(entry("k1", "v1"), entry("k2", "v2"));
    }
}
