package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicMusicFolder;

import java.util.List;

public class OpenSubsonicMusicFoldersResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicMusicFoldersResponseDto> {

    private MusicFolders musicFolders;

    public MusicFolders getMusicFolders() {
        return musicFolders;
    }

    public OpenSubsonicMusicFoldersResponseDto setMusicFolders(MusicFolders musicFolders) {
        this.musicFolders = musicFolders;
        return this;
    }

    public static class MusicFolders {

        private List<OpenSubsonicMusicFolder> musicFolder;

        public List<OpenSubsonicMusicFolder> getMusicFolder() {
            return musicFolder;
        }

        public MusicFolders setMusicFolder(List<OpenSubsonicMusicFolder> musicFolder) {
            this.musicFolder = musicFolder;
            return this;
        }

    }

}
