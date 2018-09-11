package net.dorokhov.pony.core.library.repository;

import net.dorokhov.pony.api.library.domain.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ScanJobRepository extends JpaRepository<ScanJob, String> {

    List<ScanJob> findByStatusIn(Collection<ScanJob.Status> statuses);
    
    ScanJob findFirstByStatusOrderByUpdateDateDesc(ScanJob.Status status);
}
