package net.dorokhov.pony.entity;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class StoredFileTests {

    @Test
    public void shouldFailOnNotNullViolation() throws Exception {

        StoredFile storedFile = getStoredFileBuilder().setId(1L).build();

        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setName(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setMimeType(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setChecksum(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setSize(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setPath(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StoredFile.Builder(storedFile).setMetaData(null).build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void songShouldBeBuilt() throws Exception {

        StoredFile storedFile = new StoredFile.Builder()
                .setId(1L)
                .setName("name")
                .setMimeType("mimeType")
                .setChecksum("checksum")
                .setSize(2L)
                .setPath("path")
                .setTag("tag")
                .setMetaData(ImmutableMap.of("k1", "v1"))
                .build();

        assertThat(storedFile.getId()).isEqualTo(Optional.of(1L));
        assertThat(storedFile.getName()).isEqualTo("name");
        assertThat(storedFile.getMimeType()).isEqualTo("mimeType");
        assertThat(storedFile.getChecksum()).isEqualTo("checksum");
        assertThat(storedFile.getSize()).isEqualTo(2L);
        assertThat(storedFile.getPath()).isEqualTo("path");
        assertThat(storedFile.getTag()).isEqualTo(Optional.of("tag"));
        assertThat(storedFile.getMetaData()).containsExactly(entry("k1", "v1"));
    }

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        StoredFile eqStoredFile1 = getStoredFileBuilder().setId(1L).build();
        StoredFile eqStoredFile2 = getStoredFileBuilder().setId(1L).build();
        StoredFile diffStoredFile = getStoredFileBuilder().setId(2L).build();

        assertThat(eqStoredFile1.hashCode()).isEqualTo(eqStoredFile2.hashCode());
        assertThat(eqStoredFile1.hashCode()).isNotEqualTo(diffStoredFile.hashCode());

        assertThat(eqStoredFile1).isEqualTo(eqStoredFile1);
        assertThat(eqStoredFile1).isEqualTo(eqStoredFile2);

        assertThat(eqStoredFile1).isNotEqualTo(diffStoredFile);
        assertThat(eqStoredFile1).isNotEqualTo("foo1");
        assertThat(eqStoredFile1).isNotEqualTo(null);
    }

    private StoredFile.Builder getStoredFileBuilder() {
        return new StoredFile.Builder()
                .setName("")
                .setMimeType("")
                .setChecksum("")
                .setSize(1L)
                .setPath("");
    }
}
