package net.dorokhov.pony2.web.controller.opensubsonic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.service.FileDistributor;
import net.dorokhov.pony2.web.service.FileFacade;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static net.dorokhov.pony2.web.common.StreamingUtils.handleStreamingExceptions;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@ResponseBody
public class OpenSubsonicFileController implements OpenSubsonicController {

    private final FileFacade fileFacade;
    private final FileDistributor fileDistributor;

    public OpenSubsonicFileController(FileFacade fileFacade, FileDistributor fileDistributor) {
        this.fileFacade = fileFacade;
        this.fileDistributor = fileDistributor;
    }

    @RequestMapping(value = {
            "/opensubsonic/rest/stream.view",
            "/opensubsonic/rest/download.view",
    }, produces = {"audio/*", APPLICATION_JSON_VALUE}, method = {GET, POST})
    public void getAudio(@RequestParam("id") String songId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getSongDistribution(songId), request, response), response);
    }

    @RequestMapping(value = "/opensubsonic/rest/getCoverArt.view", produces = {"image/*", APPLICATION_JSON_VALUE}, method = {GET, POST})
    public void getLargeArtwork(@RequestParam("id") String artworkId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getLargeArtworkDistribution(artworkId), request, response), response);
    }
}
