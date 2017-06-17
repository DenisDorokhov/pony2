package net.dorokhov.pony.config.repository;

import net.dorokhov.pony.config.domain.Config;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<Config, String> {
}
