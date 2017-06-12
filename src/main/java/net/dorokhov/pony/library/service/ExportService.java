package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.library.service.exception.ObjectNotFoundException;

import java.io.IOException;

public interface ExportService {
    ExportBundle exportSong(Long id) throws ObjectNotFoundException, IOException;
    ExportBundle exportAlbum(Long id) throws ObjectNotFoundException, IOException;
    ExportBundle exportArtist(Long id) throws ObjectNotFoundException, IOException;
}
