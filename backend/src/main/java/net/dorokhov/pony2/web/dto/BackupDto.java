package net.dorokhov.pony2.web.dto;

public class BackupDto {

    private String fileContent;

    public String getFileContent() {
        return fileContent;
    }

    public BackupDto setFileContent(String fileContent) {
        this.fileContent = fileContent;
        return this;
    }

    public static BackupDto of(String backup) {
        return new BackupDto()
                .setFileContent(backup);
    }
}
