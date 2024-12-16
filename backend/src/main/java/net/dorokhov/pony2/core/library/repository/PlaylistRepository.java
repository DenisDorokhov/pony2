package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    List<Playlist> findByUserIdAndType(String userId, Playlist.Type type);
}
