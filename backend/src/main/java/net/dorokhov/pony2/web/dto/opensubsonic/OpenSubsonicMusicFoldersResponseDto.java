package net.dorokhov.pony2.web.dto.opensubsonic;

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

        private List<MusicFolder> musicFolder;

        public List<MusicFolder> getMusicFolder() {
            return musicFolder;
        }

        public MusicFolders setMusicFolder(List<MusicFolder> musicFolder) {
            this.musicFolder = musicFolder;
            return this;
        }

        public static class MusicFolder {

            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public MusicFolder setId(int id) {
                this.id = id;
                return this;
            }

            public String getName() {
                return name;
            }

            public MusicFolder setName(String name) {
                this.name = name;
                return this;
            }
        }
    }
}
