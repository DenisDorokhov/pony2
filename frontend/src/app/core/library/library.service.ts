import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs-compat/add/operator/distinctUntilChanged';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
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
  private songPlaybackRequestSubject = new Subject<Song | undefined>();
  private scrollToSongRequestSubject = new BehaviorSubject<Song | undefined>(undefined);

  constructor(private httpClient: HttpClient) {
  }

  get libraryState(): LibraryState {
    return this.libraryStateSubject.value;
  }

  observeLibraryState(): Observable<LibraryState> {
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

  get selectedArtist(): Artist | undefined {
    return this.selectedArtistSubject.value;
  }

  observeSelectedArtist(): Observable<Artist | undefined> {
    return this.selectedArtistSubject.asObservable()
      .distinctUntilChanged((artist1, artist2) => {
        if (artist1 === artist2) {
          return true;
        }
        if (!artist1 || !artist2) {
          return false;
        }
        return artist1.id === artist2.id;
      });
  }

  selectArtist(artist: Artist) {
    this.selectedArtistSubject.next(artist);
  }

  deselectArtist() {
    this.selectedArtistSubject.next(undefined);
  }

  get selectedSong(): Song | undefined {
    return this.selectedSongSubject.value;
  }

  observeSelectedSong(): Observable<Song | undefined> {
    return this.selectedSongSubject.asObservable()
      .distinctUntilChanged((song1, song2) => {
        if (song1 === song2) {
          return true;
        }
        if (!song1 || !song2) {
          return false;
        }
        return song1.id === song2.id;
      });
  }

  selectSong(song: Song) {
    this.selectedSongSubject.next(song);
  }

  deselectSong() {
    this.selectedSongSubject.next(undefined);
  }

  observeSongPlaybackRequest(): Observable<Song | undefined> {
    return this.songPlaybackRequestSubject.asObservable()
      .distinctUntilChanged();
  }

  requestSongPlayback(song?: Song) {
    this.songPlaybackRequestSubject.next(song);
  }
  
  observeScrollToSongRequest(): Observable<Song> {
    return this.scrollToSongRequestSubject.asObservable()
      .filter(song => song !== undefined);
  }
  
  startScrollToSong(song: Song) {
    this.scrollToSongRequestSubject.next(song);
  }
  
  finishScrollToSong() {
    this.scrollToSongRequestSubject.next(undefined);
  }
}
