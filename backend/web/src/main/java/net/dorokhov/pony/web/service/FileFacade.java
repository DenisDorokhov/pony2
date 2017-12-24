package net.dorokhov.pony.web.service;

import net.dorokhov.pony.api.library.domain.ExportBundle;
import net.dorokhov.pony.web.domain.FileDistribution;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;

public interface FileFacade {

    FileDistribution getSongDistribution(Long songId) throws ObjectNotFoundException;
    FileDistribution getLargeArtworkDistribution(Long artworkId) throws ObjectNotFoundException;
    FileDistribution getSmallArtworkDistribution(Long artworkId) throws ObjectNotFoundException;

    ExportBundle exportSong(Long songId) throws ObjectNotFoundException;
    ExportBundle exportAlbum(Long albumId) throws ObjectNotFoundException;
}
