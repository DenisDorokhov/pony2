package net.dorokhov.pony3.core.installation.repository;

import net.dorokhov.pony3.api.installation.domain.Installation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallationRepository extends JpaRepository<Installation, String> {
}
