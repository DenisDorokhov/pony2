import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject, forkJoin, Observable, Subject} from 'rxjs';
import {distinctUntilChanged, filter, map, tap} from 'rxjs/operators';
import {Album, Artist, ArtistSongs, Genre, SearchResult, Song} from '../domain/library.model';
import {AuthenticationService} from './authentication.service';
import {
  ArtistDto,
  ArtistSongsDto,
  GenreDto,
  RandomSongsRequestDto,
  SearchResultDto,
  SongDetailsDto
} from '../domain/library.dto';
import {InstallationService} from './installation.service';
import {TranslateService} from '@ngx-translate/core';

const ENGLISH_TO_RUSSIAN_MAPPING: Record<string, string> = {
  'F': 'А',
  '<': 'Б',
  'D': 'В',
  'U': 'Г',
  'L': 'Д',
  'T': 'Е',
  '~': 'Ё',
  ':': 'Ж',
  'P': 'З',
  'B': 'И',
  'Q': 'Й',
  'R': 'К',
  'K': 'Л',
  'V': 'М',
  'Y': 'Н',
  'J': 'О',
  'G': 'П',
  'H': 'Р',
  'C': 'С',
  'N': 'Т',
  'E': 'У',
  'A': 'Ф',
  '{': 'Х',
  'W': 'Ц',
  'X': 'Ч',
  'I': 'Ш',
  'O': 'Щ',
  'S': 'Ы',
  '"': 'Э',
  '>': 'Ю',
  'Z': 'Я',
  'f': 'а',
  ',': 'б',
  'd': 'в',
  'u': 'г',
  'l': 'д',
  't': 'е',
  '`': 'ё',
  ';': 'ж',
  'p': 'з',
  'b': 'и',
  'q': 'й',
  'r': 'к',
  'k': 'л',
  'v': 'м',
  'y': 'н',
  'j': 'о',
  'g': 'п',
  'h': 'р',
  'c': 'с',
  'n': 'т',
  'e': 'у',
  'a': 'ф',
  '[': 'х',
  'w': 'ц',
  'x': 'ч',
  'i': 'ш',
  'o': 'щ',
  's': 'ы',
  '\'': 'э',
  '.': 'ю',
  'z': 'я',
  '}': 'Ъ',
  ']': 'ъ',
  'M': 'Ь',
  'm': 'ь',
};
const RUSSIAN_TO_ENGLISH_MAPPING: Record<string, string> = {
  'Ф': 'A',
  'И': 'B',
  'С': 'C',
  'В': 'D',
  'У': 'E',
  'А': 'F',
  'П': 'G',
  'Р': 'H',
  'Ш': 'I',
  'О': 'J',
  'Л': 'K',
  'Д': 'L',
  'Ь': 'M',
  'Т': 'N',
  'Щ': 'O',
  'З': 'P',
  'Й': 'Q',
  'К': 'R',
  'Ы': 'S',
  'Е': 'T',
  'Г': 'U',
  'М': 'V',
  'Ц': 'W',
  'Ч': 'X',
  'Н': 'Y',
  'Я': 'Z',
  'ф': 'a',
  'и': 'b',
  'с': 'c',
  'в': 'd',
  'у': 'e',
  'а': 'f',
  'п': 'g',
  'р': 'h',
  'ш': 'i',
  'о': 'j',
  'л': 'k',
  'д': 'l',
  'ь': 'm',
  'т': 'n',
  'щ': 'o',
  'з': 'p',
  'й': 'q',
  'к': 'r',
  'ы': 's',
  'е': 't',
  'г': 'u',
  'м': 'v',
  'ц': 'w',
  'ч': 'x',
  'н': 'y',
  'я': 'z',
};

export interface SongSelection {
  song: Song;
  play: boolean;
}

