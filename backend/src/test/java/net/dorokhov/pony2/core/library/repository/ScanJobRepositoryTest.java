package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.IntegrationTest;
import net.dorokhov.pony2.api.library.domain.ScanJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony2.test.ScanJobFixtures.scanJobFull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScanJobRepositoryTest extends IntegrationTest {
    
    @Autowired
    private ScanJobRepository scanJobRepository;

    @Test
    public void shouldSave() {

        ScanJob scanJob = scanJobRepository.save(scanJobFull());

        assertThat(scanJobRepository.findById(scanJob.getId())).isNotEmpty();
    }
}
