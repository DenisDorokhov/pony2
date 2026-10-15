package net.dorokhov.pony2.core.library.repository;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT t FROM DiscoveryTask t
            WHERE t.job.id = :jobId AND (:type IS NULL OR t.type = :type)
            ORDER BY t.updateDate DESC NULLS LAST, t.id DESC
            """)
    Page<DiscoveryTask> findByJobIdAndType(String jobId, @Nullable DiscoveryTaskType type, Pageable pageable);

    long countByJobIdAndStatus(String jobId, DiscoveryTask.Status status);
}
