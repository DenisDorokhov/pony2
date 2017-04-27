package net.dorokhov.pony.artwork;

import net.dorokhov.pony.artwork.domain.ByteSourceArtworkDraft;
import net.dorokhov.pony.artwork.domain.FileArtworkDraft;
import net.dorokhov.pony.artwork.domain.ImageNodeArtworkDraft;
import net.dorokhov.pony.entity.Artwork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ArtworkService {

    long getCount();
    long getCountByMinimalDate(LocalDateTime minimalDate);

    long getTotalSize();

    Optional<Artwork> getById(Long id);

    Page<Artwork> getAll(Pageable pageable);

    Optional<File> getLargeImageFile(Long id);
    Optional<File> getSmallImageFile(Long id);
    
    Artwork getOrSave(ByteSourceArtworkDraft draft) throws IOException;
    Artwork getOrSave(FileArtworkDraft draft) throws IOException;
    Artwork getOrSave(ImageNodeArtworkDraft draft) throws IOException;

    void delete(Long id);
}
