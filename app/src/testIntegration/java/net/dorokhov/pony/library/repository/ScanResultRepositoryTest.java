package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.library.domain.ScanResult;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony.fixture.ScanResultFixtures.scanResult;
import static net.dorokhov.pony.api.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ScanResultRepository scanResultRepository;

    @Test
    public void shouldSave() throws Exception {
        ScanResult genre = scanResultRepository.save(scanResult(FULL));
        assertThat(scanResultRepository.findOne(genre.getId())).isNotNull();
    }
}
