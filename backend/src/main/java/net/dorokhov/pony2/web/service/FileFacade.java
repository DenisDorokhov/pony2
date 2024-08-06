package net.dorokhov.pony2.web.service;

import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.service.ExportService;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.web.dto.FileDistribution;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
public class FileFacade {
    
    private final LibraryService libraryService;
    private final ExportService exportService;

    public FileFacade(LibraryService libraryService, ExportService exportService) {
        this.libraryService = libraryService;
        this.exportService = exportService;
    }

    @Transactional(readOnly = true)
    public FileDistribution getSongDistribution(String songId) throws ObjectNotFoundException {
        Song song = libraryService.getSongById(songId).orElse(null);
        if (song == null) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
        File file = song.getFile();
        if (!file.exists()) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
        return new FileDistribution()
                .setFile(file)
                .setName(file.getName())
                .setMimeType(song.getFileType().getMimeType())
                .setModificationDate(song.getUpdateDate() != null ? song.getUpdateDate() : song.getCreationDate());
    }

    @Transactional(readOnly = true)
    public FileDistribution getLargeArtworkDistribution(String artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = getArtworkFiles(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getLargeFile();
        return new FileDistribution()
                .setFile(file)
                .setName(file.getName())
                .setMimeType(artwork.getMimeType())
                .setModificationDate(artwork.getDate());
    }

    @Transactional(readOnly = true)
    public FileDistribution getSmallArtworkDistribution(String artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = getArtworkFiles(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getSmallFile();
        return new FileDistribution()
                .setFile(file)
                .setName(file.getName())
                .setMimeType(artwork.getMimeType())
                .setModificationDate(artwork.getDate());
    }

    @Transactional(readOnly = true)
    public ExportBundle exportSong(String songId) throws ObjectNotFoundException {
        ExportBundle exportBundle = exportService.exportSong(songId);
        if (exportBundle == null) {
            throw new ObjectNotFoundException(Song.class, songId);
        }
        return exportBundle;
    }

    @Transactional(readOnly = true)
    public ExportBundle exportAlbum(String albumId) throws ObjectNotFoundException {
        ExportBundle exportBundle = exportService.exportAlbum(albumId);
        if (exportBundle == null) {
            throw new ObjectNotFoundException(Album.class, albumId);
        }
        return exportBundle;
    }

    private ArtworkFiles getArtworkFiles(String artworkId) throws ObjectNotFoundException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId).orElse(null);
        if (artworkFiles == null) {
            throw new ObjectNotFoundException(Artwork.class, artworkId);
        }
        return artworkFiles;
    }
}
