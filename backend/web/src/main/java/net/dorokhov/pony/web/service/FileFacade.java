package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.library.domain.ExportBundle;
import net.dorokhov.pony.web.domain.FileDistribution;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

public interface FileFacade {

    FileDistribution getSongDistribution(String songId) throws ObjectNotFoundException;
    FileDistribution getLargeArtworkDistribution(String artworkId) throws ObjectNotFoundException;
    FileDistribution getSmallArtworkDistribution(String artworkId) throws ObjectNotFoundException;

    ExportBundle exportSong(String songId) throws ObjectNotFoundException;
    ExportBundle exportAlbum(String albumId) throws ObjectNotFoundException;
}
