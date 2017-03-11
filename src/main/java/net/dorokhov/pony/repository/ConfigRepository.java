package net.dorokhov.pony.repository;

import net.dorokhov.pony.entity.Config;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ConfigRepository extends PagingAndSortingRepository<Config, String> {
}
