package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.PlaybackHistorySong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybackHistorySongRepository extends JpaRepository<PlaybackHistorySong, String> {
    long countByUserId(String userId);
    Page<PlaybackHistorySong> findByUserId(String userId, Pageable pageable);
    void deleteBySongId(String songId);
    void deleteByUserId(String userId);
}
