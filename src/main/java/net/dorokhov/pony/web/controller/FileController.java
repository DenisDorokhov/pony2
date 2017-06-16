package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.library.domain.Artwork;
import net.dorokhov.pony.library.domain.ArtworkFiles;
import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.library.domain.Song;
import net.dorokhov.pony.library.service.ExportService;
import net.dorokhov.pony.library.service.LibraryService;
import net.dorokhov.pony.web.util.FileRequestHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@ResponseBody
@RequestMapping("/api/file")
public class FileController implements ErrorHandlingController {

    private final LibraryService libraryService;
    private final ExportService exportService;
    private final FileRequestHandler fileRequestHandler;

    public FileController(LibraryService libraryService, ExportService exportService) {

        this.libraryService = libraryService;
        this.exportService = exportService;

        fileRequestHandler = new FileRequestHandler();
    }

    @GetMapping("/audio/{songId}")
    public ResponseEntity<?> getAudio(@PathVariable("songId") Long songId,
                                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        Song song = libraryService.getSongById(songId);
        if (song == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        File file = song.getFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), song.getFileType().getMimeType(),
                song.getUpdateDate() != null ? song.getUpdateDate() : song.getCreationDate()
        ), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/artwork/large/{artworkId}")
    public ResponseEntity<?> getLargeArtwork(@PathVariable("artworkId") Long artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws IOException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId);
        if (artworkFiles == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getLargeFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), artwork.getMimeType(), artwork.getDate()
        ), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/artwork/small/{artworkId}")
    public ResponseEntity<?> getSmallArtwork(@PathVariable("artworkId") Long artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws IOException {
        ArtworkFiles artworkFiles = libraryService.getArtworkFilesById(artworkId);
        if (artworkFiles == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Artwork artwork = artworkFiles.getArtwork();
        File file = artworkFiles.getSmallFile();
        fileRequestHandler.handleRequest(new FileRequestHandler.Command(
                file, file.getName(), artwork.getMimeType(), artwork.getDate()
        ), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/song/{songId}")
    public ResponseEntity<?> exportSong(@PathVariable("songId") Long songId, HttpServletResponse response) throws IOException {
        ExportBundle exportBundle = exportService.exportSong(songId);
        if (exportBundle == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        setExportBundleHeaders(exportBundle, response);
        try (OutputStream outputStream = response.getOutputStream()) {
            exportBundle.getContent().write(outputStream);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/album/{albumId}")
    public ResponseEntity<?> exportAlbum(@PathVariable("albumId") Long albumId, HttpServletResponse response) throws IOException {
        ExportBundle exportBundle = exportService.exportAlbum(albumId);
        if (exportBundle == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        setExportBundleHeaders(exportBundle, response);
        try (OutputStream outputStream = response.getOutputStream()) {
            exportBundle.getContent().write(outputStream);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void setExportBundleHeaders(ExportBundle exportBundle, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", exportBundle.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + UriUtils.encodeQuery(exportBundle.getFileName(), "UTF-8") + "\"");
    }
}
