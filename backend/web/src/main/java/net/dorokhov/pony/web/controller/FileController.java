package net.dorokhov.pony.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.dorokhov.pony.api.library.domain.ExportBundle;
import net.dorokhov.pony.web.controller.common.ErrorHandlingController;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.service.FileDistributor;
import net.dorokhov.pony.web.service.FileFacade;
import net.dorokhov.pony.web.service.exception.ObjectNotFoundException;
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

import static net.dorokhov.pony.web.controller.common.ApiResponseValues.*;

@Controller
@ResponseBody
@RequestMapping("/api/file")
@Api(tags = "Files")
@ApiResponses({
        @ApiResponse(code = UNAUTHORIZED_CODE, message = UNAUTHORIZED_MESSAGE, response = ErrorDto.class),
})
public class FileController implements ErrorHandlingController {

    private final FileFacade fileFacade;
    private final FileDistributor fileDistributor;

    public FileController(FileFacade fileFacade, FileDistributor fileDistributor) {
        this.fileFacade = fileFacade;
        this.fileDistributor = fileDistributor;
    }

    @GetMapping(value = "/audio/{songId}", produces = {"audio/*", "application/json"})
    @ApiOperation("Get audio stream by song ID.")
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested song not found.", response = ErrorDto.class),
    })
    public ResponseEntity<?> getAudio(@PathVariable String songId,
                                      HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getSongDistribution(songId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/artwork/large/{artworkId}", produces = {"image/*", "application/json"})
    @ApiOperation("Get large artwork file by artwork ID.")
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested artwork not found.", response = ErrorDto.class),
    })
    public ResponseEntity<?> getLargeArtwork(@PathVariable String artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getLargeArtworkDistribution(artworkId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/artwork/small/{artworkId}", produces = {"image/*", "application/json"})
    @ApiOperation("Get small artwork file by artwork ID.")
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested artwork not found.", response = ErrorDto.class),
    })
    public ResponseEntity<?> getSmallArtwork(@PathVariable String artworkId,
                                             HttpServletRequest request, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        fileDistributor.distribute(fileFacade.getSmallArtworkDistribution(artworkId), request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/export/song/{songId}", produces = {"audio/*", "application/json"})
    @ApiOperation("Export song file by song ID. File will be returned with corresponding name within attachment content disposition.")
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested song not found.", response = ErrorDto.class),
    })
    public ResponseEntity<?> exportSong(@PathVariable String songId, HttpServletResponse response) throws ObjectNotFoundException, IOException {
        ExportBundle exportBundle = fileFacade.exportSong(songId);
        setExportBundleHeaders(exportBundle, response);
        try (OutputStream outputStream = response.getOutputStream()) {
            exportBundle.getContent().write(outputStream);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/export/album/{albumId}", produces = {"application/zip", "application/json"})
    @ApiOperation("Export entire album by album ID. File will be returned with corresponding name within attachment content disposition.")
    @ApiResponses({
            @ApiResponse(code = NOT_FOUND_CODE, message = "Requested album not found.", response = ErrorDto.class),
    })
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
