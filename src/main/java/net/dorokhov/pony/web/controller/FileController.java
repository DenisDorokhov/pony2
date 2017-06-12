package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.ExportService;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.library.service.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.util.FileRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@ResponseBody
@RequestMapping("/file")
public class FileController {

    @ControllerAdvice(assignableTypes = FileController.class)
    public static class FileControllerAdvice {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<String> onAccessDenied() {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(ObjectNotFoundException.class)
        public ResponseEntity<String> onObjectNotFound() {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> onUnexpectedError(Exception e) {
            logger.error("Unexpected error occurred.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private final LibraryService libraryService;
    private final ExportService exportService;
    private final FileRequestHandler fileRequestHandler;

    public FileController(LibraryService libraryService, ExportService exportService) {
        
        this.libraryService = libraryService;
        this.exportService = exportService;
        
        fileRequestHandler = new FileRequestHandler();
    }
    
    @GetMapping("/audio/{songId}")
    public void getAudio(@PathVariable("songId") Long songId,
                         HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        Song song = libraryService.getSongById(songId);
        File file = song.getFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), song.getFileType().getMimeType(), 
                song.getUpdateDate() != null ? song.getUpdateDate() : song.getCreationDate()
        ), request, response);
    }

    @GetMapping("/artwork/large/{artworkId}")
    public void getLargeArtwork(@PathVariable("artworkId") Long artworkId, 
                           HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getLargeFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), artwork.getMimeType(), artwork.getDate()
        ), request, response);
    }

    @GetMapping("/artwork/small/{artworkId}")
    public void getSmallArtwork(@PathVariable("artworkId") Long artworkId, 
                           HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId);
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getSmallFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), artwork.getMimeType(), artwork.getDate()
        ), request, response);
    }
    
    @GetMapping("/export/song/{songId}")
    public void exportSong(@PathVariable("songId") Long songId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        try (OutputStream outputStream = response.getOutputStream()) {
            ExportBundle exportBundle = exportService.exportSong(songId);
            setExportBundleHeaders(exportBundle, response);
            exportBundle.getContent().write(outputStream);
        }
    }
    
    @GetMapping("/export/album/{albumId}")
    public void exportAlbum(@PathVariable("albumId") Long albumId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        try (OutputStream outputStream = response.getOutputStream()) {
            ExportBundle exportBundle = exportService.exportAlbum(albumId);
            setExportBundleHeaders(exportBundle, response);
            exportBundle.getContent().write(outputStream);
        }
    }
    
    private void setExportBundleHeaders(ExportBundle exportBundle, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", exportBundle.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + UriUtils.encodeQuery(exportBundle.getFileName(), "UTF-8") + "\"");
    }
}
