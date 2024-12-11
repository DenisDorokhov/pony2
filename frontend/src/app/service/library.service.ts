import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {distinctUntilChanged, filter, map, tap} from 'rxjs/operators';
import {Album, Artist, ArtistSongs, SearchResult, Song} from "../domain/library.model";
import {AuthenticationService} from "./authentication.service";
import {ArtistDto, ArtistSongsDto, SearchResultDto, SongDetailsDto} from "../domain/library.dto";

export enum LibraryState {
  UNKNOWN,
  NON_EMPTY,
  EMPTY,
}

@Injectable({
  providedIn: 'root'
})
export class LibraryService {

  private static readonly DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY: string = 'pony2.LibraryService.defaultArtistId';

  private libraryStateSubject = new BehaviorSubject<LibraryState>(LibraryState.UNKNOWN);

  private selectedArtistSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private selectedSongSubject = new BehaviorSubject<Song | undefined>(undefined);

  private scrollToArtistRequestSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private scrollToAlbumRequestSubject = new BehaviorSubject<Album | undefined>(undefined);
  private scrollToSongRequestSubject = new BehaviorSubject<Song | undefined>(undefined);

  private refreshRequestSubject = new Subject<void>();
  private songPlaybackRequestSubject = new Subject<Song | undefined>();

  constructor(
    private authenticationService: AuthenticationService,
    private httpClient: HttpClient
  ) {
    this.authenticationService.observeLogout().subscribe(() => {
      this.libraryStateSubject.next(LibraryState.UNKNOWN);
      this.selectedArtistSubject.next(undefined);
      this.selectedSongSubject.next(undefined);
      this.scrollToArtistRequestSubject.next(undefined);
      this.scrollToSongRequestSubject.next(undefined);
      this.storeDefaultArtistId(undefined);
    });
  }

  observeLibraryState(): Observable<LibraryState> {
    return this.libraryStateSubject.asObservable()
      .pipe(distinctUntilChanged());
  }

  getArtists(): Observable<Artist[]> {
    return this.httpClient.get<ArtistDto[]>('/api/library/artists')
      .pipe(
        map(artistDtos => artistDtos.map(artistDto => new Artist(artistDto))),
        tap(artists =>
          this.libraryStateSubject.next(artists.length > 0 ? LibraryState.NON_EMPTY : LibraryState.EMPTY))
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

  getGenreRandomSongs(genreId: string, count: number): Observable<Song[]> {
    return this.httpClient.get<SongDetailsDto[]>('/api/library/randomGenreSongs/' + genreId + '?count=' + count)
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
      const defaultArtistId = this.loadDefaultArtistId();
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
    return this.selectedSongSubject.value;
  }

  observeSelectedSong(): Observable<Song | undefined> {
    return this.selectedSongSubject.asObservable()
      .pipe(distinctUntilChanged((song1, song2) => {
        if (song1 === song2) {
          return true;
        }
        if (!song1 || !song2) {
          return false;
        }
        return song1.id === song2.id;
      }));
  }

  selectSong(song: Song) {
    this.selectedSongSubject.next(song);
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

  observeScrollToArtistRequest(): Observable<Artist> {
    return this.scrollToArtistRequestSubject.asObservable()
      .pipe(filter(artist => artist !== undefined));
  }

  startScrollToArtist(artist: Artist) {
    this.scrollToArtistRequestSubject.next(artist);
  }

  finishScrollToArtist() {
    this.scrollToArtistRequestSubject.next(undefined);
  }

  observeScrollToAlbumRequest(): Observable<Album> {
    return this.scrollToAlbumRequestSubject.asObservable()
      .pipe(filter(album => album !== undefined));
  }

  startScrollToAlbum(album: Album) {
    this.scrollToAlbumRequestSubject.next(album);
  }

  finishScrollToAlbum() {
    this.scrollToAlbumRequestSubject.next(undefined);
  }

  observeScrollToSongRequest(): Observable<Song> {
    return this.scrollToSongRequestSubject.asObservable()
      .pipe(filter(song => song !== undefined));
  }

  startScrollToSong(song: Song) {
    this.scrollToSongRequestSubject.next(song);
  }

  finishScrollToSong() {
    this.scrollToSongRequestSubject.next(undefined);
  }

  private loadDefaultArtistId(): string | undefined {
    return window.localStorage.getItem(LibraryService.DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY) ?? undefined;
  }

  private storeDefaultArtistId(artistId: string | undefined) {
    if (artistId) {
      window.localStorage.setItem(LibraryService.DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY, artistId);
    } else {
      window.localStorage.removeItem(LibraryService.DEFAULT_ARTIST_ID_LOCAL_STORAGE_KEY);
    }
  }

  search(query: string): Observable<SearchResult> {
    return this.httpClient.get<SearchResultDto>('/api/library/search', { params: {query} })
      .pipe(
        map(searchResultDto => new SearchResult(searchResultDto))
      );
  }
}
