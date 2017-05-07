package net.dorokhov.pony.library.service.impl.file;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumCalculatorTest {
    
    private static final Resource RESOURCE = new ClassPathResource("test.txt");
    private static final byte[] BYTES = {0x74, 0x65, 0x73, 0x74};
    private static final String CHECKSUM = "098f6bcd4621d373cade4e832627b4f6";
    
    private final ChecksumCalculator checksumCalculator = new ChecksumCalculator();

    @Test
    public void shouldCalculateStreamChecksum() throws Exception {
        try (InputStream stream = RESOURCE.getInputStream()) {
            String checksum = checksumCalculator.calculate(stream);
            assertThat(checksum).isEqualTo(CHECKSUM);
        }
    }

    @Test
    public void shouldCalculateBytesChecksum() throws Exception {
        String checksum = checksumCalculator.calculate(BYTES);
        assertThat(checksum).isEqualTo(CHECKSUM);
    }

    @Test
    public void shouldCalculateFileChecksum() throws Exception {
        File file = RESOURCE.getFile();
        String checksum = checksumCalculator.calculate(file);
        assertThat(checksum).isEqualTo(CHECKSUM);
    }
}
