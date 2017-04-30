package net.dorokhov.pony.library.service.internal.artwork.command;

public abstract class ArtworkCommand {
    
    protected final String sourceUri;

    public ArtworkCommand(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getSourceUri() {
        return sourceUri;
    }
}
