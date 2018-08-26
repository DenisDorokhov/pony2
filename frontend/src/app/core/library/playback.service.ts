import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Howl} from 'howler';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {Song} from './library.model';
import {PlaylistService} from './playlist.service';

export enum PlaybackState {
  INACTIVE,
  LOADING,
  ERROR,
  READY,
  PLAYING,
  PAUSED,
  ENDED,
}

@Injectable()
export class PlaybackService {

  private _playlistService: PlaylistService;

  private currentPlaylistServiceSubject: BehaviorSubject<PlaylistService | undefined> = new BehaviorSubject<PlaylistService>(undefined);
  private currentStateSubject: BehaviorSubject<PlaybackState> = new BehaviorSubject<PlaybackState>(PlaybackState.INACTIVE);

  private howl: Howl | undefined;
  private currentSongSubscription: Subscription;

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
          this.currentStateSubject.next(PlaybackState.INACTIVE);
        }
      });
    } else {
      this.unloadHowl();
      this.currentStateSubject.next(PlaybackState.INACTIVE);
    }
    this.currentPlaylistServiceSubject.next(playlistService);
  }

  get currentPlaylistService(): Observable<PlaylistService> {
    return this.currentPlaylistServiceSubject.asObservable()
      .distinctUntilChanged();
  }

  get currentState(): Observable<PlaybackState> {
    return this.currentStateSubject.asObservable()
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

  private unloadHowl() {
    if (this.howl) {
      this.howl.unload();
      this.howl = undefined;
    }
  }

  private loadHowlForSong(song: Song) {
    this.unloadHowl();
    console.log(`Loading audio '${song.id} -> ${song.artistName} - ${song.name}'.`);
    this.currentStateSubject.next(PlaybackState.LOADING);
    this.howl = new Howl(<IHowlProperties>{
      src: [song.audioUrl],
      format: [song.fileExtension],
      html5: true,
      onload: () => {
        console.log('Audio loaded.');
        this.currentStateSubject.next(this.howl.playing() ? PlaybackState.PLAYING : PlaybackState.READY);
      },
      onloaderror: (id, error) => {
        console.error(`Audio could not be loaded: "${error}".`);
        this.currentStateSubject.next(PlaybackState.ERROR);
      },
      onplay: () => this.currentStateSubject.next(PlaybackState.PLAYING),
      onplayerror: (id, error) => {
        console.error(`Playback failed: "${error}".`);
        this.currentStateSubject.next(PlaybackState.ERROR);
      },
      onend: () => {
        console.log('Playback finished.');
        this.currentStateSubject.next(PlaybackState.ENDED);
      },
      onpause: () => this.currentStateSubject.next(PlaybackState.PAUSED),
    });
  }
}
