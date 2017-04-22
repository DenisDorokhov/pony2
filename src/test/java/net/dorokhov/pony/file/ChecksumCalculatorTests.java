package net.dorokhov.pony.file;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumCalculatorTests {
    
    private static final Resource RESOURCE = new ClassPathResource("test.txt");
    private static final byte[] BYTES = {0x74, 0x65, 0x73, 0x74};
    private static final String CHECKSUM = "098f6bcd4621d373cade4e832627b4f6";
    
    private ChecksumCalculator checksumCalculator;

    @Before
    public void setUp() throws Exception {
        checksumCalculator = new ChecksumCalculator();
    }

    @Test
    public void calculateStreamChecksum() throws Exception {
        try (InputStream stream = RESOURCE.getInputStream()) {
            String checksum = checksumCalculator.calculate(stream);
            assertThat(checksum).isEqualTo(CHECKSUM);
        }
    }

    @Test
    public void calculateBytesChecksum() throws Exception {
        String checksum = checksumCalculator.calculate(BYTES);
        assertThat(checksum).isEqualTo(CHECKSUM);
    }

    @Test
    public void calculateFileChecksum() throws Exception {
        File file = RESOURCE.getFile();
        String checksum = checksumCalculator.calculate(file);
        assertThat(checksum).isEqualTo(CHECKSUM);
    }
}
