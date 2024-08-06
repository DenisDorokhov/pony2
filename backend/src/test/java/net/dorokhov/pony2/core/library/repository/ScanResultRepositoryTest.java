package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.ScanResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony2.api.library.domain.ScanType.FULL;
import static net.dorokhov.pony2.test.ScanResultFixtures.scanResult;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScanResultRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ScanResultRepository scanResultRepository;

    @Test
    public void shouldSave() {

        ScanResult genre = scanResultRepository.save(scanResult(FULL));

        assertThat(scanResultRepository.findById(genre.getId())).isNotEmpty();
    }
}
