package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicAlbumID3;

import java.util.List;

public class OpenSubsonicAlbumList2Response extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicAlbumList2Response> {

    private AlbumList2 albumList2;

    public AlbumList2 getAlbumList2() {
        return albumList2;
    }

    public OpenSubsonicAlbumList2Response setAlbumList2(AlbumList2 albumList2) {
        this.albumList2 = albumList2;
        return this;
    }

    public static class AlbumList2 {

        private List<OpenSubsonicAlbumID3> album;

        public List<OpenSubsonicAlbumID3> getAlbum() {
            return album;
        }

        public AlbumList2 setAlbum(List<OpenSubsonicAlbumID3> album) {
            this.album = album;
            return this;
        }
    }
}
