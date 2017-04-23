package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Installation;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InstallationRepository extends PagingAndSortingRepository<Installation, Long> {
}
