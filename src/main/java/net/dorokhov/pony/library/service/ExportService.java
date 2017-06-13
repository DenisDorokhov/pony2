package net.dorokhov.pony.library.service;

import net.dorokhov.pony.library.domain.ExportBundle;

import javax.annotation.Nullable;
import java.io.IOException;

public interface ExportService {
    
    @Nullable
    ExportBundle exportSong(Long id) throws IOException;
    
    @Nullable
    ExportBundle exportAlbum(Long id) throws IOException;
    
    @Nullable
    ExportBundle exportArtist(Long id) throws IOException;
}
