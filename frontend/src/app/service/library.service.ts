import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject, forkJoin, Observable, Subject} from 'rxjs';
import {distinctUntilChanged, filter, map, tap} from 'rxjs/operators';
import {Album, AlbumSongs, Artist, ArtistSongs, Genre, SearchResult, Song} from '../domain/library.model';
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

export interface SongSelection {
  song: Song;
  play: boolean;
}

export interface ScrollToSongRequest {
  song: Song;
  scrollToArtist: boolean;
}

export enum ArtistSortingOrder {
  NAME_ASCENDING = 'NAME_ASCENDING',
  NAME_DESCENDING = 'NAME_DESCENDING',
  MODIFICATION_ASCENDING = 'MODIFICATION_ASCENDING',
  MODIFICATION_DESCENDING = 'MODIFICATION_DESCENDING',
}

export enum AlbumSortingOrder {
  YEAR_ASCENDING = 'YEAR_ASCENDING',
  YEAR_DESCENDING = 'YEAR_DESCENDING',
  NAME_ASCENDING = 'NAME_ASCENDING',
  NAME_DESCENDING = 'NAME_DESCENDING',
  MODIFICATION_ASCENDING = 'MODIFICATION_ASCENDING',
  MODIFICATION_DESCENDING = 'MODIFICATION_DESCENDING',
}

const DEFAULT_ARTIST_SORTING_ORDER = ArtistSortingOrder.NAME_ASCENDING;
const DEFAULT_ALBUM_SORTING_ORDER = AlbumSortingOrder.YEAR_DESCENDING;

const LOCAL_STORAGE_KEY_DEFAULT_ARTIST_ID = 'pony2.LibraryService.defaultArtistId';
const LOCAL_STORAGE_KEY_ARTIST_SORTING_ORDER = 'pony2.LibraryService.artistSortingOrder';
const LOCAL_STORAGE_KEY_ALBUM_SORTING_ORDER = 'pony2.LibraryService.albumSortingOrder';

@Injectable({
  providedIn: 'root'
})
export class LibraryService {

  private genresSubject = new BehaviorSubject<Genre[]>([]);
  private artistsSubject = new BehaviorSubject<Artist[]>([]);

  private selectedArtistSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private selectedSongSubject = new BehaviorSubject<SongSelection | undefined>(undefined);

  private scrollToArtistRequestSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private scrollToAlbumRequestSubject = new BehaviorSubject<Album | undefined>(undefined);
  private scrollToSongRequestSubject = new BehaviorSubject<ScrollToSongRequest | undefined>(undefined);

  private sortingOrderArtistSubject = new BehaviorSubject<ArtistSortingOrder>(DEFAULT_ARTIST_SORTING_ORDER);
  private sortingOrderAlbumSubject = new BehaviorSubject<AlbumSortingOrder>(DEFAULT_ALBUM_SORTING_ORDER);

  private refreshRequestSubject = new Subject<void>();
  private songPlaybackRequestSubject = new Subject<Song | undefined>();
  private filterByGenreRequestSubject = new Subject<Genre | undefined>();

  private appInForeground = true;

