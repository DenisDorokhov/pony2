import {BehaviorSubject, interval, Observable} from 'rxjs';
import {Howl} from 'howler';
import {distinctUntilChanged} from 'rxjs/operators';
import {Song} from '../domain/library.model';

export enum PlaybackState {
  STOPPED = 'STOPPED',
  LOADING = 'LOADING',
  ERROR = 'ERROR',
  PLAYING = 'PLAYING',
  PAUSED = 'PAUSED',
  ENDED = 'ENDED',
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

export class AudioPlayer {

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
    this.firePlaybackEvent(PlaybackState.LOADING, song);
    this.howl = new Howl({
      src: [song.audioUrl],
      format: [song.fileExtension],
      html5: true,
      onload: () => {
        this.firePlaybackEvent(PlaybackState.PLAYING, song, 0);
      },
      onloaderror: (_, error) => {
        console.error('Audio could not be loaded: ' + JSON.stringify(error));
        this.firePlaybackEvent(PlaybackState.ERROR, song);
      },
      onplay: () => {
        this.firePlaybackEvent(PlaybackState.PLAYING, song, this.lastPlaybackEvent.progress);
      },
      onplayerror: (_, error) => {
        console.error('Playback failed: ' + JSON.stringify(error));
        this.firePlaybackEvent(PlaybackState.ERROR, song, this.lastPlaybackEvent.progress);
      },
      onend: () => {
        this.firePlaybackEvent(PlaybackState.ENDED, song, this.lastPlaybackEvent.progress);
      },
      onpause: () => {
        // Workaround for duplicate Howler events.
        this.firePlaybackEvent(PlaybackState.PAUSED, song, this.lastPlaybackEvent.progress);
      }
    });
    // Workaround for Howler bug: https://github.com/goldfire/howler.js/issues/1175
    if (navigator && navigator.mediaSession) {
      navigator.mediaSession.setActionHandler('play', () => this.howl?.play());
      navigator.mediaSession.setActionHandler('pause', () => this.howl?.pause());
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
