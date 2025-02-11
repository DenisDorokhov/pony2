package net.dorokhov.pony2.core.library.repository;

import jakarta.persistence.LockModeType;
import net.dorokhov.pony2.api.library.domain.Playlist;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Playlist> findLockedById(String id);
    List<Playlist> findByUserId(String userId, Sort sort);
    List<Playlist> findByUserIdAndType(String userId, Playlist.Type type, Sort sort);
    void deleteByUserId(String userId);
}
