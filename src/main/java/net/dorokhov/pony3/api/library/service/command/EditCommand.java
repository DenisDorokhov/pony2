package net.dorokhov.pony3.api.library.service.command;

import net.dorokhov.pony3.api.library.domain.WritableAudioData;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EditCommand {
    
    private final String songId;
    private final WritableAudioData audioData;

    public EditCommand(String songId, WritableAudioData audioData) {
        this.songId = checkNotNull(songId);
        this.audioData = checkNotNull(audioData);
    }

    public String getSongId() {
        return songId;
    }

    public WritableAudioData getAudioData() {
        return audioData;
    }
}
