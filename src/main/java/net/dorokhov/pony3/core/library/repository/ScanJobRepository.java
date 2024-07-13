package net.dorokhov.pony3.core.library.repository;

import net.dorokhov.pony3.api.library.domain.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScanJobRepository extends JpaRepository<ScanJob, String> {

    List<ScanJob> findByStatusIn(Collection<ScanJob.Status> statuses);
    
    Optional<ScanJob> findFirstByStatusOrderByUpdateDateDesc(ScanJob.Status status);
}
