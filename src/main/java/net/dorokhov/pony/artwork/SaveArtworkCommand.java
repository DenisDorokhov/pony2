package net.dorokhov.pony.artwork;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.EMPTY_MAP;

public abstract class SaveArtworkCommand {
    
    protected final String tag;
    protected final Map<String, String> metaData;

    public SaveArtworkCommand(String tag, Map<String, String> metaData) {
        this.tag = tag;
        this.metaData = metaData != null ? ImmutableMap.copyOf(metaData) : EMPTY_MAP;
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }
}
