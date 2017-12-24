package net.dorokhov.pony.library.repository;

import net.dorokhov.pony.api.library.domain.ScanJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {

    Page<ScanJob> findByStatusIn(Collection<ScanJob.Status> statuses, Pageable pageable);
    
    ScanJob findFirstByStatusOrderByUpdateDateDesc(ScanJob.Status status);
}
