package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, String> {

    List<DiscoveryTask> findByStatus(DiscoveryTask.Status status);

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.id IN :ids
            ORDER BY t.id
            """)
    List<DiscoveryTask> findForEvaluationByIds(List<String> ids);

    long countByJobId(String jobId);

    long countByJobIdAndStatus(String jobId, DiscoveryTask.Status status);
}
