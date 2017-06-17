package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ExportBundle;

import javax.annotation.Nullable;

public interface ExportService {
    
    @Nullable
    ExportBundle exportSong(Long id);
    
    @Nullable
    ExportBundle exportAlbum(Long id);
    
    @Nullable
    ExportBundle exportArtist(Long id);
}
