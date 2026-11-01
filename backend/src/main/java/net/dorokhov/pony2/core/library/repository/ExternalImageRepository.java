package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.ExternalImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalImageRepository extends JpaRepository<ExternalImage, String> {
}
