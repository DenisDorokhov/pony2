import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, of, Subscription} from 'rxjs';
import {tap} from 'rxjs/operators';
import {Song} from '../domain/library.model';
import {AuthenticationService} from './authentication.service';
import {moveItemInArray} from '@angular/cdk/drag-drop';
import {LibraryService} from './library.service';
import {AudioPlayer, PlaybackEvent, PlaybackState} from './audio-player.service';
import {BrowserNotificationService} from './browser-notification.service';
import {PlaybackHistoryService} from './playback-history.service';
import {RandomSongsRequestDto} from '../domain/library.dto';

export enum PlaybackMode {
  NORMAL = 'NORMAL',
  REPEAT_ALL = 'REPEAT_ALL',
  REPEAT_ONE = 'REPEAT_ONE',
  SHUFFLE = 'SHUFFLE',
  RADIO = 'RADIO',
}

interface QueueState {
  queueSongIds: string[];
  originalQueueSongIds: string[] | undefined;
  currentIndex: number;
  progress: number;
  mode: PlaybackMode;
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {

  private static readonly STATE_LOCAL_STORAGE_KEY: string = 'pony2.PlaybackService.state';

  private static readonly RANDOM_SONGS_COUNT = 20;

  private audioPlayer = new AudioPlayer();
  private queueSubject: BehaviorSubject<Song[]> = new BehaviorSubject<Song[]>([]);
  private currentSongSubject: BehaviorSubject<Song | undefined> = new BehaviorSubject<Song | undefined>(undefined);
  private modeSubject = new BehaviorSubject(PlaybackMode.NORMAL);

  private _currentIndex = -1;
  private _queue: Song[] = [];
  private _mode = PlaybackMode.NORMAL;

  private originalQueue: Song[] | undefined;
  private queueShuffleSubscription: Subscription | undefined;

  constructor(
    private readonly authenticationService: AuthenticationService,
    private readonly libraryService: LibraryService,
    private readonly browserNotificationService: BrowserNotificationService,
    private readonly playbackHistoryService: PlaybackHistoryService,
  ) {
    this.authenticationService.observeLogout().subscribe(() => {
        this.audioPlayer.stop();
        this.currentSongSubject.next(undefined);
        this._queue = [];
        this.originalQueue = undefined;
        this.queueShuffleSubscription?.unsubscribe();
        this.queueShuffleSubscription = undefined;
        this.queueSubject.next([]);
      });
    this.audioPlayer.observePlaybackEvent().subscribe(playbackEvent =>
      this.handlePlaybackEvent(playbackEvent));
    if (navigator && navigator.mediaSession) {
      navigator.mediaSession.setActionHandler(
        'nexttrack',
        () => this.switchToNextSong()
      );
      navigator.mediaSession.setActionHandler(
        'previoustrack',
        () => this.rewindToBeginningOrSwitchToPreviousSong()
      );
      navigator.mediaSession.setActionHandler(
        'seekto',
        details => {
          if (this.lastPlaybackEvent.state === PlaybackState.PLAYING || this.lastPlaybackEvent.state === PlaybackState.PAUSED) {
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

  get currentSong(): Song | undefined {
    return this._currentIndex > -1 ? this._queue[this._currentIndex] : undefined;
  }

  get currentPlaybackState(): PlaybackState | undefined {
    return this.lastPlaybackEvent?.state;
  }

  get mode(): PlaybackMode {
    return this._mode;
  }

  set mode(value: PlaybackMode) {
    if (this._mode !== value) {
      this._mode = value;
      this.queueShuffleSubscription?.unsubscribe();
      this.queueShuffleSubscription = undefined;
      if (this._mode === PlaybackMode.SHUFFLE) {
        this.shuffleQueue(this.originalQueue !== undefined ? this.originalQueue : this._queue);
      }
      if (this._mode === PlaybackMode.RADIO) {
        this.shuffleQueue(
          this.originalQueue !== undefined ? this.originalQueue : this._queue,
          () => {
            const genreIds = this._queue.map(next => next.genreId);
            const request = {
              count: PlaybackService.RANDOM_SONGS_COUNT - 1,
              genreIds: Array.from(new Set<string>(genreIds).values()),
              lastArtistId: this._queue[0].album.artist.id
            } as RandomSongsRequestDto;
            return this.libraryService.getRandomSongs(request);
          }
        );
      }
      if (this.originalQueue !== undefined && (
          this._mode === PlaybackMode.NORMAL ||
          this._mode === PlaybackMode.REPEAT_ALL ||
          this._mode === PlaybackMode.REPEAT_ONE
      )) {
        this.restoreOriginalQueue();
      }
      this.storeState();
      this.modeSubject.next(this._mode);
    }
  }

  private shuffleQueue(queue: Song[], songFetcher?: () => Observable<Song[]>) {
    this.originalQueue = [...queue];
    this._queue = [];
    if (this.originalQueue.length > 0) {
      if (this._currentIndex > -1) {
        this._queue.push(this.originalQueue[this._currentIndex]);
        this._currentIndex = 0;
      }
      if (songFetcher) {
        this.queueSubject.next(this._queue.slice());
        this.queueShuffleSubscription?.unsubscribe();
        this.queueShuffleSubscription = songFetcher().subscribe(songs => {
          this.addFetchedRandomSongsToQueue(songs);
          this.queueShuffleSubscription = undefined;
        });
      } else {
        this.addRandomSongsToQueue(PlaybackService.RANDOM_SONGS_COUNT - 1);
      }
    }
  }

  private addFetchedRandomSongsToQueue(songs: Song[]) {
    songs.forEach(song => this._queue.push(song));
    this.queueSubject.next(this._queue.slice());
  }

  private restoreOriginalQueue() {
    const oldQueue = [...this._queue];
    this._queue = this.originalQueue !== undefined ? [...this.originalQueue] : [];
    if (this._currentIndex > -1) {
      const currentSong = oldQueue[this._currentIndex];
      const currentSongIndex = this._queue.findIndex(next => next.id === currentSong.id);
      if (currentSongIndex > -1) {
        this._currentIndex = currentSongIndex;
      } else {
        this._queue.unshift(currentSong);
        this._currentIndex = 0;
      }
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

  observeMode(): Observable<PlaybackMode> {
    return this.modeSubject.asObservable();
  }

  restoreQueueState(): Observable<any | undefined> {
    const state = this.loadQueueState();
    if (state) {
      this._mode = state.mode;
      this.modeSubject.next(this._mode);
      let allSongIds = [...state.queueSongIds];
      if (state.originalQueueSongIds !== undefined) {
        allSongIds = allSongIds.concat(state.originalQueueSongIds);
      }
      return this.libraryService.getSongs(allSongIds).pipe(
        tap(fetchedSongs => {
          const idToSong: Record<string, Song> = fetchedSongs.reduce(function(result: any, song) {
            result[song.id] = song;
            return result;
          }, {});
          const queue: Song[] = state.queueSongIds.flatMap(songId => idToSong[songId] ? [idToSong[songId]] : []);
          if (queue.length > 0) {
            if (state.originalQueueSongIds !== undefined) {
              this.originalQueue = state.originalQueueSongIds.flatMap(songId => idToSong[songId] ? idToSong[songId] : []);
            }
            this._queue = queue;
            this.queueSubject.next(this._queue.slice());
            const currentSongId = state.currentIndex > -1 ? state.queueSongIds[state.currentIndex] : undefined;
            let switchToIndex = -1;
            if (currentSongId && idToSong[currentSongId]) {
              switchToIndex = state.currentIndex;
              for (let i = 0; i < state.currentIndex; i++) {
                if (!idToSong[state.queueSongIds[i]]) {
                  switchToIndex--;
                }
              }
            }
            if (switchToIndex > -1) {
              this.switchToIndex(switchToIndex, false);
              const song = queue[switchToIndex];
              if (this.libraryService.defaultArtistId === song.album.artist.id) {
                this.libraryService.selectSong(song);
                this.libraryService.requestScrollToSong(song);
              }
              this.audioPlayer.pause();
              if (state.progress !== undefined) {
                this.audioPlayer.seekToSeconds(state.progress * idToSong[currentSongId!].duration);
              }
            }
          }
        })
      );
    } else {
      this._mode = PlaybackMode.NORMAL;
      this.modeSubject.next(this._mode);
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
    this.queueSubject.next(this._queue.slice());
    this.switchToIndex(switchToIndex, play);
    if (this._mode === PlaybackMode.SHUFFLE) {
      this.shuffleQueue(this._queue);
    }
    if (this._mode === PlaybackMode.RADIO) {
      const genreIds = this._queue.map(next => next.genreId);
      const request = {
        count: PlaybackService.RANDOM_SONGS_COUNT - 1,
        genreIds: Array.from(new Set<string>(genreIds).values()),
        lastArtistId: this._queue[0].album.artist.id
      } as RandomSongsRequestDto;
      this.shuffleQueue(this._queue, () => this.libraryService.getRandomSongs(request));
    }
  }

  private switchToIndex(index: number, play = true): Song {
    const song = this._queue[index];
    this._currentIndex = index;
    this.currentSongSubject.next(song);
    if (play) {
      this.audioPlayer.play(song);
      this.playbackHistoryService.addSongToHistory(song.id).subscribe();
      this.browserNotificationService.showSongNotification(song);
    } else {
      this.audioPlayer.load(song);
    }
    this.addRandomSongsToQueueIfNeeded();
    return song;
  }

  private addRandomSongsToQueueIfNeeded() {
    if (this._currentIndex === this._queue.length - 1) {
      if (this._mode === PlaybackMode.SHUFFLE) {
        this.addRandomSongsToQueue(PlaybackService.RANDOM_SONGS_COUNT);
      }
      if (this._mode === PlaybackMode.RADIO) {
        const genreIds = this._queue.map(next => next.genreId);
        const request = {
          count: PlaybackService.RANDOM_SONGS_COUNT,
          genreIds: Array.from(new Set<string>(genreIds).values()),
          lastArtistId: this._queue[0].album.artist.id
        } as RandomSongsRequestDto;
        this.libraryService.getRandomSongs(request).subscribe(songs =>
          this.addFetchedRandomSongsToQueue(songs));
      }
    }
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
    this.addRandomSongsToQueueIfNeeded();
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
    this.addRandomSongsToQueueIfNeeded();
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
      const song = this.currentSongSubject.value!;
      this.audioPlayer.play(song);
      this.playbackHistoryService.addSongToHistory(song.id).subscribe();
    } else {
      this.switchToIndex(index);
    }
  }

  playOrPause() {
    if (this.lastPlaybackEvent.state !== PlaybackState.STOPPED) {
      if (this.lastPlaybackEvent.state === PlaybackState.PLAYING) {
        this.audioPlayer.pause();
      } else if (this.lastPlaybackEvent.state === PlaybackState.ERROR && this.lastPlaybackEvent.song) {
        const song = this.lastPlaybackEvent.song;
        this.audioPlayer.play(song);
        this.playbackHistoryService.addSongToHistory(song.id).subscribe();
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
    return this._mode === PlaybackMode.NORMAL || this._mode === PlaybackMode.SHUFFLE || this._mode === PlaybackMode.RADIO ?
      this._currentIndex > 0 && this._queue.length > 0 : this._queue.length > 0;
  }

  rewindToBeginningOrSwitchToPreviousSong(): Song | undefined {
    const seekToBeginning = () => {
      this.seek(0);
      return this.lastPlaybackEvent.song;
    };
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
      return this.lastPlaybackEvent.song;
    }
    if (this.hasPreviousSong()) {
      switch (this._mode) {
        case PlaybackMode.REPEAT_ALL:
          if (this._currentIndex === 0) {
            return this.switchToIndex(this._queue.length - 1);
          } else {
            return this.switchToIndex(this._currentIndex - 1);
          }
        default:
          return this.switchToIndex(this._currentIndex - 1);
      }
    } else {
      return undefined;
    }
  }

  hasNextSong(): boolean {
    return this._mode === PlaybackMode.REPEAT_ONE || this._mode === PlaybackMode.REPEAT_ALL ? this._queue.length > 0 : this._currentIndex + 1 < this._queue.length;
  }

  switchToNextSong(): Song | undefined {
    if (this.hasNextSong()) {
      switch (this._mode) {
        case PlaybackMode.REPEAT_ALL:
          if (this._currentIndex >= this._queue.length - 1) {
            return this.switchToIndex(0);
          } else {
            return this.switchToIndex(this._currentIndex + 1);
          }
        case PlaybackMode.REPEAT_ONE:
          return this.switchToIndex(this._currentIndex);
        default:
          return this.switchToIndex(this._currentIndex + 1);
      }
    } else {
      return undefined;
    }
  }

  private handlePlaybackEvent(playbackEvent: PlaybackEvent) {
    if (playbackEvent.state === PlaybackState.ENDED) {
      this.switchToNextSong();
      this.storeState();
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
        queueSongIds: this._queue.map(next => next.id),
        originalQueueSongIds: this.originalQueue !== undefined ? this.originalQueue.map(next => next.id) : undefined,
        currentIndex: this._currentIndex,
        progress: this.lastPlaybackEvent.state === PlaybackState.ENDED ? undefined : this.lastPlaybackEvent.progress,
        mode: this._mode
      } as QueueState;
      window.localStorage.setItem(localStorageKey, JSON.stringify(state));
    }
  }

  createQueue(song: Song) {
    this.mode = PlaybackMode.NORMAL;
    this._queue = [];
    this._queue.push(song);
    this.queueSubject.next(this._queue.slice());
    this._currentIndex = 0;
    if (song.id !== this.currentSongSubject.value?.id) {
      this.currentSongSubject.next(song);
      this.audioPlayer.play(song);
      this.playbackHistoryService.addSongToHistory(song.id).subscribe();
      this.browserNotificationService.showSongNotification(song);
    }
  }
}
