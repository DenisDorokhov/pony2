package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.library.service.exception.AlbumNotFoundException;
import net.dorokhov.pony.library.service.exception.ArtistNotFoundException;
import net.dorokhov.pony.library.service.exception.SongNotFoundException;

import java.io.IOException;

public interface ExportService {
    ExportBundle exportSong(Long id) throws SongNotFoundException, IOException;
    ExportBundle exportAlbum(Long id) throws AlbumNotFoundException, IOException;
    ExportBundle exportArtist(Long id) throws ArtistNotFoundException, IOException;
}
