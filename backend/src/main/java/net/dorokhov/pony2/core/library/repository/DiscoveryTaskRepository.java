package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, String> {

    List<DiscoveryTask> findByStatus(DiscoveryTask.Status status);

    long countByJobId(String jobId);

    long countByJobIdAndStatus(String jobId, DiscoveryTask.Status status);
}
