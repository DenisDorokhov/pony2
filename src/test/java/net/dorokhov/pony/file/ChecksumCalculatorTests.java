package net.dorokhov.pony.file;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumCalculatorTests {
    
    private static final Resource TEST_RESOURCE = new ClassPathResource("test.txt");
    private static final String CHECKSUM = "d8e8fca2dc0f896fd7cb4cb0031ba249";
    
    private ChecksumCalculator checksumCalculator;

    @Before
    public void setUp() throws Exception {
        checksumCalculator = new ChecksumCalculator();
    }

    @Test
    public void calculateStreamChecksum() throws Exception {
        try (InputStream stream = TEST_RESOURCE.getInputStream()) {
            String checksum = checksumCalculator.calculate(stream);
            assertThat(checksum).isEqualTo(CHECKSUM);
        }
    }

    @Test
    public void calculateFileChecksum() throws Exception {
        File file = TEST_RESOURCE.getFile();
        String checksum = checksumCalculator.calculate(file);
        assertThat(checksum).isEqualTo(CHECKSUM);
    }
}
