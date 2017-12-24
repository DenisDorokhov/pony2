package net.dorokhov.pony.installation.repository;

import net.dorokhov.pony.api.installation.domain.Installation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, Long> {
}
