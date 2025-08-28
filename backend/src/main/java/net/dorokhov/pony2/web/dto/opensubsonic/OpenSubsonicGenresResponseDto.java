package net.dorokhov.pony2.web.dto.opensubsonic;

import java.util.List;

public class OpenSubsonicGenresResponseDto extends OpenSubsonicResponseDto.AbstractResponse<OpenSubsonicGenresResponseDto> {

    private Genres genres;

    public OpenSubsonicGenresResponseDto(List<OpenSubsonicGenresResponseDto.Genres.Genre> genres) {
        this.genres = new Genres().setGenre(genres);
    }

    public Genres getGenres() {
        return genres;
    }

    public OpenSubsonicGenresResponseDto setGenres(Genres genres) {
        this.genres = genres;
        return this;
    }

    public static class Genres {

        private List<Genre> genre;

        public List<Genre> getGenre() {
            return genre;
        }

        public Genres setGenre(List<Genre> genre) {
            this.genre = genre;
            return this;
        }

        public static class Genre {

            private int songCount;
            private int albumCount;
            private String value;

            public int getSongCount() {
                return songCount;
            }

            public Genre setSongCount(int songCount) {
                this.songCount = songCount;
                return this;
            }

            public int getAlbumCount() {
                return albumCount;
            }

            public Genre setAlbumCount(int albumCount) {
                this.albumCount = albumCount;
                return this;
            }

            public String getValue() {
                return value;
            }

            public Genre setValue(String value) {
                this.value = value;
                return this;
            }
        }
    }
}