export interface ScrollToSongRequest {
  song: Song;
  scrollToArtist: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class LibraryService {

  private static readonly DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY: string = 'pony2.LibraryService.defaultArtistId';

  private selectedArtistSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private selectedSongSubject = new BehaviorSubject<SongSelection | undefined>(undefined);

  private scrollToArtistRequestSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private scrollToAlbumRequestSubject = new BehaviorSubject<Album | undefined>(undefined);
  private scrollToSongRequestSubject = new BehaviorSubject<ScrollToSongRequest | undefined>(undefined);

  private refreshRequestSubject = new Subject<void>();
  private songPlaybackRequestSubject = new Subject<Song | undefined>();
  private filterByGenreRequestSubject = new Subject<Genre | undefined>();

  private genresSubject = new BehaviorSubject<Genre[]>([]);
  private artistsSubject = new BehaviorSubject<Artist[]>([]);

  private appInForeground = true;

  constructor(
    private authenticationService: AuthenticationService,
    private installationService: InstallationService,
    private translateService: TranslateService,
    private httpClient: HttpClient
  ) {
    this.authenticationService.observeLogout().subscribe(() => {
      this.selectedArtistSubject.next(undefined);
      this.selectedSongSubject.next(undefined);
      this.scrollToArtistRequestSubject.next(undefined);
      this.scrollToSongRequestSubject.next(undefined);
      this.storeDefaultArtistId(undefined);
    });
    this.observeRefreshRequest().subscribe(() => {
      this.requestGenres().subscribe();
      this.requestArtists().subscribe();
    });
    window.addEventListener('blur', () => {
      this.appInForeground = false;
    });
    window.addEventListener('focus', () => {
      if (!this.appInForeground && this.installationService.installationStatus?.installed) {
        this.requestRefresh();
      }
      this.appInForeground = true;
    });
  }

  get defaultArtistId(): string | undefined {
    const localStorageKey = this.resolveDefaultArtistLocalStorageKey();
    if (localStorageKey) {
      return window.localStorage.getItem(localStorageKey) ?? undefined;
    } else {
      return undefined;
    }
  }

  private resolveDefaultArtistLocalStorageKey(): string | undefined {
    if (this.authenticationService.isAuthenticated) {
      return LibraryService.DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY + '.' + this.authenticationService.currentUser!.id;
    }
    return undefined;
  }

  initialize(): Observable<void> {
    return forkJoin({
      likePlaylist: this.requestGenres(),
      playlists: this.requestArtists()
    }).pipe(
      map(() => undefined),
    );
  }

  observeGenres(): Observable<Genre[]> {
    return this.genresSubject.asObservable();
  }

  requestGenres(): Observable<Genre[]> {
    return this.httpClient.get<GenreDto[]>('/api/library/genres')
      .pipe(
        map(genreDtos => genreDtos.map(genreDto => new Genre(genreDto))),
        tap(genres => this.genresSubject.next(genres)),
      );
  }

  observeArtists(): Observable<Artist[]> {
    return this.artistsSubject.asObservable();
  }

  requestArtists(): Observable<Artist[]> {
    return this.httpClient.get<ArtistDto[]>('/api/library/artists')
      .pipe(
        map(artistDtos => artistDtos.map(artistDto => new Artist(artistDto))),
        tap(artists => this.artistsSubject.next(artists)),
      );
  }

  getArtistSongs(artist: string): Observable<ArtistSongs> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .pipe(
        map(artistSongsDto => new ArtistSongs(artistSongsDto))
      );
  }

  getSongs(songIds: string[]): Observable<Song[]> {
    return this.httpClient.post<SongDetailsDto[]>('/api/library/songs/fetchByIds', songIds)
      .pipe(
        map(songDetails =>
          songDetails.map(songDetails =>
            this.songDetailsToSong(songDetails)))
      );
  }

  private songDetailsToSong(songDetails: SongDetailsDto): Song {
    return new Song(
      songDetails.song,
      new Album(
        songDetails.albumDetails.album,
        new Artist(songDetails.albumDetails.artist)
      )
    );
  }

  getRandomSongs(request: RandomSongsRequestDto): Observable<Song[]> {
    return this.httpClient.post<SongDetailsDto[]>('/api/library/fetchRandomSongs', request)
      .pipe(
        map(songDetails =>
          songDetails.map(songDetails =>
            this.songDetailsToSong(songDetails)))
      );
  }

  requestRefresh() {
    this.refreshRequestSubject.next();
  }

  observeRefreshRequest(): Observable<void> {
    return this.refreshRequestSubject.asObservable();
  }

  get selectedArtist(): Artist | undefined {
    return this.selectedArtistSubject.value;
  }

  observeSelectedArtist(): Observable<Artist | undefined> {
    return this.selectedArtistSubject.asObservable()
      .pipe(distinctUntilChanged(Artist.equals));
  }

  selectDefaultArtist(artists: Artist[]): Artist | undefined {
    if (artists.length > 0) {
      const defaultArtistId = this.defaultArtistId;
      const defaultArtist = artists
        .find(artist => artist.id === defaultArtistId);
      if (defaultArtist) {
        this.selectArtistAndMakeDefault(defaultArtist);
        return defaultArtist;
      } else {
        this.selectArtistAndMakeDefault(artists[0]);
        return artists[0];
      }
    }
    return undefined;
  }

