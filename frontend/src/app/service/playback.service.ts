import {Injectable} from '@angular/core';
import {BehaviorSubject, defer, interval, Observable, of, Subscription} from 'rxjs';
import {distinctUntilChanged} from 'rxjs/operators';
import {Song} from "../domain/library.model";
import {Howl, HowlOptions} from "howler";
import {AuthenticationService} from "./authentication.service";
import {Playlist} from "../domain/playlist.model";

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

  seek(progress: number) {
    if (this.howl) {
      this.howl.seek(progress * this.lastPlaybackEvent.song!.duration);
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
      onloaderror: () => {
        console.error('Audio could not be loaded.');
        this.firePlaybackEvent(PlaybackState.ERROR, song);
      },
      onplay: () => {
        console.info('Playback started / resumed.');
        this.firePlaybackEvent(PlaybackState.PLAYING, song, this.lastPlaybackEvent.progress);
      },
      onplayerror: () => {
        console.error('Playback failed.');
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
        const progress = seek / this.lastPlaybackEvent.song!.duration;
        if (this.lastPlaybackEvent.progress !== progress) {
          this.firePlaybackEvent(this.lastPlaybackEvent.state, this.lastPlaybackEvent.song, progress);
        }
      }
    }
  }
}

@Injectable({
  providedIn: 'root'
})
export class PlaybackService {

  private audioPlayer = new AudioPlayer();

  private playlist: Playlist | undefined;

  private queueSubject: BehaviorSubject<Song[]> = new BehaviorSubject<Song[]>([]);
  private currentSongSubject: BehaviorSubject<Song | undefined> = new BehaviorSubject<Song | undefined>(undefined);

  private queueSubscription: Subscription | undefined;
  private currentSongSubscription: Subscription | undefined;

  constructor(
    private authenticationService: AuthenticationService
  ) {
    this.authenticationService.observeLogout()
      .subscribe(() => {
        this.audioPlayer.stop();
        this.currentSongSubject.next(undefined);
        this.queueSubject.next([]);
        this.queueSubscription?.unsubscribe();
        this.currentSongSubscription?.unsubscribe();
        this.playlist = undefined;
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
            this.audioPlayer.seek((details.seekTime || 0) / this.lastPlaybackEvent.song!.duration);
          }
        }
      );
    }
  }

  get lastPlaybackEvent(): PlaybackEvent {
    return this.audioPlayer.lastPlaybackEvent;
  }

  get currentSongIndex(): number {
    return this.playlist?.currentIndex ?? -1;
  }

  observeQueue(): Observable<Song[]> {
    return this.queueSubject.asObservable();
  }

  removeSongFromQueue(index: number): Song {
    return this.playlist!.removeSong(index);
  }

  moveSongInQueue(fromIndex: number, toIndex: number): Song {
    return this.playlist!.moveSong(fromIndex, toIndex);
  }

  observeCurrentSong(): Observable<Song | undefined> {
    return this.currentSongSubject.asObservable();
  }

  observePlaybackEvent(): Observable<PlaybackEvent> {
    return this.audioPlayer.observePlaybackEvent();
  }

  switchPlaylist(playlist: Playlist) {
    if (this.queueSubscription) {
      this.queueSubscription.unsubscribe();
    }
    if (this.currentSongSubscription) {
      this.currentSongSubscription.unsubscribe();
    }
    this.playlist = playlist;
    this.queueSubscription = playlist.observeQueue()
      .subscribe(queue => this.queueSubject.next(queue));
    this.currentSongSubscription = playlist.observeCurrentSong()
      .subscribe(song => {
        if (song) {
          this.audioPlayer.play(song);
        } else {
          this.audioPlayer.stop();
        }
        this.currentSongSubject.next(song);
      });
  }

  play(index: number) {
    if (this.playlist!.currentIndex === index) {
      this.audioPlayer.play(this.playlist!.currentSong);
    } else {
      this.playlist!.switchToIndex(index);
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
    if (this.lastPlaybackEvent.state === PlaybackState.PLAYING || this.lastPlaybackEvent.state === PlaybackState.PAUSED) {
      this.audioPlayer.seek(progress);
    }
  }

  hasPreviousSong(): boolean {
    return this.playlist !== undefined && this.playlist.queue.length > 0;
  }

  switchToPreviousSong(): Observable<Song | undefined> {
    const seekToBeginning = () => {
      this.seek(0);
      return of(this.lastPlaybackEvent.song);
    };
    return defer(() => {
      if (this.lastPlaybackEvent.state === PlaybackState.PLAYING || this.lastPlaybackEvent.state === PlaybackState.PAUSED) {
        if (!this.playlist!.hasPreviousSong()) {
          return seekToBeginning();
        }
        if (this.lastPlaybackEvent.progress) {
          const secondsSinceStart = Math.ceil(this.lastPlaybackEvent.progress * this.lastPlaybackEvent.song!.duration);
          if (secondsSinceStart > 3) {
            return seekToBeginning();
          }
        }
      }
      return this.playlist!.switchToPreviousSong();
    });
  }

  hasNextSong(): boolean {
    return this.playlist !== undefined && this.playlist.hasNextSong();
  }

  switchToNextSong(): Observable<Song | undefined> {
    return this.playlist!.switchToNextSong();
  }

  private handlePlaybackEvent(playbackEvent: PlaybackEvent) {
    if (playbackEvent.state === PlaybackState.ENDED) {
      this.playlist!.switchToNextSong().subscribe();
    }
    if (
      playbackEvent.state === PlaybackState.LOADING
      || playbackEvent.state === PlaybackState.PLAYING
      || playbackEvent.state === PlaybackState.PAUSED
    ) {
      if (navigator && navigator.mediaSession && playbackEvent.song) {
        const metadata = new MediaMetadata();
        if (playbackEvent.song.name) {
          metadata.title = playbackEvent.song.name;
        }
        if (playbackEvent.song.artistName) {
          metadata.artist = playbackEvent.song.artistName;
        }
        if (playbackEvent.song.album.name) {
          metadata.album = playbackEvent.song.album.name + (playbackEvent.song.album.year ? ' (' + playbackEvent.song.album.year + ')' : '');
        }
        if (playbackEvent.song.album.largeArtworkUrl) {
          metadata.artwork = [{src: location.protocol + '//' + location.host + playbackEvent.song.album.largeArtworkUrl}];
        }
        navigator.mediaSession.metadata = metadata;
      }
    }
  }
}
