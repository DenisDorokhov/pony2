import {BehaviorSubject, defer, Observable, of} from 'rxjs';
import {distinctUntilChanged} from 'rxjs/operators';
import {Song} from './library.model';
import {moveItemInArray} from "@angular/cdk/drag-drop";

export interface Playlist {

  readonly queue: Song[];
  readonly currentIndex: number;
  readonly currentSong: Song | undefined;

  observeQueue(): Observable<Song[]>;
  observeCurrentSong(): Observable<Song | undefined>;

  hasNextSong(): boolean;
  hasPreviousSong(): boolean;

  switchToIndex(index: number): Song;
  switchToSong(songId: string): Song | undefined;
  switchToNextSong(): Observable<Song | undefined>;
  switchToPreviousSong(): Observable<Song | undefined>;

  removeSong(index: number): Song;
  moveSong(fromIndex: number, toIndex: number): Song;
}

export class StaticPlaylist implements Playlist {

  private _currentIndex = -1;

  private currentSongSubject = new BehaviorSubject<Song | undefined>(undefined);
  private queueSubject: BehaviorSubject<Song[]>;

  constructor(private _queue: Song[]) {
    this.queueSubject = new BehaviorSubject<Song[]>(this._queue.slice());
    if (this._queue.length > 0) {
      this.switchToIndex(0);
    }
  }

  get queue(): Song[] {
    return this._queue.slice();
  }

  get currentSong(): Song | undefined {
    return this.currentSongSubject.value;
  }

  get currentIndex(): number {
    return this._currentIndex;
  }

  observeQueue(): Observable<Song[]> {
    return this.queueSubject.asObservable();
  }

  observeCurrentSong(): Observable<Song | undefined> {
    return this.currentSongSubject.asObservable()
      .pipe(distinctUntilChanged());
  }

  hasNextSong(): boolean {
    return this._currentIndex + 1 < this._queue.length;
  }

  hasPreviousSong(): boolean {
    return this._currentIndex > 0 && this._queue.length > 0;
  }

  switchToIndex(index: number): Song {
    const song = this._queue[index];
    this._currentIndex = index;
    this.currentSongSubject.next(song);
    return song;
  }

  switchToSong(songId: string): Song | undefined {
    const targetIndex = this._queue.findIndex(song => song.id === songId);
    if (targetIndex >= 0) {
      return this.switchToIndex(targetIndex);
    } else {
      return undefined;
    }
  }

  switchToNextSong(): Observable<Song | undefined> {
    return defer(() => {
      if (this.hasNextSong()) {
        return of(this.switchToIndex(this._currentIndex + 1));
      } else {
        return of(undefined);
      }
    });
  }

  switchToPreviousSong(): Observable<Song | undefined> {
    return defer(() => {
      if (this.hasPreviousSong()) {
        return of(this.switchToIndex(this._currentIndex - 1));
      } else {
        return of(undefined);
      }
    });
  }

  removeSong(index: number): Song {
    const song = this._queue[index];
    if (this._currentIndex === index) {
      this._currentIndex = -1;
      this.currentSongSubject.next(undefined);
    } else if (this._currentIndex > index) {
      this._currentIndex--;
    }
    this._queue.splice(index, 1);
    this.queueSubject.next(this._queue.slice());
    return song;
  }

  moveSong(fromIndex: number, toIndex: number): Song {
    if (this._currentIndex === fromIndex) {
      this._currentIndex = toIndex;
    } else if (this._currentIndex < toIndex) {
      this._currentIndex++;
    }
    const fromSong = this._queue[fromIndex];
    moveItemInArray(this._queue, fromIndex, toIndex);
    this.queueSubject.next(this._queue.slice());
    return fromSong;
  }
}
