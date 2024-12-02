import {Injectable} from '@angular/core';
import {BehaviorSubject, defer, interval, Observable, of} from 'rxjs';
import {distinctUntilChanged, tap} from 'rxjs/operators';
import {Song} from "../domain/library.model";
import {Howl, HowlOptions} from "howler";
import {AuthenticationService} from "./authentication.service";
import {moveItemInArray} from "@angular/cdk/drag-drop";
import {LibraryService} from "./library.service";

export enum PlaybackState {
  STOPPED,
  LOADING,
  ERROR,
  PLAYING,
  PAUSED,
  ENDED,
}

export class PlaybackEvent {

  readonly state: PlaybackState;
  readonly song: Song | undefined;
  readonly progress: number | undefined; // From 0.0 to 1.0.

  constructor(state: PlaybackState, song?: Song, progress?: number) {
    this.state = state;
    this.song = song;
    this.progress = progress;
  }
}

class AudioPlayer {

  private playbackEventSubject: BehaviorSubject<PlaybackEvent>;

  private howl: Howl | undefined;

  private lastSeekValue: number | undefined;

  constructor() {
    this.playbackEventSubject = new BehaviorSubject<PlaybackEvent>(new PlaybackEvent(PlaybackState.STOPPED));
    interval(250).subscribe(() => this.fireSongProgressPlaybackEvent());
  }

  get lastPlaybackEvent(): PlaybackEvent {
    return this.playbackEventSubject.value;
  }

  observePlaybackEvent(): Observable<PlaybackEvent> {
    return this.playbackEventSubject.asObservable()
      .pipe(distinctUntilChanged((playbackEvent1: PlaybackEvent, playbackEvent2: PlaybackEvent) => {
        return playbackEvent1.state === playbackEvent2.state
          && playbackEvent1.song === playbackEvent2.song
          && playbackEvent1.progress === playbackEvent2.progress;
      }));
  }

  load(song?: Song) {
    if (song) {
      this.loadHowlForSong(song);
    }
    if (this.howl) {
      this.howl.load();
    } else {
      throw new Error(`Could not load: audio is not initialized. Current state: '${this.lastPlaybackEvent.state}'.`);
    }
  }

  play(song?: Song) {
    if (song) {
      this.loadHowlForSong(song);
    }
    if (this.howl) {
      this.howl.play();
    } else {
      throw new Error(`Could not play: audio is not initialized. Current state: '${this.lastPlaybackEvent.state}'.`);
    }
  }

  pause() {
    if (this.howl) {
      this.howl.pause();
    } else {
      throw new Error(`Could not pause: audio is not initialized. Current state: '${this.lastPlaybackEvent.state}'.`);
    }
  }

  seekToPercentage(progress: number) {
    if (this.lastPlaybackEvent.song) {
      this.seekToSeconds(progress * this.lastPlaybackEvent.song!.duration);
    }
  }

  seekToSeconds(progress: number) {
    if (this.howl) {
      this.howl.seek(progress);
      this.lastSeekValue = progress;
      if (this.lastPlaybackEvent.state === PlaybackState.ENDED) {
        this.howl.pause();
      }
    } else {
      throw new Error(`Could not seek: audio is not initialized. Current state: '${this.lastPlaybackEvent.state}'.`);
    }
  }

  stop() {
    this.unloadHowl();
    this.firePlaybackEvent(PlaybackState.STOPPED);
  }

  private unloadHowl() {
    if (this.howl) {
      this.howl.unload();
      this.howl = undefined;
    }
  }

