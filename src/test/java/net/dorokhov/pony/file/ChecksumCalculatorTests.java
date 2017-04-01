package net.dorokhov.pony.file;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumCalculatorTests {
    
    private ChecksumCalculator checksumCalculator;

    @Before
    public void setUp() {
        checksumCalculator = new ChecksumCalculator();
    }

    @Test
    public void calculateStreamChecksum() throws Exception {

        InputStream stream = new ClassPathResource("test.txt").getInputStream();
        String checksum = checksumCalculator.calculate(stream);
        
        assertThat(checksum).isEqualTo("d8e8fca2dc0f896fd7cb4cb0031ba249");
    }

    @Test
    public void calculateFileChecksum() throws Exception {

        File file = new ClassPathResource("test.txt").getFile();
        String checksum = checksumCalculator.calculate(file);
        
        assertThat(checksum).isEqualTo("d8e8fca2dc0f896fd7cb4cb0031ba249");
    }
}