  selectArtistAndMakeDefault(artist: Artist) {
    this.selectedArtistSubject.next(artist);
    this.storeDefaultArtistId(artist ? artist.id : undefined);
  }

  deselectArtist() {
    this.selectedArtistSubject.next(undefined);
  }

  get selectedSong(): Song | undefined {
    return this.selectedSongSubject.value?.song;
  }

  observeSelectedSong(): Observable<SongSelection | undefined> {
    return this.selectedSongSubject.asObservable()
      .pipe(distinctUntilChanged((songSelection1, songSelection2) =>
        Song.equals(songSelection1?.song, songSelection2?.song) && songSelection1?.play === songSelection2?.play
      ));
  }

  finishSongSelection() {
    if (this.selectedSongSubject.value) {
      // Avoid repeated playback after behavior subject re-dispatch.
      this.selectedSongSubject.value.play = false;
    }
  }

  selectSong(song: Song, play = false) {
    this.selectedSongSubject.next({ song, play });
  }

  deselectSong() {
    this.selectedSongSubject.next(undefined);
  }

  observeSongPlaybackRequest(): Observable<Song | undefined> {
    return this.songPlaybackRequestSubject.asObservable();
  }

  requestSongPlayback(song?: Song) {
    this.songPlaybackRequestSubject.next(song);
  }

  observeFilterByGenreRequest(): Observable<Genre | undefined> {
    return this.filterByGenreRequestSubject.asObservable();
  }

  requestFilterByGenre(genre: Genre) {
    this.filterByGenreRequestSubject.next(genre);
  }

  observeScrollToArtistRequest(): Observable<Artist> {
    return this.scrollToArtistRequestSubject.asObservable()
      .pipe(filter(artist => artist !== undefined));
  }

  requestScrollToArtist(artist: Artist) {
    this.scrollToArtistRequestSubject.next(artist);
  }

  finishScrollToArtist() {
    this.scrollToArtistRequestSubject.next(undefined);
  }

  observeScrollToAlbumRequest(): Observable<Album> {
    return this.scrollToAlbumRequestSubject.asObservable()
      .pipe(filter(album => album !== undefined));
  }

  requestScrollToAlbum(album: Album) {
    this.scrollToAlbumRequestSubject.next(album);
  }

  finishScrollToAlbum() {
    this.scrollToAlbumRequestSubject.next(undefined);
  }

  observeScrollToSongRequest(): Observable<ScrollToSongRequest> {
    return this.scrollToSongRequestSubject.asObservable()
      .pipe(filter(request => request !== undefined));
  }

  requestScrollToSong(song: Song, scrollToArtist = true) {
    this.scrollToSongRequestSubject.next({song, scrollToArtist});
  }

  finishScrollToSong() {
    this.scrollToSongRequestSubject.next(undefined);
  }

  private storeDefaultArtistId(artistId: string | undefined) {
    const localStorageKey = this.resolveDefaultArtistLocalStorageKey();
    if (localStorageKey) {
      if (artistId) {
        window.localStorage.setItem(localStorageKey, artistId);
      } else {
        window.localStorage.removeItem(localStorageKey);
      }
    }
  }

  search(query: string): Observable<SearchResult> {
    return this.httpClient.get<SearchResultDto>('/api/library/search', { params: {query} })
      .pipe(
        map(searchResultDto => new SearchResult(searchResultDto))
      );
  }

  searchGenres(genres: Genre[], query: string): Genre[] {
    const queryNormalized = query.trim().toLowerCase();
    return genres.filter(genre => {
      const genreNameNormalized = (genre.name ?? this.translateService.instant('library.genre.unknownLabel')).trim().toLowerCase();
      if (queryNormalized.length === 0 || genreNameNormalized.indexOf(queryNormalized) >= 0) {
        return true;
      }
      const transformedRuEnQuery = queryNormalized.split('')
        .map(char => RUSSIAN_TO_ENGLISH_MAPPING[char] ?? char)
        .join('');
      if (genreNameNormalized.indexOf(transformedRuEnQuery) >= 0) {
        return true;
      }
      const transformedEnRuQuery = queryNormalized.split('')
        .map(char => ENGLISH_TO_RUSSIAN_MAPPING[char] ?? char)
        .join('');
      return genreNameNormalized.indexOf(transformedEnRuQuery) >= 0;
    });
  }

  reBuildSearchIndex(): Observable<void> {
    return this.httpClient.post<void>('/api/admin/library/reBuildSearchIndex', null);
  }
}
