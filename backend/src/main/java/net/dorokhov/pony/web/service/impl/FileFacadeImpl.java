package net.dorokhov.pony.web.service.impl;

import net.dorokhov.pony.library.domain.*;
import net.dorokhov.pony.library.service.ExportService;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.web.domain.FileDistribution;
import net.dorokhov.pony.web.service.FileFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
public class FileFacadeImpl implements FileFacade {
    
    private final LibraryService libraryService;
    private final ExportService exportService;

    public FileFacadeImpl(LibraryService libraryService, ExportService exportService) {
        this.libraryService = libraryService;
        this.exportService = exportService;
    }

    @Override
    @Transactional(readOnly = true)
    public FileDistribution getSongDistribution(Long songId) throws ObjectNotFoundException {
        Song song = libraryService.getSongById(songId);
        if (song == null) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
        File file = song.getFile();
        return new FileDistribution(file, file.getName(), song.getFileType().getMimeType(),
                song.getUpdateDate() != null ? song.getUpdateDate() : song.getCreationDate());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDistribution getLargeArtworkDistribution(Long artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = getArtworkFiles(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getLargeFile();
        return new FileDistribution(file, file.getName(), artwork.getMimeType(), artwork.getDate());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDistribution getSmallArtworkDistribution(Long artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = getArtworkFiles(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getSmallFile();
        return new FileDistribution(file, file.getName(), artwork.getMimeType(), artwork.getDate());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportBundle exportSong(Long songId) throws ObjectNotFoundException {
        ExportBundle exportBundle = exportService.exportSong(songId);
        if (exportBundle == null) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
        return exportBundle;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportBundle exportAlbum(Long albumId) throws ObjectNotFoundException {
        ExportBundle exportBundle = exportService.exportAlbum(albumId);
        if (exportBundle == null) {
            throw new ObjectNotFoundException(Album.class, albumId);
        }
        return exportBundle;
    }

    private ArtworkFiles getArtworkFiles(Long artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId);
        if (artworkFiles == null) {
            throw new ObjectNotFoundException(Artwork.class, artworkId);
        }
        return artworkFiles;
    }
}
