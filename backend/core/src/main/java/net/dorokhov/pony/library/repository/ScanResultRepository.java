package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.api.library.domain.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {
}
