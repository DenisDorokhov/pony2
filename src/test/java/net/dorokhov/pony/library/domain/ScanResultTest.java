package net.dorokhov.pony.library.domain;

import org.junit.Test;

import static net.dorokhov.pony.fixture.ScanResultFixtures.scanResultBuilder;
import static net.dorokhov.pony.library.domain.ScanType.FULL;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTest {

    @Test
    public void shouldSupportEqualityAndHashCode() throws Exception {

        ScanResult eqScanResult1 = scanResultBuilder(FULL).id(1L).build();
        ScanResult eqScanResult2 = scanResultBuilder(FULL).id(1L).build();
        ScanResult diffScanResult = scanResultBuilder(FULL).id(2L).build();

        assertThat(eqScanResult1.hashCode()).isEqualTo(eqScanResult2.hashCode());
        assertThat(eqScanResult1.hashCode()).isNotEqualTo(diffScanResult.hashCode());

        assertThat(eqScanResult1).isEqualTo(eqScanResult1);
        assertThat(eqScanResult1).isEqualTo(eqScanResult2);

        assertThat(eqScanResult1).isNotEqualTo(diffScanResult);
        assertThat(eqScanResult1).isNotEqualTo("foo1");
        assertThat(eqScanResult1).isNotEqualTo(null);
    }
}