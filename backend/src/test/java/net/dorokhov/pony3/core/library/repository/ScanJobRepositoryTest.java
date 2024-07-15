package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.library.domain.ScanJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.dorokhov.pony3.test.ScanJobFixtures.scanJobFull;
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
