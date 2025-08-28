package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicReplayGain {

    private Double trackGain;
    private Double albumGain;
    private Double trackPeak;
    private Double albumPeak;
    private Double baseGain;
    private Double fallbackGain;

    public Double getTrackGain() {
        return trackGain;
    }

    public OpenSubsonicReplayGain setTrackGain(Double trackGain) {
        this.trackGain = trackGain;
        return this;
    }

    public Double getAlbumGain() {
        return albumGain;
    }

    public OpenSubsonicReplayGain setAlbumGain(Double albumGain) {
        this.albumGain = albumGain;
        return this;
    }

    public Double getTrackPeak() {
        return trackPeak;
    }

    public OpenSubsonicReplayGain setTrackPeak(Double trackPeak) {
        this.trackPeak = trackPeak;
        return this;
    }

    public Double getAlbumPeak() {
        return albumPeak;
    }

    public OpenSubsonicReplayGain setAlbumPeak(Double albumPeak) {
        this.albumPeak = albumPeak;
        return this;
    }

    public Double getBaseGain() {
        return baseGain;
    }

    public OpenSubsonicReplayGain setBaseGain(Double baseGain) {
        this.baseGain = baseGain;
        return this;
    }

    public Double getFallbackGain() {
        return fallbackGain;
    }

    public OpenSubsonicReplayGain setFallbackGain(Double fallbackGain) {
        this.fallbackGain = fallbackGain;
        return this;
    }
}
