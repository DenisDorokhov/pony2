package net.dorokhov.pony.fixture;

import net.dorokhov.pony.library.domain.FileType;
import net.dorokhov.pony.library.domain.ReadableAudioData;

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
