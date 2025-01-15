package net.dorokhov.pony2.core.library.repository;

import net.dorokhov.pony2.api.library.domain.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, String> {
    void deleteBySongId(String songId);
    List<PlaylistSong> findBySongId(String id);
}
