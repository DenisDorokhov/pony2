package net.dorokhov.pony.installation.service.command;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.user.service.command.UserCreationDraft;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class InstallationDraft {

    private Integer autoScanInterval;
    private List<File> libraryFolders;
    private UserCreationDraft userCreationDraft;

    public InstallationDraft(Integer autoScanInterval, List<File> libraryFolders, UserCreationDraft userCreationDraft) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = ImmutableList.copyOf(checkNotNull(libraryFolders));
        this.userCreationDraft = checkNotNull(userCreationDraft);
    }

    public Optional<Integer> getAutoScanInterval() {
        return Optional.ofNullable(autoScanInterval);
    }

    public List<File> getLibraryFolders() {
        return libraryFolders;
    }

    public UserCreationDraft getUserCreationDraft() {
        return userCreationDraft;
    }
}
