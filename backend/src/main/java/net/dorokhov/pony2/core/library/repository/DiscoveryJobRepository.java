package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.DiscoveryJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DiscoveryJobRepository extends JpaRepository<DiscoveryJob, String> {

    List<DiscoveryJob> findByStatusIn(Collection<DiscoveryJob.Status> statuses);
}
