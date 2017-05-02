package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.library.domain.ScanResult;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ScanResultRepository extends PagingAndSortingRepository<ScanResult, Long> {
}
