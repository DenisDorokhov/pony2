import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs-compat/add/operator/distinctUntilChanged';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/error.dto';
import {ArtistSongsDto} from './artist-songs.dto';
import {ArtistDto} from './artist.dto';
import {SongDto} from './song.dto';

export enum LibraryState {
  UNKNOWN,
  NON_EMPTY,
  EMPTY,
}

@Injectable()
export class LibraryService {

  private libraryStateSubject = new BehaviorSubject<LibraryState>(LibraryState.UNKNOWN);
  private selectedArtistSubject = new BehaviorSubject<ArtistDto>(undefined);
  private selectedSongSubject = new BehaviorSubject<SongDto>(undefined);

  constructor(private httpClient: HttpClient) {
  }

  get libraryState(): Observable<LibraryState> {
    return this.libraryStateSubject.asObservable()
      .distinctUntilChanged();
  }

  getArtists(): Observable<ArtistDto[]> {
    return this.httpClient.get<ArtistDto[]>('/api/library/artists')
      .catch(ErrorDto.observableFromHttpErrorResponse)
      .do(artists => this.libraryStateSubject.next(artists.length > 0 ? LibraryState.NON_EMPTY : LibraryState.EMPTY));
  }

  getArtistSongs(artist: number): Observable<ArtistSongsDto> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }

  get selectedArtist(): Observable<ArtistDto> {
    return this.selectedArtistSubject.asObservable()
      .distinctUntilChanged();
  }

  selectArtist(artist: ArtistDto) {
    this.selectedArtistSubject.next(artist);
  }

  deselectArtist() {
    this.selectedArtistSubject.next(undefined);
  }

  get selectedSong(): Observable<SongDto> {
    return this.selectedSongSubject.asObservable()
      .distinctUntilChanged();
  }

  selectSong(song: SongDto) {
    this.selectedSongSubject.next(song);
  }

  deselectSong() {
    this.selectedSongSubject.next(undefined);
  }
}
