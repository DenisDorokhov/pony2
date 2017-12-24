package net.dorokhov.pony.core.installation.repository;

import net.dorokhov.pony.api.installation.domain.Installation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, Long> {
}
