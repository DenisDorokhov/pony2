package net.dorokhov.pony2.test;

import net.dorokhov.pony2.api.library.domain.FileType;
import net.dorokhov.pony2.api.library.domain.ReadableAudioData;

public class ReadableAudioDataFixtures {

    public static ReadableAudioData readableAudioData() {
        return new ReadableAudioData()
                .setPath("someFile")
                .setFileType(FileType.of("audio/mpeg", "mp3"));
    }
}
