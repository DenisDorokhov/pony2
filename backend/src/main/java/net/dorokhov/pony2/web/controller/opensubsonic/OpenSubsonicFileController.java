package net.dorokhov.pony2.web.controller.opensubsonic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dorokhov.pony2.web.dto.PlaylistSongsDto;
import net.dorokhov.pony2.web.service.FileDistributor;
import net.dorokhov.pony2.web.service.FileFacade;
import net.dorokhov.pony2.web.service.PlaylistFacade;
import net.dorokhov.pony2.web.service.exception.ObjectNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.dorokhov.pony2.web.common.StreamingUtils.handleStreamingExceptions;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@ResponseBody
public class OpenSubsonicFileController implements OpenSubsonicController {

    private final FileFacade fileFacade;
    private final FileDistributor fileDistributor;
    private final PlaylistFacade playlistFacade;

    public OpenSubsonicFileController(
            FileFacade fileFacade,
            FileDistributor fileDistributor,
            PlaylistFacade playlistFacade
    ) {
        this.fileFacade = fileFacade;
        this.fileDistributor = fileDistributor;
        this.playlistFacade = playlistFacade;
    }

    @RequestMapping(value = {
            "/opensubsonic/rest/stream.view",
            "/opensubsonic/rest/download.view",
    }, produces = {"audio/*", APPLICATION_JSON_VALUE}, method = {GET, POST})
    public void getStream(@RequestParam("id") String songId, HttpServletRequest request, HttpServletResponse response) {
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getSongDistribution(songId), request, response), response);
    }

    @RequestMapping(value = "/opensubsonic/rest/getCoverArt.view", produces = {"image/*", APPLICATION_JSON_VALUE}, method = {GET, POST})
    public void getCoverArt(@RequestParam("id") String artworkOrPlaylistId, HttpServletRequest request, HttpServletResponse response) {
        String resolvedArtworkId;
        if (artworkOrPlaylistId.startsWith("pl-")) {
            resolvedArtworkId = resolvePlaylistArtworkId(artworkOrPlaylistId.replaceAll("^pl-", "")).orElse(artworkOrPlaylistId);
        } else {
            resolvedArtworkId = artworkOrPlaylistId;
        }
        handleStreamingExceptions(() -> fileDistributor.distribute(fileFacade.getLargeArtworkDistribution(resolvedArtworkId), request, response), response);
    }

    private Optional<String> resolvePlaylistArtworkId(String playlistId) {
        try {
            PlaylistSongsDto playlist = playlistFacade.getPlaylistById(playlistId);
            return resolvePlaylistArtworkId(playlist);
        } catch (ObjectNotFoundException e) {
            return Optional.empty();
        }
    }

    private Optional<String> resolvePlaylistArtworkId(PlaylistSongsDto playlist) {
        List<String> artworks = playlist.getSongs().stream()
                .map(playlistSong -> playlistSong.getSong().getAlbumDetails().getAlbum().getArtworkId())
                .filter(Objects::nonNull)
                .toList();
        if (artworks.isEmpty()) {
            return Optional.empty();
        }
        int artworkIndex = (int) Math.floor(artworks.size() / 2.0);
        return Optional.of(artworks.get(artworkIndex));
    }
}
