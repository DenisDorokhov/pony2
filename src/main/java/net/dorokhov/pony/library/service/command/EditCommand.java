package net.dorokhov.pony.library.service.command;

import net.dorokhov.pony.library.service.impl.audio.domain.WritableAudioData;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EditCommand {
    
    private final long songId;
    private final WritableAudioData audioData;

    public EditCommand(long songId, WritableAudioData audioData) {
        this.songId = songId;
        this.audioData = checkNotNull(audioData);
    }

    public long getSongId() {
        return songId;
    }

    public WritableAudioData getAudioData() {
        return audioData;
    }
}
