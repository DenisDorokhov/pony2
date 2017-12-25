package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.api.library.domain.ScanJob;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony.test.ScanJobFixtures.scanJobFull;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanJobRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ScanJobRepository scanJobRepository;

    @Test
    public void shouldSave() throws Exception {
        ScanJob scanJob = scanJobRepository.save(scanJobFull());
        assertThat(scanJobRepository.findOne(scanJob.getId())).isNotNull();
    }
}