  constructor(
    private authenticationService: AuthenticationService,
    private installationService: InstallationService,
    private httpClient: HttpClient
  ) {
    this.authenticationService.observeLogout().subscribe(() => {
      this.artistsSubject.next([]);
      this.genresSubject.next([]);
      this.selectedArtistSubject.next(undefined);
      this.selectedSongSubject.next(undefined);
      this.scrollToArtistRequestSubject.next(undefined);
      this.scrollToAlbumRequestSubject.next(undefined);
      this.scrollToSongRequestSubject.next(undefined);
      this.storeArtistSortingOrder(undefined);
      this.storeAlbumSortingOrder(undefined);
      this.storeDefaultArtistId(undefined);
    });
    this.authenticationService.observeAuthentication().subscribe(() => {
      const artistSortingOrder = this.findLocalStorageItem(this.resolveArtistSortingOrderLocalStorageKey()) as ArtistSortingOrder;
      if (artistSortingOrder) {
        this.sortingOrderArtistSubject.next(artistSortingOrder);
      }
      const albumSortingOrder = this.findLocalStorageItem(this.resolveAlbumSortingOrderLocalStorageKey()) as AlbumSortingOrder;
      if (albumSortingOrder) {
        this.sortingOrderAlbumSubject.next(albumSortingOrder);
      }
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

  private storeDefaultArtistId(artistId: string | undefined) {
    this.storeLocalStorageItem(this.resolveDefaultArtistLocalStorageKey(), artistId);
  }

  private storeLocalStorageItem(localStorageKey: string | undefined, value: string | undefined) {
    if (localStorageKey) {
      if (value) {
        window.localStorage.setItem(localStorageKey, value);
      } else {
        window.localStorage.removeItem(localStorageKey);
      }
    }
  }

  private storeArtistSortingOrder(sortingOrder: ArtistSortingOrder | undefined) {
    this.storeLocalStorageItem(this.resolveArtistSortingOrderLocalStorageKey(), sortingOrder);
    this.sortingOrderArtistSubject.next(sortingOrder ?? DEFAULT_ARTIST_SORTING_ORDER);
  }

  private resolveArtistSortingOrderLocalStorageKey(): string | undefined {
    if (this.authenticationService.isAuthenticated) {
      return LOCAL_STORAGE_KEY_ARTIST_SORTING_ORDER + '.' + this.authenticationService.currentUser!.id;
    }
    return undefined;
  }

  private storeAlbumSortingOrder(sortingOrder: AlbumSortingOrder | undefined) {
    this.storeLocalStorageItem(this.resolveAlbumSortingOrderLocalStorageKey(), sortingOrder);
    this.sortingOrderAlbumSubject.next(sortingOrder ?? DEFAULT_ALBUM_SORTING_ORDER);
  }

  private resolveAlbumSortingOrderLocalStorageKey(): string | undefined {
    if (this.authenticationService.isAuthenticated) {
      return LOCAL_STORAGE_KEY_ALBUM_SORTING_ORDER + '.' + this.authenticationService.currentUser!.id;
    }
    return undefined;
  }

  get defaultArtistId(): string | undefined {
    const localStorageKey = this.resolveDefaultArtistLocalStorageKey();
    return this.findLocalStorageItem(localStorageKey);
  }

  private findLocalStorageItem(localStorageKey: string | undefined): string | undefined {
    if (localStorageKey) {
      return window.localStorage.getItem(localStorageKey) ?? undefined;
    } else {
      return undefined;
    }
  }

  private resolveDefaultArtistLocalStorageKey(): string | undefined {
    if (this.authenticationService.isAuthenticated) {
      return LOCAL_STORAGE_KEY_DEFAULT_ARTIST_ID + '.' + this.authenticationService.currentUser!.id;
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
        tap(artists => {
          this.sortArtists(artists);
          this.artistsSubject.next(artists);
        }),
      );
  }

  sortArtists(artists: Artist[]) {
    const sortingOrder = this.sortingOrderArtistSubject.getValue();
    switch (sortingOrder) {
      case ArtistSortingOrder.NAME_ASCENDING:
        artists.sort(Artist.compareByName);
        break;
      case ArtistSortingOrder.NAME_DESCENDING:
        artists.sort(Artist.compareByName);
        artists.reverse();
        break;
      case ArtistSortingOrder.MODIFICATION_ASCENDING:
        artists.sort(Artist.compareByModificationDate);
        break;
      case ArtistSortingOrder.MODIFICATION_DESCENDING:
        artists.sort(Artist.compareByModificationDate);
        artists.reverse();
        break;
    }
  }

  getArtistSongs(artist: string): Observable<ArtistSongs> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .pipe(
        map(artistSongsDto => {
          const artistSongs = new ArtistSongs(artistSongsDto);
          this.sortArtistSongs(artistSongs);
          return artistSongs;
        }),
      );
  }

  sortArtistSongs(artistSongs: ArtistSongs) {
    const sortingOrder = this.sortingOrderAlbumSubject.getValue();
    switch (sortingOrder) {
      case AlbumSortingOrder.NAME_ASCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByName);
        break;
      case AlbumSortingOrder.NAME_DESCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByName);
        artistSongs.albumSongs.reverse();
        break;
      case AlbumSortingOrder.YEAR_ASCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByYear);
        break;
      case AlbumSortingOrder.YEAR_DESCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByYear);
        artistSongs.albumSongs.reverse();
        break;
      case AlbumSortingOrder.MODIFICATION_ASCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByModificationDate);
        break;
      case AlbumSortingOrder.MODIFICATION_DESCENDING:
        artistSongs.albumSongs.sort(AlbumSongs.compareByModificationDate);
        artistSongs.albumSongs.reverse();
        break;
    }
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

  search(query: string): Observable<SearchResult> {
    return this.httpClient.get<SearchResultDto>('/api/library/search', { params: {query} })
      .pipe(
        map(searchResultDto => new SearchResult(searchResultDto))
      );
  }

  reBuildSearchIndex(): Observable<void> {
    return this.httpClient.post<void>('/api/admin/library/reBuildSearchIndex', null);
  }

  observeArtistSortingOrder(): Observable<ArtistSortingOrder> {
    return this.sortingOrderArtistSubject.asObservable()
      .pipe(distinctUntilChanged());
  }

  updateArtistSortingOrder(sortingOrder: ArtistSortingOrder) {
    const oldSortingOrder = this.sortingOrderArtistSubject.getValue();
    this.storeArtistSortingOrder(sortingOrder);
    if (oldSortingOrder !== sortingOrder) {
      const artists = this.artistsSubject.getValue();
      this.sortArtists(artists);
      this.artistsSubject.next(artists);
    }
  }

  observeAlbumSortingOrder(): Observable<AlbumSortingOrder> {
    return this.sortingOrderAlbumSubject.asObservable()
      .pipe(distinctUntilChanged());
  }

  updateAlbumSortingOrder(sortingOrder: AlbumSortingOrder) {
    this.storeAlbumSortingOrder(sortingOrder);
  }
}
