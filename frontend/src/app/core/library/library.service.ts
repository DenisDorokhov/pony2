import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs-compat/add/operator/distinctUntilChanged';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/common.dto';
import {ArtistSongsDto} from './library.dto';
import {Artist, ArtistSongs, Song} from './library.model';

export enum LibraryState {
  UNKNOWN,
  NON_EMPTY,
  EMPTY,
}

@Injectable()
export class LibraryService {

  private libraryStateSubject = new BehaviorSubject<LibraryState>(LibraryState.UNKNOWN);
  private selectedArtistSubject = new BehaviorSubject<Artist | undefined>(undefined);
  private selectedSongSubject = new BehaviorSubject<Song | undefined>(undefined);
  private songPlaybackRequestSubject = new BehaviorSubject<Song | undefined>(undefined);

  constructor(private httpClient: HttpClient) {
  }

  get libraryState(): Observable<LibraryState> {
    return this.libraryStateSubject.asObservable()
      .distinctUntilChanged();
  }

  getArtists(): Observable<Artist[]> {
    return this.httpClient.get<Artist[]>('/api/library/artists')
      .catch(ErrorDto.observableFromHttpErrorResponse)
      .map(artistDtos => artistDtos.map(artistDto => new Artist(artistDto)))
      .do(artists => this.libraryStateSubject.next(artists.length > 0 ? LibraryState.NON_EMPTY : LibraryState.EMPTY));
  }

  getArtistSongs(artist: number): Observable<ArtistSongs> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .catch(ErrorDto.observableFromHttpErrorResponse)
      .map(artistSongsDto => new ArtistSongs(artistSongsDto));
  }

  get selectedArtist(): Observable<Artist | undefined> {
    return this.selectedArtistSubject.asObservable()
      .distinctUntilChanged();
  }

  selectArtist(artist: Artist) {
    this.selectedArtistSubject.next(artist);
  }

  deselectArtist() {
    this.selectedArtistSubject.next(undefined);
  }

  get selectedSong(): Observable<Song | undefined> {
    return this.selectedSongSubject.asObservable()
      .distinctUntilChanged();
  }

  selectSong(song: Song) {
    this.selectedSongSubject.next(song);
  }

  deselectSong() {
    this.selectedSongSubject.next(undefined);
  }

  get songPlaybackRequest(): Observable<Song | undefined> {
    return this.songPlaybackRequestSubject.asObservable()
      .distinctUntilChanged();
  }

  requestSongPlayback(song: Song) {
    this.songPlaybackRequestSubject.next(song);
  }
}
