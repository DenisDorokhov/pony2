package net.dorokhov.pony3.web.controller;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony3.api.library.domain.ExportBundle;
import net.dorokhov.pony3.common.RethrowingLambdas;
import net.dorokhov.pony3.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony3.web.service.FileDistributor;
import net.dorokhov.pony3.web.service.FileFacade;
import net.dorokhov.pony3.web.service.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.OutputStream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
@ResponseBody
public class FileController implements ErrorHandlingController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileFacade fileFacade;
    private final FileDistributor fileDistributor;

    public FileController(FileFacade fileFacade, FileDistributor fileDistributor) {
        this.fileFacade = fileFacade;
        this.fileDistributor = fileDistributor;
    }

    @GetMapping(value = "/api/file/audio/{songId}", produces = {"audio/*", APPLICATION_JSON_VALUE})
    public void getAudio(@PathVariable String songId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getSongDistribution(songId), request, response), response);
    }

    private void handleStreamingExceptions(RethrowingLambdas.ThrowingRunnable runnable, HttpServletResponse response) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (e instanceof IOException) {
                if (isBrokenPipe(e)) {
                    // Filter out broken pipe exceptions, as they could easily happen during streaming.
                    logger.trace("Broken pipe error occurred.", e);
                } else {
                    logger.error("Unexpected error occurred.", e);
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ioe) {
                        logger.error("Could not send HTTP status.", ioe);
                    }
                }
            } else if (e instanceof ObjectNotFoundException) {
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } catch (IOException ioe) {
                    logger.error("Could not send HTTP status.", ioe);
                }
            } else {
                logger.error("Unexpected error during file distribution.", e);
            }
        }
    }

    private boolean isBrokenPipe(Exception e) {
        Throwable rootCause = e;
        try {
            rootCause = Throwables.getRootCause(e);
        } catch (IllegalArgumentException iae) {
            logger.error("Could not get root cause of exception.", iae);
        }
        return Strings.nullToEmpty(rootCause.getMessage()).toLowerCase().contains("broken pipe");
    }

    @GetMapping(value = "/api/file/artwork/large/{artworkId}", produces = {"image/*", APPLICATION_JSON_VALUE})
    public void getLargeArtwork(@PathVariable String artworkId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getLargeArtworkDistribution(artworkId), request, response), response);
    }

    @GetMapping(value = "/api/file/artwork/small/{artworkId}", produces = {"image/*", APPLICATION_JSON_VALUE})
    public void getSmallArtwork(@PathVariable String artworkId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getSmallArtworkDistribution(artworkId), request, response), response);
    }

    @GetMapping(value = "/api/file/export/song/{songId}", produces = {"audio/*", APPLICATION_JSON_VALUE})
    public ResponseEntity<?> exportSong(@PathVariable String songId, HttpServletResponse response) {
        try {
            ExportBundle exportBundle = fileFacade.exportSong(songId);
            setExportBundleHeaders(exportBundle, response);
            try (OutputStream outputStream = response.getOutputStream()) {
                exportBundle.getContent().write(outputStream);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            // Filter out broken pipe exceptions, as they could easily happen during streaming.
            if (!isBrokenPipe(e)) {
                logger.error("Unexpected error when exporting song.", e);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/api/file/export/album/{albumId}", produces = {"application/zip", APPLICATION_JSON_VALUE})
    public ResponseEntity<?> exportAlbum(@PathVariable String albumId, HttpServletResponse response) {
        try {
            ExportBundle exportBundle = fileFacade.exportAlbum(albumId);
            setExportBundleHeaders(exportBundle, response);
            try (OutputStream outputStream = response.getOutputStream()) {
                exportBundle.getContent().write(outputStream);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ObjectNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            // Filter out broken pipe exceptions, as they could easily happen during streaming.
            if (!isBrokenPipe(e)) {
                logger.error("Unexpected error when exporting album.", e);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void setExportBundleHeaders(ExportBundle exportBundle, HttpServletResponse response) {
        response.setHeader("Content-Type", exportBundle.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + UriUtils.encodeQuery(exportBundle.getFileName(), "UTF-8") + "\"");
    }
}
