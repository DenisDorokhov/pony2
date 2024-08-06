package net.dorokhov.pony2.core.config.repository;

import net.dorokhov.pony2.api.config.domain.Config;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String> {
}
