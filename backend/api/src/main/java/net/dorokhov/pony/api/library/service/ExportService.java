package net.dorokhov.pony.api.library.service;

import net.dorokhov.pony.api.library.domain.ExportBundle;

import javax.annotation.Nullable;

public interface ExportService {
    
    @Nullable
    ExportBundle exportSong(String id);
    
    @Nullable
    ExportBundle exportAlbum(String id);
    
    @Nullable
    ExportBundle exportArtist(String id);
}
