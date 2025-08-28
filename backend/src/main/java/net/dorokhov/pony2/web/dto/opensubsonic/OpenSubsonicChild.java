package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicChild {

    private String id;
    private String parent;
    private boolean isDir;
    private String title;
    private String album;
    private String artist;
    private Integer track;
    private Integer year;
    private String genre;
    private String coverArt;
    private Long size;
    private String contentType;
    private String suffix;
    private String transcodedContentType;
    private String transcodedSuffix;
    private Integer duration;
    private Integer bitRate;
    private Integer bitDepth;
    private Integer samplingRate;
    private Integer channelCount;
    private String path;
    private Boolean isVideo;
    private Integer userRating;
    private Double averageRating;
    private Long playCount;
    private Integer discNumber;
    private String created;
    private String starred;
    private String albumId;
    private String artistId;
    private String type;
    private String mediaType;
    private Long bookmarkPosition;
    private Integer originalWidth;
    private Integer originalHeight;
    private String played;
    private Integer bpm;
    private String comment;
    private String sortName;
    private String musicBrainzId;
    private List<String> isrc;
    private List<OpenSubsonicItemGenre> genres;
    private List<OpenSubsonicArtistID3> artists;
    private String displayArtist;
    private List<OpenSubsonicArtistID3> albumArtists;
    private String displayAlbumArtist;
    private List<OpenSubsonicContributor> contributors;
    private String displayComposer;
    private List<String> moods;
    private OpenSubsonicReplayGain replayGain;
    private String explicitStatus;

    public String getId() {
        return id;
    }

    public OpenSubsonicChild setId(String id) {
        this.id = id;
        return this;
    }

    public String getParent() {
        return parent;
    }

    public OpenSubsonicChild setParent(String parent) {
        this.parent = parent;
        return this;
    }

    public boolean isDir() {
        return isDir;
    }

    public OpenSubsonicChild setDir(boolean dir) {
        isDir = dir;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public OpenSubsonicChild setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAlbum() {
        return album;
    }

    public OpenSubsonicChild setAlbum(String album) {
        this.album = album;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public OpenSubsonicChild setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public Integer getTrack() {
        return track;
    }

    public OpenSubsonicChild setTrack(Integer track) {
        this.track = track;
        return this;
    }

    public Integer getYear() {
        return year;
    }

    public OpenSubsonicChild setYear(Integer year) {
        this.year = year;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public OpenSubsonicChild setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public OpenSubsonicChild setCoverArt(String coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public OpenSubsonicChild setSize(Long size) {
        this.size = size;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public OpenSubsonicChild setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getSuffix() {
        return suffix;
    }

    public OpenSubsonicChild setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public String getTranscodedContentType() {
        return transcodedContentType;
    }

    public OpenSubsonicChild setTranscodedContentType(String transcodedContentType) {
        this.transcodedContentType = transcodedContentType;
        return this;
    }

    public String getTranscodedSuffix() {
        return transcodedSuffix;
    }

    public OpenSubsonicChild setTranscodedSuffix(String transcodedSuffix) {
        this.transcodedSuffix = transcodedSuffix;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public OpenSubsonicChild setDuration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public OpenSubsonicChild setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
        return this;
    }

    public Integer getBitDepth() {
        return bitDepth;
    }

    public OpenSubsonicChild setBitDepth(Integer bitDepth) {
        this.bitDepth = bitDepth;
        return this;
    }

    public Integer getSamplingRate() {
        return samplingRate;
    }

    public OpenSubsonicChild setSamplingRate(Integer samplingRate) {
        this.samplingRate = samplingRate;
        return this;
    }

    public Integer getChannelCount() {
        return channelCount;
    }

    public OpenSubsonicChild setChannelCount(Integer channelCount) {
        this.channelCount = channelCount;
        return this;
    }

    public String getPath() {
        return path;
    }

    public OpenSubsonicChild setPath(String path) {
        this.path = path;
        return this;
    }

    public Boolean getVideo() {
        return isVideo;
    }

    public OpenSubsonicChild setVideo(Boolean video) {
        isVideo = video;
        return this;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public OpenSubsonicChild setUserRating(Integer userRating) {
        this.userRating = userRating;
        return this;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public OpenSubsonicChild setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
        return this;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public OpenSubsonicChild setPlayCount(Long playCount) {
        this.playCount = playCount;
        return this;
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public OpenSubsonicChild setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public OpenSubsonicChild setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getStarred() {
        return starred;
    }

    public OpenSubsonicChild setStarred(String starred) {
        this.starred = starred;
        return this;
    }

    public String getAlbumId() {
        return albumId;
    }

    public OpenSubsonicChild setAlbumId(String albumId) {
        this.albumId = albumId;
        return this;
    }

    public String getArtistId() {
        return artistId;
    }

    public OpenSubsonicChild setArtistId(String artistId) {
        this.artistId = artistId;
        return this;
    }

    public String getType() {
        return type;
    }

    public OpenSubsonicChild setType(String type) {
        this.type = type;
        return this;
    }

    public String getMediaType() {
        return mediaType;
    }

    public OpenSubsonicChild setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public Long getBookmarkPosition() {
        return bookmarkPosition;
    }

    public OpenSubsonicChild setBookmarkPosition(Long bookmarkPosition) {
        this.bookmarkPosition = bookmarkPosition;
        return this;
    }

    public Integer getOriginalWidth() {
        return originalWidth;
    }

    public OpenSubsonicChild setOriginalWidth(Integer originalWidth) {
        this.originalWidth = originalWidth;
        return this;
    }

    public Integer getOriginalHeight() {
        return originalHeight;
    }

    public OpenSubsonicChild setOriginalHeight(Integer originalHeight) {
        this.originalHeight = originalHeight;
        return this;
    }

    public String getPlayed() {
        return played;
    }

    public OpenSubsonicChild setPlayed(String played) {
        this.played = played;
        return this;
    }

    public Integer getBpm() {
        return bpm;
    }

    public OpenSubsonicChild setBpm(Integer bpm) {
        this.bpm = bpm;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OpenSubsonicChild setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getSortName() {
        return sortName;
    }

    public OpenSubsonicChild setSortName(String sortName) {
        this.sortName = sortName;
        return this;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public OpenSubsonicChild setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
        return this;
    }

    public List<String> getIsrc() {
        return isrc;
    }

    public OpenSubsonicChild setIsrc(List<String> isrc) {
        this.isrc = isrc;
        return this;
    }

    public List<OpenSubsonicItemGenre> getGenres() {
        return genres;
    }

    public OpenSubsonicChild setGenres(List<OpenSubsonicItemGenre> genres) {
        this.genres = genres;
        return this;
    }

    public List<OpenSubsonicArtistID3> getArtists() {
        return artists;
    }

    public OpenSubsonicChild setArtists(List<OpenSubsonicArtistID3> artists) {
        this.artists = artists;
        return this;
    }

    public String getDisplayArtist() {
        return displayArtist;
    }

    public OpenSubsonicChild setDisplayArtist(String displayArtist) {
        this.displayArtist = displayArtist;
        return this;
    }

    public List<OpenSubsonicArtistID3> getAlbumArtists() {
        return albumArtists;
    }

    public OpenSubsonicChild setAlbumArtists(List<OpenSubsonicArtistID3> albumArtists) {
        this.albumArtists = albumArtists;
        return this;
    }

    public String getDisplayAlbumArtist() {
        return displayAlbumArtist;
    }

    public OpenSubsonicChild setDisplayAlbumArtist(String displayAlbumArtist) {
        this.displayAlbumArtist = displayAlbumArtist;
        return this;
    }

    public List<OpenSubsonicContributor> getContributors() {
        return contributors;
    }

    public OpenSubsonicChild setContributors(List<OpenSubsonicContributor> contributors) {
        this.contributors = contributors;
        return this;
    }

    public String getDisplayComposer() {
        return displayComposer;
    }

    public OpenSubsonicChild setDisplayComposer(String displayComposer) {
        this.displayComposer = displayComposer;
        return this;
    }

    public List<String> getMoods() {
        return moods;
    }

    public OpenSubsonicChild setMoods(List<String> moods) {
        this.moods = moods;
        return this;
    }

    public OpenSubsonicReplayGain getReplayGain() {
        return replayGain;
    }

    public OpenSubsonicChild setReplayGain(OpenSubsonicReplayGain replayGain) {
        this.replayGain = replayGain;
        return this;
    }

    public String getExplicitStatus() {
        return explicitStatus;
    }

    public OpenSubsonicChild setExplicitStatus(String explicitStatus) {
        this.explicitStatus = explicitStatus;
        return this;
    }
}
