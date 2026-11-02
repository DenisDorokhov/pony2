package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.DiscoveryTaskType;
import net.dorokhov.pony2.api.library.domain.ImageFace;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.library.domain.SpotifyArtistData;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Collections.emptyList;

@Service
@Transactional(readOnly = true)
public class DiscoveryTaskResultReader {

    private final DiscoveryTaskRepository taskRepository;

    public DiscoveryTaskResultReader(DiscoveryTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public @Nullable SpotifyArtistData readSpotifyArtistData(String artistId) {
        DiscoveryTask task = taskRepository.findLatestCompletedByArtistIdAndType(
                        artistId, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1))
                .orElseThrow(() -> new IllegalStateException("No completed SPOTIFY_ARTIST_DATA task for artist '" + artistId + "'."));
        return task.getResult() != null ? JsonConverter.fromJson(task.getResult(), SpotifyArtistData.class) : null;
    }

    public List<String> readSpotifyTopTracks(String artistId) {
        return taskRepository.findLatestCompletedSpotifyTopTracksResultByArtistId(artistId, Limit.of(1))
                .map(json -> JsonConverter.listFromJson(json, String.class)).orElse(emptyList());
    }

    public @Nullable List<ImageFace> readArtistBioImageFaces(String artistId, @Nullable String imageId) {
        if (imageId == null) {
            return null;
        }
        return taskRepository.findLatestCompletedByArtistIdAndType(artistId, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES, Limit.of(1))
                .map(DiscoveryTask::getResult)
                .map(json -> JsonConverter.fromJson(json, ArtistBioImageFacesService.Result.class))
                .filter(result -> imageId.equals(result.externalImageId()))
                .map(ArtistBioImageFacesService.Result::faces)
                .orElse(null);
    }

    public @Nullable MusicBrainzAlbumData readMusicBrainzAlbumData(String albumId) {
        return taskRepository.findLatestCompletedMusicBrainzResultByAlbumId(albumId, Limit.of(1))
                .map(json -> JsonConverter.fromJson(json, MusicBrainzAlbumData.class)).orElse(null);
    }
}
