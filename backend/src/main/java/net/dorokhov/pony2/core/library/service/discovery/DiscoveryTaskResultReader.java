package net.dorokhov.pony2.core.library.service.discovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.ArtistBiographyData;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return taskRepository.findLatestCompletedResultByArtistIdAndType(artistId, DiscoveryTaskType.SPOTIFY_TOP_TRACKS, Limit.of(1))
                .map(json -> JsonConverter.listFromJson(json, String.class)).orElse(emptyList());
    }

    public ArtistBiographyData readArtistBiography(String artistId, @Nullable String imageId) {
        String result = taskRepository.findLatestCompletedResultByArtistIdAndType(artistId, DiscoveryTaskType.SPOTIFY_ARTIST_DATA, Limit.of(1))
                .orElse(null);
        return toArtistBiography(artistId, imageId, result);
    }

    private ArtistBiographyData toArtistBiography(String artistId, @Nullable String imageId, @Nullable String result) {
        Biography biography = null;
        if (result != null) {
            biography = JsonConverter.fromJson(result, Biography.class);
        }
        String text = null;
        List<ArtistBiographyData.Link> links = emptyList();
        List<String> similarArtistSpotifyIds = emptyList();
        if (biography != null) {
            text = biography.biography();
            if (biography.links() != null) {
                links = biography.links().stream().map(ArtistBiographyData.Link::of).toList();
            }
            if (biography.similarArtists() != null) {
                similarArtistSpotifyIds = biography.similarArtists().stream()
                        .filter(Objects::nonNull)
                        .map(SpotifyArtistData.SpotifyArtist::id)
                        .filter(id -> id != null && !id.isBlank())
                        .distinct()
                        .toList();
            }
        }
        if (text != null && text.isBlank()) {
            text = null;
        }
        return new ArtistBiographyData(
                imageId, readArtistBioImageFaces(artistId, imageId), text, links, similarArtistSpotifyIds
        );
    }

    public Map<String, Boolean> readArtistBiographyAvailability(List<String> artistIds) {
        if (artistIds.isEmpty()) {
            return Map.of();
        }
        return taskRepository.findLatestCompletedResultsByArtistIdsAndType(artistIds, DiscoveryTaskType.SPOTIFY_ARTIST_DATA)
                .stream().collect(Collectors.toMap(DiscoveryTaskRepository.ArtistResult::getArtistId,
                        result -> toArtistBiography(result.getArtistId(), null, result.getResult()).isAvailable()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Biography(
            @Nullable String biography, @Nullable List<SpotifyArtistData.ExternalLink> links,
            @Nullable List<SpotifyArtistData.SpotifyArtist> similarArtists
    ) {}

    public @Nullable List<ImageFace> readArtistBioImageFaces(String artistId, @Nullable String imageId) {
        if (imageId == null) {
            return null;
        }
        return taskRepository.findLatestCompletedResultByArtistIdAndType(artistId, DiscoveryTaskType.ARTIST_BIO_IMAGE_FACES, Limit.of(1))
                .map(json -> JsonConverter.fromJson(json, ArtistBioImageFacesService.Result.class))
                .filter(result -> imageId.equals(result.externalImageId()))
                .map(ArtistBioImageFacesService.Result::faces)
                .orElse(null);
    }

    public @Nullable MusicBrainzAlbumData readMusicBrainzAlbumData(String albumId) {
        return taskRepository.findLatestCompletedResultByAlbumIdAndType(albumId, DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA, Limit.of(1))
                .map(json -> JsonConverter.fromJson(json, MusicBrainzAlbumData.class)).orElse(null);
    }
}
