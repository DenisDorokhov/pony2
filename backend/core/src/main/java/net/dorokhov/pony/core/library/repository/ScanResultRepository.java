package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanResultRepository extends JpaRepository<ScanResult, String> {
}
