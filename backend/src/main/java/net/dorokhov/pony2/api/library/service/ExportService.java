package net.dorokhov.pony2.api.library.service;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ExportBundle;

public interface ExportService {
    
    @Nullable
    ExportBundle exportSong(String id);
    
    @Nullable
    ExportBundle exportAlbum(String id);
    
    @Nullable
    ExportBundle exportArtist(String id);
}
