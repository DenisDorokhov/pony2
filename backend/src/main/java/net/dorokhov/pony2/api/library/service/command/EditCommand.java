package net.dorokhov.pony2.api.library.service.command;

import net.dorokhov.pony2.api.library.domain.WritableAudioData;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EditCommand {
    
    private final String songFilePath;
    private final WritableAudioData audioData;

    public EditCommand(String songFilePath, WritableAudioData audioData) {
        this.songFilePath = checkNotNull(songFilePath);
        this.audioData = checkNotNull(audioData);
    }

    public String getSongFilePath() {
        return songFilePath;
    }

    public WritableAudioData getAudioData() {
        return audioData;
    }
}
