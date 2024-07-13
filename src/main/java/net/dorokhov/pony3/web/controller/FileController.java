package net.dorokhov.pony3.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony3.api.library.domain.ExportBundle;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.service.FileDistributor;
import net.dorokhov.pony3.web.service.FileFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.OutputStream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@ResponseBody
@RequestMapping("/api/file")
public class FileController implements ErrorHandlingController {

    private final FileFacade fileFacade;
    private final FileDistributor fileDistributor;

    public FileController(FileFacade fileFacade, FileDistributor fileDistributor) {
        this.fileFacade = fileFacade;
        this.fileDistributor = fileDistributor;
    }

    @GetMapping(value = "/audio/{songId}", produces = {"audio/*", APPLICATION_JSON_VALUE})
    public void getAudio(@PathVariable String songId,
                                      HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getSongDistribution(songId), request, response);
    }

    @GetMapping(value = "/artwork/large/{artworkId}", produces = {"image/*", APPLICATION_JSON_VALUE})
    public void getLargeArtwork(@PathVariable String artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getLargeArtworkDistribution(artworkId), request, response);
    }

    @GetMapping(value = "/artwork/small/{artworkId}", produces = {"image/*", APPLICATION_JSON_VALUE})
    public void getSmallArtwork(@PathVariable String artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getSmallArtworkDistribution(artworkId), request, response);
    }

    @GetMapping(value = "/export/song/{songId}", produces = {"audio/*", APPLICATION_JSON_VALUE})
    public ResponseEntity<?> exportSong(@PathVariable String songId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ExportBundle exportBundle = fileFacade.exportSong(songId);
        setExportBundleHeaders(exportBundle, response);
        try (OutputStream outputStream = response.getOutputStream()) {
            exportBundle.getContent().write(outputStream);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/export/album/{albumId}", produces = {"application/zip", APPLICATION_JSON_VALUE})
    public ResponseEntity<?> exportAlbum(@PathVariable String albumId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ExportBundle exportBundle = fileFacade.exportAlbum(albumId);
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
