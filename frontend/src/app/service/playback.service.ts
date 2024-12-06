import {Injectable} from '@angular/core';
import {BehaviorSubject, defer, Observable, of} from 'rxjs';
import {tap} from 'rxjs/operators';
import {Song} from "../domain/library.model";
import {AuthenticationService} from "./authentication.service";
import {moveItemInArray} from "@angular/cdk/drag-drop";
import {LibraryService} from "./library.service";
import {AudioPlayer, PlaybackEvent, PlaybackState} from "./audio-player.service";

export enum PlaybackMode {
  NORMAL = 'NORMAL',
  REPEAT_ALL = 'REPEAT_ALL',
  REPEAT_ONE = 'REPEAT_ONE',
  SHUFFLE = 'SHUFFLE',
}

interface QueueState {
  songIds: string[];
  originalSongIds: string[] | undefined;
  currentIndex: number;
  progress: number;
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {

  private static readonly STATE_LOCAL_STORAGE_KEY: string = 'pony2.PlaybackService.state';
  private static readonly MODE_LOCAL_STORAGE_KEY: string = 'pony2.PlaybackService.mode';

  private audioPlayer = new AudioPlayer();
  private queueSubject: BehaviorSubject<Song[]> = new BehaviorSubject<Song[]>([]);
  private currentSongSubject: BehaviorSubject<Song | undefined> = new BehaviorSubject<Song | undefined>(undefined);

  private _currentIndex = -1;
  private _queue: Song[] = [];
  private _mode: PlaybackMode;

  private originalQueue: Song[] | undefined;

  constructor(
    private authenticationService: AuthenticationService,
    private libraryService: LibraryService,
  ) {
    this._mode = window.localStorage.getItem(PlaybackService.MODE_LOCAL_STORAGE_KEY) as PlaybackMode || PlaybackMode.NORMAL;
    this.authenticationService.observeLogout()
      .subscribe(() => {
        this.audioPlayer.stop();
        this.currentSongSubject.next(undefined);
        this._queue = [];
        this.queueSubject.next([]);
      });
    this.audioPlayer.observePlaybackEvent()
      .subscribe(playbackEvent => this.handlePlaybackEvent(playbackEvent));
    if (navigator && navigator.mediaSession) {
      navigator.mediaSession.setActionHandler(
        'nexttrack',
        () => {
          console.info("Next track by media key detected.");
          this.switchToNextSong().subscribe();
        }
      );
      navigator.mediaSession.setActionHandler(
        'previoustrack',
        () => {
          console.info("Previous track by media key detected.");
          this.switchToPreviousSong().subscribe();
        }
      );
      navigator.mediaSession.setActionHandler(
        'seekto',
        details => {
          if (this.lastPlaybackEvent.state === PlaybackState.PLAYING || this.lastPlaybackEvent.state === PlaybackState.PAUSED) {
            console.info("Seek request by media session detected.");
            this.audioPlayer.seekToPercentage((details.seekTime || 0) / this.lastPlaybackEvent.song!.duration);
          }
        }
      );
    }
  }

  get lastPlaybackEvent(): PlaybackEvent {
    return this.audioPlayer.lastPlaybackEvent;
  }

  get currentSongIndex(): number {
    return this._currentIndex ?? -1;
  }

  get mode(): PlaybackMode {
    return this._mode;
  }

  set mode(value: PlaybackMode) {
    if (this._mode !== value) {
      const oldMode = this._mode;
      this._mode = value;
      window.localStorage.setItem(PlaybackService.MODE_LOCAL_STORAGE_KEY, value);
      if (this._mode === PlaybackMode.SHUFFLE) {
        this.shuffleQueue(this._queue);
      } else if (oldMode === PlaybackMode.SHUFFLE) {
        this.restoreOriginalQueue();
      } else {
        this.originalQueue = undefined;
      }
      this.storeState();
    }
  }

  private shuffleQueue(queue: Song[]) {
    this.originalQueue = [...queue];
    this._queue = [];
    if (this.originalQueue.length > 0) {
      if (this._currentIndex > -1) {
        this._queue.push(this.originalQueue[this._currentIndex]);
        this._currentIndex = 0;
      }
      this.addRandomSongsToQueue(19);
    }
  }

  private restoreOriginalQueue() {
    const oldQueue = [...this._queue];
    this._queue = this.originalQueue !== undefined ? [...this.originalQueue] : [];
    if (this._currentIndex > -1) {
      const currentSong = oldQueue[this._currentIndex];
      this._currentIndex = this._queue.findIndex(next => next.id === currentSong.id);
    }
    this.queueSubject.next(this._queue.slice());
    this.originalQueue = undefined;
  }

  private addRandomSongsToQueue(count: number) {
    for (let i = 0; i < count; i++) {
      this._queue.push(this.originalQueue![this.randomInt(0, this.originalQueue!.length - 1)]);
    }
    this.queueSubject.next(this._queue.slice());
  }

  private randomInt(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  restoreQueueState(): Observable<any | undefined> {
    const state = this.loadQueueState();
    if (state) {
      let allSongIds = [...state.songIds];
      if (state.originalSongIds !== undefined) {
        allSongIds = allSongIds.concat(state.originalSongIds);
      }
      return this.libraryService.getSongs(allSongIds).pipe(
        tap(fetchedSongs => {
          const idToSong: {[songId: string]: Song} = fetchedSongs.reduce(function(result: any, song) {
            result[song.id] = song;
            return result;
          }, {});
          const currentSongId = state.currentIndex > -1 ? state.songIds[state.currentIndex] : undefined;
          let switchToIndex = -1;
          if (currentSongId && idToSong[currentSongId]) {
            switchToIndex = state.currentIndex;
            for (let i = 0; i < state.currentIndex; i++) {
              if (!idToSong[state.songIds[i]]) {
                switchToIndex--;
              }
            }
          }
          const queue: Song[] = state.songIds.flatMap(songId => idToSong[songId] ? [idToSong[songId]] : []);
          if (state.originalSongIds !== undefined) {
            this.originalQueue = state.originalSongIds.flatMap(songId => idToSong[songId] ? idToSong[songId] : []);
          }
          this._queue = queue;
          this.queueSubject = new BehaviorSubject<Song[]>(this._queue.slice());
          this.switchToIndex(switchToIndex > -1 ? switchToIndex : 0, false);
          this.audioPlayer.pause();
          if (switchToIndex > -1 && state.progress !== undefined) {
            this.audioPlayer.seekToSeconds(state.progress * idToSong[currentSongId!].duration);
          }
        })
      );
    } else {
      return of(undefined);
    }
  }

  private loadQueueState(): QueueState | undefined {
    const localStorageKey = this.resolveQueueStateLocalStorageKey();
    if (localStorageKey) {
      const stateJson = window.localStorage.getItem(localStorageKey);
      if (stateJson) {
        return JSON.parse(stateJson) as QueueState;
      }
    }
    return undefined;
  }

  private resolveQueueStateLocalStorageKey(): string | undefined {
    if (this.authenticationService.isAuthenticated) {
      return PlaybackService.STATE_LOCAL_STORAGE_KEY + '.' + this.authenticationService.currentUser!.id;
    }
    return undefined;
  }

  switchQueue(queue: Song[], switchToIndex: number, play = true) {
    this._queue = queue;
    this.queueSubject = new BehaviorSubject<Song[]>(this._queue.slice());
    this.switchToIndex(switchToIndex, play);
    if (this._mode === PlaybackMode.SHUFFLE) {
      this.shuffleQueue(this._queue);
    }
  }

  private switchToIndex(index: number, play = true): Song {
    const song = this._queue[index];
    this._currentIndex = index;
    this.currentSongSubject.next(song);
    if (play) {
      this.audioPlayer.play(song);
    } else {
      this.audioPlayer.load(song);
    }
    if (this._mode === PlaybackMode.SHUFFLE && this._currentIndex === this._queue.length - 1) {
      this.addRandomSongsToQueue(20);
    }
    return song;
  }

  observeQueue(): Observable<Song[]> {
    return this.queueSubject.asObservable();
  }

  removeFromQueue(index: number): Song {
    const song = this._queue[index];
    if (this._currentIndex === index) {
      this._currentIndex = -1;
      this.currentSongSubject.next(undefined);
      this.audioPlayer.stop();
    } else if (this._currentIndex > index) {
      this._currentIndex--;
    }
    this._queue.splice(index, 1);
    this.queueSubject.next(this._queue.slice());
    this.storeState();
    return song;
  }

  moveInQueue(fromIndex: number, toIndex: number): Song {
    if (this._currentIndex === fromIndex) {
      this._currentIndex = toIndex;
    } else {
      if (fromIndex > toIndex) {
        if (this._currentIndex >= toIndex) {
          this._currentIndex++;
        }
      } else if (fromIndex < toIndex) {
        if (this._currentIndex <= toIndex) {
          this._currentIndex--;
        }
      }
    }
    const fromSong = this._queue[fromIndex];
    moveItemInArray(this._queue, fromIndex, toIndex);
    this.queueSubject.next(this._queue.slice());
    this.storeState();
    return fromSong;
  }

  addToQueue(song: Song): void {
    this._queue.push(song);
    this.queueSubject.next(this._queue.slice());
    this.storeState();
  }

  playNext(song: Song): void {
    if (this._currentIndex === -1) {
      this._queue.push(song);
      this.queueSubject.next(this._queue.slice());
    } else {
      this._queue.splice(this._currentIndex + 1, 0, song);
      this.queueSubject.next(this._queue.slice());
    }
    this.storeState();
  }

  observeCurrentSong(): Observable<Song | undefined> {
    return this.currentSongSubject.asObservable();
  }

  observePlaybackEvent(): Observable<PlaybackEvent> {
    return this.audioPlayer.observePlaybackEvent();
  }

  play(index: number) {
    if (this._currentIndex === index) {
      this.audioPlayer.play(this.currentSongSubject.value);
    } else {
      this.switchToIndex(index);
    }
  }

  playOrPause() {
    if (this.lastPlaybackEvent.state !== PlaybackState.STOPPED) {
      if (this.lastPlaybackEvent.state === PlaybackState.PLAYING) {
        this.audioPlayer.pause();
      } else if (this.lastPlaybackEvent.state === PlaybackState.ERROR) {
        this.audioPlayer.play(this.lastPlaybackEvent.song);
      } else {
        this.audioPlayer.play();
      }
    }
  }

  seek(progress: number) {
    if (
      this.lastPlaybackEvent.state === PlaybackState.PLAYING ||
      this.lastPlaybackEvent.state === PlaybackState.PAUSED ||
      this.lastPlaybackEvent.state === PlaybackState.ENDED
    ) {
      this.audioPlayer.seekToPercentage(progress);
    }
  }

  hasPreviousSong(): boolean {
    return this._mode === PlaybackMode.NORMAL || this._mode === PlaybackMode.SHUFFLE ? this._currentIndex > 0 && this._queue.length > 0 : this._queue.length > 0;
  }

  switchToPreviousSong(): Observable<Song | undefined> {
    const seekToBeginning = () => {
      this.seek(0);
      return of(this.lastPlaybackEvent.song);
    };
    return defer(() => {
      if (
        this.lastPlaybackEvent.state === PlaybackState.PLAYING ||
        this.lastPlaybackEvent.state === PlaybackState.PAUSED
      ) {
        if (!this.hasPreviousSong()) {
          return seekToBeginning();
        }
        if (this.lastPlaybackEvent.progress) {
          const secondsSinceStart = Math.ceil(this.lastPlaybackEvent.progress * this.lastPlaybackEvent.song!.duration);
          if (secondsSinceStart > 3) {
            return seekToBeginning();
          }
        }
      } else if (this.lastPlaybackEvent.state === PlaybackState.ENDED) {
        this.playOrPause();
        return of(this.lastPlaybackEvent.song);
      }
      return defer(() => {
        if (this.hasPreviousSong()) {
          switch (this._mode) {
            case PlaybackMode.REPEAT_ALL:
              if (this._currentIndex === 0) {
                return of(this.switchToIndex(this._queue.length - 1));
              } else {
                return of(this.switchToIndex(this._currentIndex - 1));
              }
            default:
              return of(this.switchToIndex(this._currentIndex - 1));
          }
        } else {
          return of(undefined);
        }
      });
    });
  }

  hasNextSong(): boolean {
    return this._mode === PlaybackMode.NORMAL ? this._currentIndex + 1 < this._queue.length : this._queue.length > 0;
  }

  switchToNextSong(): Observable<Song | undefined> {
    return defer(() => {
      if (this.hasNextSong()) {
        switch (this._mode) {
          case PlaybackMode.REPEAT_ALL:
            if (this._currentIndex >= this._queue.length - 1) {
              return of(this.switchToIndex(0));
            } else {
              return of(this.switchToIndex(this._currentIndex + 1));
            }
          case PlaybackMode.REPEAT_ONE:
            return of(this.switchToIndex(this._currentIndex));
          default:
            return of(this.switchToIndex(this._currentIndex + 1));
        }
      } else {
        return of(undefined);
      }
    });
  }

  private handlePlaybackEvent(playbackEvent: PlaybackEvent) {
    if (playbackEvent.state === PlaybackState.ENDED) {
      this.switchToNextSong().subscribe(() => this.storeState());
    } else if (
      playbackEvent.state === PlaybackState.LOADING
      || playbackEvent.state === PlaybackState.PLAYING
      || playbackEvent.state === PlaybackState.PAUSED
    ) {
      this.storeState();
      if (navigator?.mediaSession) {
        const metadata = new MediaMetadata();
        if (playbackEvent.song?.name) {
          metadata.title = playbackEvent.song.name;
        }
        if (playbackEvent.song?.artistName) {
          metadata.artist = playbackEvent.song.artistName;
        }
        if (playbackEvent.song?.album.name) {
          metadata.album = playbackEvent.song.album.name + (playbackEvent.song.album.year ? ' (' + playbackEvent.song.album.year + ')' : '');
        }
        if (playbackEvent.song?.album.largeArtworkUrl) {
          metadata.artwork = [{src: location.protocol + '//' + location.host + playbackEvent.song.album.largeArtworkUrl}];
        }
        navigator.mediaSession.metadata = metadata;
      }
    }
  }

  private storeState() {
    const localStorageKey = this.resolveQueueStateLocalStorageKey();
    if (localStorageKey) {
      const state = {
        songIds: this._queue.map(next => next.id),
        originalSongIds: this.originalQueue !== undefined ? this.originalQueue.map(next => next.id) : undefined,
        currentIndex: this._currentIndex,
        progress: this.lastPlaybackEvent.state === PlaybackState.ENDED ? undefined : this.lastPlaybackEvent.progress
      } as QueueState;
      window.localStorage.setItem(localStorageKey, JSON.stringify(state));
    }
  }
}
