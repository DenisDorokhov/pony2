package net.dorokhov.pony2.core.installation.repository;

import net.dorokhov.pony2.api.installation.domain.Installation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, String> {
}
