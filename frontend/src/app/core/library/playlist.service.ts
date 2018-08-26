import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {Song} from './library.model';

export interface PlaylistService {
  readonly songs: Song[];
  readonly currentSong: Observable<Song | undefined>;
  switchToSong(songId: number): Song | undefined;
  switchToNextSong(): Observable<Song | undefined>;
  switchToPreviousSong(): Song | undefined;
}

export class StaticPlaylistService implements PlaylistService {

  private currentSongSubject = new BehaviorSubject<Song | undefined>(undefined);
  private currentSongIndex = -1;

  constructor(private _songs: Song[]) {
    if (this._songs.length > 0) {
      this.switchToIndex(0);
    }
  }

  get songs(): Song[] {
    return this._songs.slice();
  }

  get currentSong(): Observable<Song | undefined> {
    return this.currentSongSubject.asObservable()
      .distinctUntilChanged();
  }

  switchToSong(songId: number): Song | undefined {
    const targetIndex = this._songs.findIndex(song => song.id === songId);
    if (targetIndex >= 0) {
      return this.switchToIndex(targetIndex);
    } else {
      return undefined;
    }
  }

  switchToNextSong(): Observable<Song | undefined> {
    if (this._songs.length === 0) {
      return undefined;
    }
    if (this._songs.length < this.currentSongIndex - 1) {
      return Observable.of(this.switchToIndex(this.currentSongIndex + 1));
    } else {
      return undefined;
    }
  }

  switchToPreviousSong(): Song | undefined {
    if (this._songs.length === 0) {
      return undefined;
    }
    if (this.currentSongIndex > 0) {
      return this.switchToIndex(this.currentSongIndex - 1);
    } else {
      return undefined;
    }
  }

  private switchToIndex(index: number): Song {
    const song = this._songs[index];
    this.currentSongSubject.next(song);
    this.currentSongIndex = index;
    return song;
  }
}
