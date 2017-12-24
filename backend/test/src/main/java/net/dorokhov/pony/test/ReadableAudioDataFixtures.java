package net.dorokhov.pony.test;

import net.dorokhov.pony.api.library.domain.FileType;
import net.dorokhov.pony.api.library.domain.ReadableAudioData;

public class ReadableAudioDataFixtures {
    
    public static ReadableAudioData readableAudioData() {
        return readableAudioDataBuilder().build();
    }

    public static ReadableAudioData.Builder readableAudioDataBuilder() {
        return ReadableAudioData.builder()
                .path("someFile")
                .fileType(FileType.of("audio/mpeg", "mp3"));
    }
}
