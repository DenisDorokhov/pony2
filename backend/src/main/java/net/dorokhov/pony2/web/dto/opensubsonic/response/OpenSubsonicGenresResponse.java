package net.dorokhov.pony2.web.dto.opensubsonic.response;

import net.dorokhov.pony2.web.dto.opensubsonic.OpenSubsonicGenre;

import java.util.List;

public class OpenSubsonicGenresResponse extends OpenSubsonicResponse.AbstractResponse<OpenSubsonicGenresResponse> {

    private Genres genres;

    public Genres getGenres() {
        return genres;
    }

    public OpenSubsonicGenresResponse setGenres(Genres genres) {
        this.genres = genres;
        return this;
    }

    public static class Genres {

        private List<OpenSubsonicGenre> genre;

        public List<OpenSubsonicGenre> getGenre() {
            return genre;
        }

        public Genres setGenre(List<OpenSubsonicGenre> genre) {
            this.genre = genre;
            return this;
        }
    }
}
