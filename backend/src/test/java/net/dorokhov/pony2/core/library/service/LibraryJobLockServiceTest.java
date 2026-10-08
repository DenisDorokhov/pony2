package net.dorokhov.pony2.core.library.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryJobLockServiceTest {

    @Test
    void shouldReleaseFromAnotherThreadOnlyOnce() throws InterruptedException {
        LibraryJobLockService service = new LibraryJobLockService();
        LibraryJobLockService.Permit first = service.tryAcquire().orElseThrow();
        assertThat(service.tryAcquire()).isEmpty();

        Thread worker = new Thread(first::close);
        worker.start();
        worker.join(5000);
        assertThat(worker.isAlive()).isFalse();

        try (LibraryJobLockService.Permit second = service.tryAcquire().orElseThrow()) {
            first.close();
            assertThat(service.tryAcquire()).isEmpty();
        }
        try (LibraryJobLockService.Permit third = service.tryAcquire().orElseThrow()) {
            assertThat(service.tryAcquire()).isEmpty();
        }
    }
}
