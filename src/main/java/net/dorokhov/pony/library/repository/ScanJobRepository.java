package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.library.domain.ScanJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collection;

public interface ScanJobRepository extends PagingAndSortingRepository<ScanJob, Long> {

    Page<ScanJob> findByStatusIn(Collection<ScanJob.Status> statuses, Pageable pageable);
}
