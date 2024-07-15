package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanResultRepository extends JpaRepository<ScanResult, String> {
}
