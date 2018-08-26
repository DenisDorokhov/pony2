import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Howl} from 'howler';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {Song} from './library.model';
import {PlaylistService} from './playlist.service';
import {interval} from 'rxjs';

export enum PlaybackState {
  INACTIVE,
  LOADING,
  ERROR,
  PLAYING,
  PAUSED,
  ENDED,
}

@Injectable()
export class PlaybackService {

  private _playlistService: PlaylistService;

  private currentPlaylistServiceSubject: BehaviorSubject<PlaylistService | undefined> = new BehaviorSubject<PlaylistService>(undefined);
  private currentPlaybackStateSubject: BehaviorSubject<PlaybackState> = new BehaviorSubject<PlaybackState>(PlaybackState.INACTIVE);
  private currentSongProgressSubject: BehaviorSubject<number> = new BehaviorSubject<number>(0);

  private currentSongSubscription: Subscription;

  private howl: Howl | undefined;

  constructor() {
    interval(50)
      .subscribe(() => this.updateSongProgress());
  }

  get playlistService(): PlaylistService {
    return this._playlistService;
  }

  set playlistService(playlistService: PlaylistService) {
    if (this._playlistService) {
      this.currentSongSubscription.unsubscribe();
    }
    this._playlistService = playlistService;
    if (this._playlistService) {
      this.currentSongSubscription = this._playlistService.currentSong.subscribe(song => {
        if (song) {
          this.loadHowlForSong(song);
        } else {
          this.unloadHowl();
          this.currentPlaybackStateSubject.next(PlaybackState.INACTIVE);
        }
      });
    } else {
      this.unloadHowl();
      this.currentPlaybackStateSubject.next(PlaybackState.INACTIVE);
    }
    this.currentPlaylistServiceSubject.next(playlistService);
  }

  get currentPlaylistService(): Observable<PlaylistService> {
    return this.currentPlaylistServiceSubject.asObservable()
      .distinctUntilChanged();
  }

  get currentState(): Observable<PlaybackState> {
    return this.currentPlaybackStateSubject.asObservable()
      .distinctUntilChanged();
  }

  get currentSongProgress(): Observable<number> {
    return this.currentSongProgressSubject.asObservable()
      .distinctUntilChanged();
  }

  play() {
    if (this.howl) {
      console.log('Playback requested.');
      this.howl.play();
    } else {
      throw new Error('Audio is not initialized. Is playlist defined?');
    }
  }

  pause() {
    if (this.howl) {
      console.log('Suspending playback.');
      this.howl.pause();
    } else {
      throw new Error('Audio is not initialized. Is playlist defined?');
    }
  }

  seek(progress: number) {
    if (this.howl) {
      const duration = this.howl.duration();
      if (duration) {
        this.howl.seek(progress * duration);

      }
    } else {
      throw new Error('Audio is not initialized. Is playlist defined?');
    }
  }

  private unloadHowl() {
    if (this.howl) {
      this.howl.unload();
      this.howl = undefined;
    }
    this.currentSongProgressSubject.next(0);
  }

  private loadHowlForSong(song: Song) {
    this.unloadHowl();
    console.log(`Loading audio '${song.id} -> ${song.artistName} - ${song.name}'.`);
    this.currentPlaybackStateSubject.next(PlaybackState.LOADING);
    this.howl = new Howl(<IHowlProperties>{
      src: [song.audioUrl],
      format: [song.fileExtension],
      html5: true,
      onload: () => {
        console.log('Audio loaded.');
        this.howl.play();
        this.currentPlaybackStateSubject.next(PlaybackState.PLAYING);
      },
      onloaderror: (id, error) => {
        console.error(`Audio could not be loaded: "${error}".`);
        this.currentPlaybackStateSubject.next(PlaybackState.ERROR);
      },
      onplay: () => this.currentPlaybackStateSubject.next(PlaybackState.PLAYING),
      onplayerror: (id, error) => {
        console.error(`Playback failed: "${error}".`);
        this.currentPlaybackStateSubject.next(PlaybackState.ERROR);
      },
      onend: () => {
        console.log('Playback finished.');
        this.currentPlaybackStateSubject.next(PlaybackState.ENDED);
      },
      onpause: () => this.currentPlaybackStateSubject.next(PlaybackState.PAUSED),
    });
  }

  private updateSongProgress() {
    if (this.howl) {
      const duration = this.howl.duration();
      const seek = this.howl.seek() as number;
      if (duration && !isNaN(seek)) {
        this.currentSongProgressSubject.next(seek / duration);
      }
    }
  }
}
