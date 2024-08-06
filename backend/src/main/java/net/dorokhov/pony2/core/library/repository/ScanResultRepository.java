package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanResultRepository extends JpaRepository<ScanResult, String> {
}
