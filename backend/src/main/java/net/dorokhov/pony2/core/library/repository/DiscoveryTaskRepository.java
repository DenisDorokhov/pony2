package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryTaskRepository extends JpaRepository<DiscoveryTask, String> {

    long countByJobId(String jobId);

    long countByJobIdAndStatus(String jobId, DiscoveryTask.Status status);
}
