package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.config.domain.ConfigSet;
import net.dorokhov.pony2.api.config.service.ConfigService;
import net.dorokhov.pony2.api.config.service.command.ConfigSetUpdateCommand;
import net.dorokhov.pony2.api.library.domain.*;
import net.dorokhov.pony2.api.library.domain.AlbumDiscoveryData.ReleaseType;
import net.dorokhov.pony2.api.library.service.LibraryService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryOverridesTest extends InstallingIntegrationTest {

    private static final String ARTIST_UPDATE_PATH = "/api/admin/library/artists/discovery";
    private static final String ALBUM_UPDATE_PATH = "/api/admin/library/albums/discovery";

    @Autowired private ApiTemplate apiTemplate;
    @Autowired private ConfigService configService;
    @Autowired private LibraryService libraryService;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistDiscoveryRepository artistDiscoveryRepository;
    @Autowired private AlbumDiscoveryRepository albumDiscoveryRepository;
    @Autowired private DiscoveryJobRepository jobRepository;
    @Autowired private DiscoveryTaskRepository taskRepository;

    private ConfigSet originalConfig;
    private Artist artist;
    private Album album;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        originalConfig = configService.get();
        configService.update(new ConfigSetUpdateCommand().setLibraryFolders(originalConfig.libraryFolders())
                .setDiscoveryEnabled(true).setLlmUrl("http://localhost:11434/v1").setLlmModel("test"));
        artist = artistRepository.save(new Artist().setName("Artist"));
        album = albumRepository.save(new Album().setArtist(artist).setName("Album"));
        accessToken = apiTemplate.authenticateAdmin().getAccessToken();
    }

    @AfterEach
    void tearDown() throws Exception {
        configService.update(new ConfigSetUpdateCommand().setLibraryFolders(originalConfig.libraryFolders())
                .setDiscoveryEnabled(originalConfig.discoveryEnabled()).setLlmUrl(originalConfig.llmUrl())
                .setLlmModel(originalConfig.llmModel()).setLlmApiKey(originalConfig.llmApiKey()));
    }

    @Test
    void shouldExposeNoOverridesBeforeDiscovery() {
        JsonNode artistData = request(artistPath(), HttpMethod.GET, null).getBody();
        JsonNode albumData = request(albumPath(), HttpMethod.GET, null).getBody();

        assertThat(artistData.hasNonNull("overrideCountryCode")).isFalse();
        assertThat(albumData.hasNonNull("overrideReleaseType")).isFalse();
        assertThat(albumData.get("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(albumDiscoveryRepository.count()).isZero();
    }

    private String artistPath() {
        return "/api/admin/library/artists/" + artist.getId() + "/discovery";
    }

    private String albumPath() {
        return "/api/admin/library/albums/" + album.getId() + "/discovery";
    }

    private ResponseEntity<JsonNode> request(String path, HttpMethod method, Object body) {
        return apiTemplate.getRestTemplate().exchange(path, method, apiTemplate.createHeaderRequest(body, accessToken), JsonNode.class);
    }

    @ParameterizedTest
    @EnumSource(ReleaseType.class)
    void shouldSaveEveryReleaseTypeBeforeDiscovery(ReleaseType releaseType) {
        ResponseEntity<JsonNode> response = request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", album.getId(), "overrideReleaseType", releaseType));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("overrideReleaseType").asString()).isEqualTo(releaseType.name());
        assertThat(response.getBody().get("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(albumDiscoveryRepository.findByAlbumId(album.getId())).get()
                .extracting(AlbumDiscovery::getOverrideReleaseType).isEqualTo(releaseType);
        assertThat(libraryService.getAlbumDiscoveryData(album.getId())).get()
                .extracting(AlbumDiscoveryData::releaseType).isEqualTo(ReleaseType.UNKNOWN);
        JsonNode albumData = artistSongs().get("albumSongs").get(0).get("album").get("discoveryData");
        assertThat(albumData.get("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(albumData.get("overrideReleaseType").asString()).isEqualTo(releaseType.name());
    }

    private JsonNode artistSongs() {
        return request("/api/library/artistSongs/" + artist.getId(), HttpMethod.GET, null).getBody();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DE", "SU"})
    void shouldKeepDiscoveredValuesAndExposeOverridesSeparately(String countryCode) {
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist).setSpotifyArtistId("spotify")
                .setMusicBrainzArtistId("artist").setCountryCode(countryCode));
        albumDiscoveryRepository.save(new AlbumDiscovery().setAlbum(album).setMusicBrainzReleaseGroupId("group"));
        DiscoveryJob job = jobRepository.save(new DiscoveryJob().setType(DiscoveryType.ALBUM).setStatus(DiscoveryJob.Status.COMPLETE));
        MusicBrainzAlbumData data = new MusicBrainzAlbumData("group", "Album", List.of(), "2000-01-01",
                "artist", List.of("DE"), null, null, null, List.of());
        taskRepository.save(new DiscoveryTask().setJob(job).setAlbum(album).setType(DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA)
                .setStatus(DiscoveryTask.Status.COMPLETE).setResult(JsonConverter.toJson(data)));

        JsonNode artistResponse = request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", artist.getId(), "overrideCountryCode", CountryCode.US)).getBody();
        JsonNode albumResponse = request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", album.getId(), "overrideReleaseType", ReleaseType.EP)).getBody();

        assertThat(artistResponse.get("countryCode").asString()).isEqualTo(countryCode);
        assertThat(artistResponse.get("overrideCountryCode").asString()).isEqualTo("US");
        assertThat(artistResponse.get("spotifyArtistId").asString()).isEqualTo("spotify");
        assertThat(artistResponse.get("musicBrainzArtistId").asString()).isEqualTo("artist");
        assertThat(albumResponse.get("releaseType").asString()).isEqualTo("LP");
        assertThat(albumResponse.get("overrideReleaseType").asString()).isEqualTo("EP");
        assertThat(albumResponse.get("musicBrainzReleaseGroupId").asString()).isEqualTo("group");
        JsonNode songs = artistSongs();
        assertThat(songs.get("artist").get("discoveryData").get("countryCode").asString()).isEqualTo(countryCode);
        assertThat(songs.get("artist").get("discoveryData").get("overrideCountryCode").asString()).isEqualTo("US");
        JsonNode albumData = songs.get("albumSongs").get(0).get("album").get("discoveryData");
        assertThat(albumData.get("releaseType").asString()).isEqualTo("LP");
        assertThat(albumData.get("overrideReleaseType").asString()).isEqualTo("EP");
        assertThat(libraryService.getAlbumDiscoveryData(album.getId())).get()
                .extracting(AlbumDiscoveryData::releaseType).isEqualTo(ReleaseType.LP);
        assertThat(libraryService.getAlbumDiscoveryData(album.getId())).get()
                .extracting(AlbumDiscoveryData::musicBrainzReleaseGroupId).isEqualTo("group");

        request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", artist.getId()));
        request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", album.getId()));

        JsonNode restoredSongs = artistSongs();
        assertThat(restoredSongs.get("artist").get("discoveryData").get("countryCode").asString()).isEqualTo(countryCode);
        assertThat(restoredSongs.get("artist").get("discoveryData").hasNonNull("overrideCountryCode")).isFalse();
        assertThat(restoredSongs.get("albumSongs").get(0).get("album").get("discoveryData").get("releaseType").asString()).isEqualTo("LP");
        assertThat(restoredSongs.get("albumSongs").get(0).get("album").get("discoveryData").hasNonNull("overrideReleaseType")).isFalse();
    }

    @Test
    void shouldClearOverridesWhenFieldsAreOmitted() {
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist).setOverrideCountryCode(CountryCode.US));
        albumDiscoveryRepository.save(new AlbumDiscovery().setAlbum(album).setOverrideReleaseType(ReleaseType.EP));
        Map<String, String> artistCommand = Map.of("artistId", artist.getId());
        Map<String, String> albumCommand = Map.of("albumId", album.getId());

        ResponseEntity<JsonNode> artistResponse = request(ARTIST_UPDATE_PATH, HttpMethod.PUT, artistCommand);
        ResponseEntity<JsonNode> albumResponse = request(ALBUM_UPDATE_PATH, HttpMethod.PUT, albumCommand);

        assertThat(artistResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(albumResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(artistResponse.getBody().hasNonNull("overrideCountryCode")).isFalse();
        assertThat(albumResponse.getBody().hasNonNull("overrideReleaseType")).isFalse();
        assertThat(artistDiscoveryRepository.findByArtistId(artist.getId())).get()
                .extracting(ArtistDiscovery::getOverrideCountryCode).isNull();
        assertThat(albumDiscoveryRepository.findByAlbumId(album.getId())).get()
                .extracting(AlbumDiscovery::getOverrideReleaseType).isNull();
    }

    @Test
    void shouldKeepOverridesAfterCleanupAndDeleteThemWhenRequested() {
        artistDiscoveryRepository.save(new ArtistDiscovery().setArtist(artist).setSpotifyArtistId("spotify")
                .setMusicBrainzArtistId("artist").setCountryCode("DE").setOverrideCountryCode(CountryCode.US));
        albumDiscoveryRepository.save(new AlbumDiscovery().setAlbum(album)
                .setMusicBrainzReleaseGroupId("group").setOverrideReleaseType(ReleaseType.EP));

        assertThat(request("/api/admin/discovery/data?deleteOverrides=false", HttpMethod.DELETE, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        JsonNode artistData = request(artistPath(), HttpMethod.GET, null).getBody();
        JsonNode albumData = request(albumPath(), HttpMethod.GET, null).getBody();
        assertThat(artistData.hasNonNull("spotifyArtistId")).isFalse();
        assertThat(artistData.hasNonNull("musicBrainzArtistId")).isFalse();
        assertThat(artistData.hasNonNull("countryCode")).isFalse();
        assertThat(artistData.get("overrideCountryCode").asString()).isEqualTo("US");
        assertThat(albumData.hasNonNull("musicBrainzReleaseGroupId")).isFalse();
        assertThat(albumData.get("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(albumData.get("overrideReleaseType").asString()).isEqualTo("EP");
        JsonNode songs = artistSongs();
        assertThat(songs.get("artist").get("discoveryData").hasNonNull("countryCode")).isFalse();
        assertThat(songs.get("artist").get("discoveryData").get("overrideCountryCode").asString()).isEqualTo("US");
        JsonNode clearedAlbumData = songs.get("albumSongs").get(0).get("album").get("discoveryData");
        assertThat(clearedAlbumData.get("releaseType").asString()).isEqualTo("UNKNOWN");
        assertThat(clearedAlbumData.get("overrideReleaseType").asString()).isEqualTo("EP");

        assertThat(request("/api/admin/discovery/data?deleteOverrides=true", HttpMethod.DELETE, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(albumDiscoveryRepository.count()).isZero();
    }

    @Test
    void shouldRejectInvalidOverrides() {
        assertThat(request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", artist.getId(), "overrideCountryCode", "invalid")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", album.getId(), "overrideReleaseType", "NONE")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(albumDiscoveryRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZZ", "SU", "XK", "EU", "UN", "xx", "NONE"})
    void shouldRejectCountryCodesOutsideIsoList(String countryCode) {
        ResponseEntity<JsonNode> response = request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", artist.getId(), "overrideCountryCode", countryCode));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(artistDiscoveryRepository.count()).isZero();
    }

    @Test
    void shouldRejectUnknownTargetsFromCommands() {
        assertThat(request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", "missing", "overrideCountryCode", "US")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", "missing", "overrideReleaseType", "EP")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(albumDiscoveryRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldRejectCommandsWithoutTargetIdentifiers(String id) {
        Map<String, String> artistCommand = new HashMap<>();
        artistCommand.put("artistId", id);
        artistCommand.put("overrideCountryCode", "US");
        Map<String, String> albumCommand = new HashMap<>();
        albumCommand.put("albumId", id);
        albumCommand.put("overrideReleaseType", "EP");

        assertThat(request(ARTIST_UPDATE_PATH, HttpMethod.PUT, artistCommand).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(ALBUM_UPDATE_PATH, HttpMethod.PUT, albumCommand).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(artistDiscoveryRepository.count()).isZero();
        assertThat(albumDiscoveryRepository.count()).isZero();
    }

    @Test
    void shouldRejectEditingWhenDiscoveryIsDisabled() throws Exception {
        configService.update(new ConfigSetUpdateCommand().setDiscoveryEnabled(false));

        assertThat(request(artistPath(), HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(albumPath(), HttpMethod.GET, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(ARTIST_UPDATE_PATH, HttpMethod.PUT, Map.of("artistId", artist.getId(), "overrideCountryCode", "US")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(ALBUM_UPDATE_PATH, HttpMethod.PUT, Map.of("albumId", album.getId(), "overrideReleaseType", "EP")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
