package net.dorokhov.pony2.web.dto;

public class PlaylistBackupDto {

    private String fileContent;

    public String getFileContent() {
        return fileContent;
    }

    public PlaylistBackupDto setFileContent(String fileContent) {
        this.fileContent = fileContent;
        return this;
    }

    public static PlaylistBackupDto of(String backup) {
        return new PlaylistBackupDto()
                .setFileContent(backup);
    }
}
