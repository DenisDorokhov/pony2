package net.dorokhov.pony.library.service.internal.artwork.command;

public abstract class ArtworkDraft {
    
    protected final String sourceUri;

    public ArtworkDraft(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getSourceUri() {
        return sourceUri;
    }
}
