package net.dorokhov.pony.artwork;

import net.dorokhov.pony.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ArtworkService {

    long getCountByTag(String tag);
    long getCountByTagAndMinimalDate(String tag, LocalDateTime minimalDate);

    long getSizeByTag(String tag);

    Optional<Artwork> getById(Long id);

    Page<Artwork> getByTag(String tag, Pageable pageable);

    Optional<File> getLargeImageFile(Long id);
    Optional<File> getSmallImageFile(Long id);
    
    Artwork getOrSave(SaveByteSourceArtworkCommand command) throws IOException;
    Artwork getOrSave(SaveFileArtworkCommand command) throws IOException;
    Artwork getOrSave(SaveImageNodeArtworkCommand command) throws IOException;

    void delete(Long id);
}