  private loadHowlForSong(song: Song) {
    this.lastSeekValue = undefined;
    this.unloadHowl();
    console.info(`Loading audio '${song.id} -> ${song.artistName} - ${song.name}'.`);
    this.firePlaybackEvent(PlaybackState.LOADING, song);
    this.howl = new Howl(<HowlOptions>{
      src: [song.audioUrl],
      format: [song.fileExtension],
      html5: true,
      onload: () => {
        console.info('Audio loaded.');
        this.firePlaybackEvent(PlaybackState.PLAYING, song, 0);
      },
      onloaderror: (_, error) => {
        console.error('Audio could not be loaded: ' + JSON.stringify(error));
        this.firePlaybackEvent(PlaybackState.ERROR, song);
      },
      onplay: () => {
        console.info('Playback started / resumed.');
        this.firePlaybackEvent(PlaybackState.PLAYING, song, this.lastPlaybackEvent.progress);
      },
      onplayerror: (_, error) => {
        console.error('Playback failed: ' + JSON.stringify(error));
        this.firePlaybackEvent(PlaybackState.ERROR, song, this.lastPlaybackEvent.progress);
      },
      onend: () => {
        console.info('Playback finished.');
        this.firePlaybackEvent(PlaybackState.ENDED, song, this.lastPlaybackEvent.progress);
      },
      onpause: () => {
        console.info('Playback paused.');
        this.firePlaybackEvent(PlaybackState.PAUSED, song, this.lastPlaybackEvent.progress);
      }
    });
    // Workaround for Howler bug: https://github.com/goldfire/howler.js/issues/1175
    if (navigator && navigator.mediaSession) {
      navigator.mediaSession.setActionHandler('play', () => {
        console.info("Play by media key detected.");
        this.howl?.play();
      });
      navigator.mediaSession.setActionHandler('pause', () => {
        console.info("Pause by media key detected.");
        this.howl?.pause();
      });
    }
  }

  private firePlaybackEvent(state: PlaybackState, song?: Song, progress?: number) {
    this.playbackEventSubject.next(new PlaybackEvent(state, song, progress));
  }

  private fireSongProgressPlaybackEvent() {
    if (this.lastPlaybackEvent.state === PlaybackState.PLAYING || this.lastPlaybackEvent.state === PlaybackState.PAUSED) {
      const seek = this.howl!.seek() as number;
      if (!isNaN(seek)) {
        // Workaround for Howler that returns zero seek value after preloading without playing.
        const progress = seek > 0 ? seek / this.lastPlaybackEvent.song!.duration : (this.lastSeekValue || 0) / this.lastPlaybackEvent.song!.duration;
        if (this.lastPlaybackEvent.progress !== progress) {
          this.firePlaybackEvent(this.lastPlaybackEvent.state, this.lastPlaybackEvent.song, progress);
        }
      }
    }
  }
}

interface QueueState {
  songIds: string[];
  currentIndex: number;
  progress: number;
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {

  private static readonly STATE_LOCAL_STORAGE_KEY: string = 'pony2.PlaybackService.state';

  private audioPlayer = new AudioPlayer();
  private queueSubject: BehaviorSubject<Song[]> = new BehaviorSubject<Song[]>([]);
  private currentSongSubject: BehaviorSubject<Song | undefined> = new BehaviorSubject<Song | undefined>(undefined);

  private _currentIndex = -1;
  private _queue: Song[] = [];

  constructor(
    private authenticationService: AuthenticationService,
    private libraryService: LibraryService,
  ) {
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

  restoreQueueState(): Observable<any | undefined> {
    const state = this.loadQueueState();
    if (state) {
      return this.libraryService.getSongs(state.songIds).pipe(
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
          this.switchQueue(queue, switchToIndex > -1 ? switchToIndex : 0, false);
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
    return this._currentIndex > 0 && this._queue.length > 0;
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
          return of(this.switchToIndex(this._currentIndex - 1));
        } else {
          return of(undefined);
        }
      });
    });
  }

  hasNextSong(): boolean {
    return this._currentIndex + 1 < this._queue.length;
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
        currentIndex: this._currentIndex,
        progress: this.lastPlaybackEvent.state === PlaybackState.ENDED ? undefined : this.lastPlaybackEvent.progress
      } as QueueState;
      window.localStorage.setItem(localStorageKey, JSON.stringify(state));
    }
  }
}
