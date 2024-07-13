package net.dorokhov.pony3.core.library.service.file;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumCalculatorTest {
    
    private static final Resource RESOURCE = new ClassPathResource("test.txt");
    private static final byte[] BYTES = {0x74, 0x65, 0x73, 0x74};
    private static final String CHECKSUM = "098f6bcd4621d373cade4e832627b4f6";
    
    private final ChecksumCalculator checksumCalculator = new ChecksumCalculator();

    @Test
    public void shouldCalculateStreamChecksum() throws IOException {
        try (InputStream stream = RESOURCE.getInputStream()) {

            String checksum = checksumCalculator.calculate(stream);

            assertThat(checksum).isEqualTo(CHECKSUM);
        }
    }

    @Test
    public void shouldCalculateBytesChecksum() {

        String checksum = checksumCalculator.calculate(BYTES);

        assertThat(checksum).isEqualTo(CHECKSUM);
    }

    @Test
    public void shouldCalculateFileChecksum() throws IOException {

        File file = RESOURCE.getFile();

        String checksum = checksumCalculator.calculate(file);

        assertThat(checksum).isEqualTo(CHECKSUM);
    }
}
