package net.dorokhov.pony.installation.service.command;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.user.service.command.UserCreationCommand;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InstallationCommand {

    private Integer autoScanInterval;
    private List<File> libraryFolders;
    private UserCreationCommand userCreationCommand;

    public InstallationCommand(@Nullable Integer autoScanInterval, List<File> libraryFolders, UserCreationCommand userCreationCommand) {
        this.autoScanInterval = autoScanInterval;
        this.libraryFolders = ImmutableList.copyOf(checkNotNull(libraryFolders));
        this.userCreationCommand = checkNotNull(userCreationCommand);
    }

    @Nullable
    public Integer getAutoScanInterval() {
        return autoScanInterval;
    }

    public List<File> getLibraryFolders() {
        return libraryFolders;
    }

    public UserCreationCommand getUserCreationCommand() {
        return userCreationCommand;
    }
}
