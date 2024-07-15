package net.dorokhov.pony3.core.config.repository;

import net.dorokhov.pony3.api.config.domain.Config;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String> {
}
