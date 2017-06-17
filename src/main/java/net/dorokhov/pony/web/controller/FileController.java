package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.library.domain.ExportBundle;
import net.dorokhov.pony.web.controller.exception.ObjectNotFoundException;
import net.dorokhov.pony.web.service.FileDistributor;
import net.dorokhov.pony.web.service.LibraryFacade;
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
import java.io.IOException;
import java.io.OutputStream;

@Controller
@ResponseBody
@RequestMapping("/api/file")
public class FileController implements ErrorHandlingController {

    private final LibraryFacade libraryFacade;
    private final FileDistributor fileDistributor;

    public FileController(LibraryFacade libraryFacade, FileDistributor fileDistributor) {
        this.libraryFacade = libraryFacade;
        this.fileDistributor = fileDistributor;
    }

    @GetMapping("/audio/{songId}")
    public ResponseEntity<?> getAudio(@PathVariable Long songId,
                                      HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(libraryFacade.getSongDistribution(songId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/artwork/large/{artworkId}")
    public ResponseEntity<?> getLargeArtwork(@PathVariable Long artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(libraryFacade.getLargeArtworkDistribution(artworkId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/artwork/small/{artworkId}")
    public ResponseEntity<?> getSmallArtwork(@PathVariable Long artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(libraryFacade.getSmallArtworkDistribution(artworkId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/song/{songId}")
    public ResponseEntity<?> exportSong(@PathVariable Long songId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ExportBundle exportBundle = libraryFacade.exportSong(songId);
        setExportBundleHeaders(exportBundle, response);
        try (OutputStream outputStream = response.getOutputStream()) {
            exportBundle.getContent().write(outputStream);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/album/{albumId}")
    public ResponseEntity<?> exportAlbum(@PathVariable Long albumId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ExportBundle exportBundle = libraryFacade.exportAlbum(albumId);
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
