package net.dorokhov.pony.artwork.draft;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

public abstract class ArtworkDraft {
    
    protected final String tag;
    protected final Map<String, String> metaData;

    public ArtworkDraft(String tag, Map<String, String> metaData) {
        this.tag = tag;
        this.metaData = metaData != null ? ImmutableMap.copyOf(metaData) : ImmutableMap.of();
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }
}
