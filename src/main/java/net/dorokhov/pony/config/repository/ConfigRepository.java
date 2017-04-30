package net.dorokhov.pony.config.repository;

import net.dorokhov.pony.config.domain.Config;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ConfigRepository extends PagingAndSortingRepository<Config, String> {
}
