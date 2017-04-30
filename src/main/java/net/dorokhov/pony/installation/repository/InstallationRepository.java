package net.dorokhov.pony.installation.repository;

import net.dorokhov.pony.installation.domain.Installation;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InstallationRepository extends PagingAndSortingRepository<Installation, Long> {
}
