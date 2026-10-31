package net.dorokhov.pony2.core.library.service.discovery.musicbrainz;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.Album;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.api.library.domain.MusicBrainzAlbumData;
import net.dorokhov.pony2.api.log.service.LogService;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.installation.service.BuildVersionProvider;
import net.dorokhov.pony2.core.library.repository.AlbumRepository;
import net.dorokhov.pony2.core.library.service.LibraryJobSynchronizer;
import net.dorokhov.pony2.core.library.service.discovery.DiscoveryTaskExchangeRecorder;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzLlmService.AlbumInput;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzLlmService.NormalizationResponse;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzLlmService.ReleaseGroupSelection;
import net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzLlmService.TrackInput;
import net.dorokhov.pony2.core.library.service.exception.DiscoveryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static net.dorokhov.pony2.core.library.service.discovery.musicbrainz.MusicBrainzLlmService.VARIOUS_ARTISTS;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
public class MusicBrainzAlbumDataService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MusicBrainzClient client;
    private final AlbumRepository albumRepository;
    private final DiscoveryTaskExchangeRecorder exchangeRecorder;
    private final UserService userService;
    private final BuildVersionProvider buildVersionProvider;
    private final MusicBrainzLlmService llmService;
    private final LogService logService;
    private final LibraryJobSynchronizer jobSynchronizer;
    private final TransactionTemplate transactionTemplate;
    private final int maxSearchResponseBytes;

    public MusicBrainzAlbumDataService(
            MusicBrainzClient client,
            AlbumRepository albumRepository,
            DiscoveryTaskExchangeRecorder exchangeRecorder,
            UserService userService,
            BuildVersionProvider buildVersionProvider,
            MusicBrainzLlmService llmService,
            LogService logService,
            LibraryJobSynchronizer jobSynchronizer,
            PlatformTransactionManager transactionManager,
            @Value("${pony.discovery.musicbrainz.maxSearchResponseBytes:409600}") int maxSearchResponseBytes
    ) {
        checkArgument(maxSearchResponseBytes > 0, "Maximum MusicBrainz search response size must be positive.");
        this.client = client;
        this.albumRepository = albumRepository;
        this.exchangeRecorder = exchangeRecorder;
        this.userService = userService;
        this.buildVersionProvider = buildVersionProvider;
        this.llmService = llmService;
        this.logService = logService;
        this.jobSynchronizer = jobSynchronizer;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
        this.maxSearchResponseBytes = maxSearchResponseBytes;
    }

    public @Nullable MusicBrainzAlbumData discover(DiscoveryTask task, boolean cacheEnabled) {
        jobSynchronizer.interruptDiscoveryIfStopped();
        String albumId = requireNonNull(task.getAlbum(),
                () -> "Discovery task '" + task.getId() + "' has no associated album.").getId();
        AlbumInput input = requireNonNull(transactionTemplate.execute(status -> {
            Album album = albumRepository.findById(albumId).orElseThrow();
            return new AlbumInput(album.getName(), album.getYear(), album.getArtist().getName());
        }));
        if (input.title() == null || input.title().isBlank() || input.artistName() == null || input.artistName().isBlank()) {
            logger.debug("Skipping MusicBrainz discovery for album '{}': album title or artist name is unavailable.", albumId);
            return null;
        }
        String userAgent = "pony2/" + buildVersionProvider.getBuildVersion().getVersion() + " (" + userService.getMainAdmin().getEmail() + ")";
        MusicBrainzClient.ExchangeObserver observer = new MusicBrainzClient.ExchangeObserver() {
            @Override
            public void onRequest(String url) {
                jobSynchronizer.interruptDiscoveryIfStopped();
                exchangeRecorder.startExchange(task, url);
            }

            @Override
            public void onResponse(String url, String response) {
                exchangeRecorder.completeExchange(task, url, response);
            }
        };
        try (
                MusicBrainzClient.Session session = client.openSession(userAgent, observer, cacheEnabled);
                LibraryJobSynchronizer.CancellationSubscription ignored = jobSynchronizer.onCancel(session::cancel)
        ) {
            ReleaseGroupSelection selection = findReleaseGroup(task, session, input, cacheEnabled);
            if (selection == null) {
                return null;
            }
            MusicBrainzDto.ReleaseGroup group = selection.group();
            String artistId = selectArtist(task, input, selection, cacheEnabled);
            if (artistId == null) {
                logger.debug("No corresponding MusicBrainz artist was selected for album '{}' in release group '{}'.", albumId, group.id());
                return null;
            }
            MusicBrainzDto.Artist artist = session.getArtist(artistId);
            jobSynchronizer.interruptDiscoveryIfStopped();
            List<String> country = artist.area() != null ? artist.area().country() : List.of();
            if (country.isEmpty() && artist.country() != null && !artist.country().isBlank()) {
                country = List.of(artist.country());
            }
            MusicBrainzAlbumData result = new MusicBrainzAlbumData(group.id(), group.primaryType(), group.secondaryTypes(), group.firstReleaseDate(),
                    artist.id(), country);
            logger.debug("MusicBrainz discovery result for album '{}': {}.", albumId, result);
            return result;
        } catch (MusicBrainzClient.CancelledException e) {
            throw new DiscoveryInterruptedException();
        }
    }

    private @Nullable ReleaseGroupSelection findReleaseGroup(
            DiscoveryTask task, MusicBrainzClient.Session session, AlbumInput input, boolean cacheEnabled
    ) {
        String query = "release:\"" + escapeQuery(input.title()) + "\" AND artistname:\"" + escapeQuery(normalizeArtistName(input.artistName())) + "\"";
        MusicBrainzClient.ReleaseGroupSearchResult searchResult = session.searchReleaseGroups(query);
        jobSynchronizer.interruptDiscoveryIfStopped();
        if (isSearchResponseTooLarge(input, searchResult.rawResponse())) {
            return null;
        }
        NormalizationResponse normalization = null;
        if (searchResult.groups().isEmpty()) {
            normalization = llmService.normalizeTitle(task, input, cacheEnabled);
            String normalizedQuery = normalizationQuery(input, normalization);
            jobSynchronizer.interruptDiscoveryIfStopped();
            if (normalizedQuery != null) {
                searchResult = session.searchReleaseGroups(normalizedQuery);
                jobSynchronizer.interruptDiscoveryIfStopped();
                if (isSearchResponseTooLarge(input, searchResult.rawResponse())) {
                    return null;
                }
            }
        }
        ReleaseGroupSelection selection = selectReleaseGroup(task, input, searchResult, cacheEnabled, List.of());
        if (selection != null) {
            return selection;
        }
        List<String> searchArtistNames = llmService.selectSearchArtists(task, input, loadTracks(task), cacheEnabled);
        if (!searchArtistNames.isEmpty()) {
            searchResult = searchAlternativeArtists(session, input, searchArtistNames, null);
            if (isSearchResponseTooLarge(input, searchResult.rawResponse())) {
                return null;
            }
            selection = selectReleaseGroup(task, input, searchResult, cacheEnabled, searchArtistNames);
            if (selection != null) {
                return selection;
            }
            if (normalization == null) {
                normalization = llmService.normalizeTitle(task, input, cacheEnabled);
            }
            searchResult = searchAlternativeArtists(session, input, searchArtistNames, normalization);
            if (isSearchResponseTooLarge(input, searchResult.rawResponse())) {
                return null;
            }
            selection = selectReleaseGroup(task, input, searchResult, cacheEnabled, searchArtistNames);
            if (selection != null) {
                return selection;
            }
        }
        logger.debug("No unambiguous MusicBrainz release group was selected for album '{}'.", requireNonNull(task.getAlbum()).getId());
        return null;
    }

    private String normalizeArtistName(String artistName) {
        String normalized = artistName.replaceFirst("(?i)^the ", "");
        return normalized.isBlank() ? artistName : normalized;
    }

    private boolean isSearchResponseTooLarge(AlbumInput input, String response) {
        int responseBytes = response.getBytes(UTF_8).length;
        if (responseBytes <= maxSearchResponseBytes) {
            return false;
        }
        logService.warn(logger, "Skipping MusicBrainz discovery for album '{}' -> '{}': search response size {} bytes exceeds the limit of {} bytes.",
                input.artistName(), input.title(), responseBytes, maxSearchResponseBytes);
        return true;
    }

    private List<TrackInput> loadTracks(DiscoveryTask task) {
        String albumId = requireNonNull(task.getAlbum(),
                () -> "Discovery task '" + task.getId() + "' has no associated album.").getId();
        return requireNonNull(transactionTemplate.execute(status ->
                albumRepository.findById(albumId).orElseThrow().getSongs().stream()
                        .map(song -> new TrackInput(song.getName(), song.getDiscNumber(), song.getTrackNumber(), song.getDuration(),
                                song.getArtistName(), song.getAlbumArtistName()))
                        .sorted(Comparator.comparingInt((TrackInput track) ->
                                        track.discNumber() != null && track.discNumber() > 0 ? track.discNumber() : 1)
                                .thenComparing(TrackInput::trackNumber, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(TrackInput::title, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList()));
    }

    private MusicBrainzClient.ReleaseGroupSearchResult searchAlternativeArtists(
            MusicBrainzClient.Session session, AlbumInput input, List<String> artistNames, @Nullable NormalizationResponse normalization
    ) {
        Map<String, MusicBrainzDto.ReleaseGroup> groups = new LinkedHashMap<>();
        Map<String, Object> rawGroups = new LinkedHashMap<>();
        for (String name : artistNames) {
            jobSynchronizer.interruptDiscoveryIfStopped();
            String condition = "artistname:\"" + escapeQuery(normalizeArtistName(name)) + "\"";
            if (!name.equals(VARIOUS_ARTISTS)) {
                condition = "(" + condition + " OR artist:\"" + escapeQuery(normalizeArtistName(name)) + "\")";
            }
            String query = normalization == null ? "release:\"" + escapeQuery(input.title()) + "\" AND " + condition
                    : normalizationQuery(input, normalization, condition);
            if (query == null) {
                continue;
            }
            MusicBrainzClient.ReleaseGroupSearchResult result = session.searchReleaseGroups(query);
            jobSynchronizer.interruptDiscoveryIfStopped();
            result.groups().forEach(group -> groups.putIfAbsent(group.id(), group));
            Map<String, Object> data = JsonConverter.mapFromJson(result.rawResponse(), String.class, Object.class);
            for (Object value : (List<?>) data.get("release-groups")) {
                Map<?, ?> group = (Map<?, ?>) value;
                rawGroups.putIfAbsent((String) group.get("id"), value);
            }
        }
        return new MusicBrainzClient.ReleaseGroupSearchResult(List.copyOf(groups.values()),
                JsonConverter.toJson(Map.of("release-groups", List.copyOf(rawGroups.values()))));
    }

    private @Nullable String normalizationQuery(AlbumInput input, NormalizationResponse normalization) {
        return normalizationQuery(input, normalization, "artistname:\"" + escapeQuery(normalizeArtistName(input.artistName())) + "\"");
    }

    private @Nullable String normalizationQuery(AlbumInput input, NormalizationResponse normalization, String artistCondition) {
        String original = "release:\"" + escapeQuery(input.title()) + "\"";
        List<UnaryOperator<String>> normalizers = List.of(
                UnaryOperator.identity(),
                term -> term.replaceAll("[^\\p{L}\\p{N} ]", " ").replaceAll(" +", " ").strip(),
                term -> term.replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll(" +", " ").strip());
        List<String> branches = new ArrayList<>();
        for (UnaryOperator<String> normalizer : normalizers) {
            String title = normalizer.apply(normalization.searchTitle());
            String full = "release:\"" + escapeQuery(title) + "\"";
            if (!title.isBlank() && !full.equals(original) && !branches.contains(full)) {
                branches.add(full);
            }
            for (String alternativeTitle : normalization.alternativeTitles()) {
                String normalizedTitle = normalizer.apply(alternativeTitle);
                String alternative = "release:\"" + escapeQuery(normalizedTitle) + "\"";
                if (!normalizedTitle.isBlank() && !alternative.equals(original) && !branches.contains(alternative)) {
                    branches.add(alternative);
                }
            }
            List<String> normalizedFragments = normalization.searchFragments().stream().map(normalizer).distinct().toList();
            if (!normalizedFragments.isEmpty() && normalizedFragments.stream().noneMatch(String::isBlank)) {
                List<String> fragmentClauses = normalizedFragments.stream()
                        .map(fragment -> "release:\"" + escapeQuery(fragment) + "\"").toList();
                String fragments = String.join(" AND ", fragmentClauses);
                String branch = fragmentClauses.size() > 1 ? "(" + fragments + ")" : fragments;
                if (!fragments.equals(original) && !branches.contains(branch)) {
                    branches.add(branch);
                }
            }
        }
        if (branches.isEmpty()) {
            return null;
        }
        String conditions = branches.size() > 1 ? "(" + String.join(" OR ", branches) + ")" : branches.getFirst();
        return artistCondition + " AND " + conditions;
    }

    private @Nullable ReleaseGroupSelection selectReleaseGroup(
            DiscoveryTask task, AlbumInput input,
            MusicBrainzClient.ReleaseGroupSearchResult searchResult,
            boolean cacheEnabled, List<String> searchArtistNames
    ) {
        Map<String, MusicBrainzDto.ReleaseGroup> candidates = new LinkedHashMap<>();
        searchResult.groups().forEach(group -> candidates.putIfAbsent(group.id(), group));
        List<MusicBrainzDto.ReleaseGroup> exact = candidates.values().stream()
                .filter(group -> group.releases().stream().anyMatch(release -> input.title().equalsIgnoreCase(release.title())))
                .toList();
        if (exact.size() == 1 && searchArtistNames.isEmpty()) {
            return new ReleaseGroupSelection(exact.getFirst(), searchResult.rawResponse(), searchArtistNames);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        String id = llmService.selectReleaseGroup(task, input, searchResult, loadTracks(task), searchArtistNames, cacheEnabled);
        return id != null ? new ReleaseGroupSelection(candidates.get(id), searchResult.rawResponse(), searchArtistNames) : null;
    }

    private @Nullable String selectArtist(
            DiscoveryTask task, AlbumInput input, ReleaseGroupSelection selection, boolean cacheEnabled
    ) {
        MusicBrainzDto.ReleaseGroup group = selection.group();
        List<String> searchArtistNames = selection.searchArtistNames();
        List<String> ids = group.artistCredit().stream().map(credit -> credit.artist().id()).distinct().toList();
        if (ids.size() == 1) {
            return ids.getFirst();
        }
        List<String> exact = group.artistCredit().stream()
                .filter(credit -> input.artistName().equalsIgnoreCase(credit.artist().name()) ||
                        input.artistName().equalsIgnoreCase(credit.name()) ||
                        searchArtistNames.stream().anyMatch(name -> name.equalsIgnoreCase(credit.artist().name()) ||
                                name.equalsIgnoreCase(credit.name())))
                .map(credit -> credit.artist().id()).distinct().toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (ids.isEmpty()) {
            return null;
        }
        List<TrackInput> tracks = searchArtistNames.isEmpty() ? List.of() : loadTracks(task);
        return llmService.selectArtist(task, input, selection, tracks, cacheEnabled);
    }

    private String escapeQuery(String value) {
        return value.replaceAll("([+\\-!(){}\\[\\]^\"~*?:\\\\/|&])", "\\\\$1");
    }
}
