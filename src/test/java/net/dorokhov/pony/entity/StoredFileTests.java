package net.dorokhov.pony.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StoredFileTests {

    @Test
    public void supportEqualityAndHashCode() throws Exception {

        StoredFile eqStoredFile1 = new StoredFile();
        eqStoredFile1.setId(1L);
        StoredFile eqStoredFile2 = new StoredFile();
        eqStoredFile2.setId(1L);
        StoredFile diffStoredFile = new StoredFile();
        diffStoredFile.setId(2L);

        assertThat(eqStoredFile1.hashCode()).isEqualTo(eqStoredFile2.hashCode());
        assertThat(eqStoredFile1.hashCode()).isNotEqualTo(diffStoredFile.hashCode());

        assertThat(eqStoredFile1).isEqualTo(eqStoredFile1);
        assertThat(eqStoredFile1).isEqualTo(eqStoredFile2);

        assertThat(eqStoredFile1).isNotEqualTo(diffStoredFile);
        assertThat(eqStoredFile1).isNotEqualTo("foo1");
        assertThat(eqStoredFile1).isNotEqualTo(null);
    }
}
