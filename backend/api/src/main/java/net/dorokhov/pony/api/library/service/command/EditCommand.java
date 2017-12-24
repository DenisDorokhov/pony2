package net.dorokhov.pony.api.library.service.command;

import net.dorokhov.pony.api.library.domain.WritableAudioData;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EditCommand {
    
    private final Long songId;
    private final WritableAudioData audioData;

    public EditCommand(Long songId, WritableAudioData audioData) {
        this.songId = checkNotNull(songId);
        this.audioData = checkNotNull(audioData);
    }

    public Long getSongId() {
        return songId;
    }

    public WritableAudioData getAudioData() {
        return audioData;
    }
}
