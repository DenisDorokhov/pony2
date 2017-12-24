package net.dorokhov.pony.core.config.repository;

import net.dorokhov.pony.api.config.domain.Config;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String> {
}
