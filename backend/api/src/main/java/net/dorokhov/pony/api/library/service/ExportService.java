package net.dorokhov.pony.api.library.service;

import net.dorokhov.pony.api.library.domain.ExportBundle;

import javax.annotation.Nullable;

public interface ExportService {
    
    @Nullable
    ExportBundle exportSong(Long id);
    
    @Nullable
    ExportBundle exportAlbum(Long id);
    
    @Nullable
    ExportBundle exportArtist(Long id);
}
