import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Howl} from 'howler';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {Song} from './library.model';
import {PlaylistService} from './playlist.service';

@Injectable()
export class PlaybackService {

  private _playlistService: PlaylistService;

  private currentStateSubject: BehaviorSubject<PlaybackService.State> = new BehaviorSubject<PlaybackService.State>(PlaybackService.State.INACTIVE);

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
    this.currentStateSubject.next(PlaybackService.State.INACTIVE);
    this.unloadHowl();
    this.currentSongSubscription = this._playlistService.currentSong.subscribe(song => {
      this.unloadHowl();
      if (song) {
        console.log(`Loading audio '${song.id} -> ${song.artistName} - ${song.name}'.`);
        this.loadHowlForSong(song);
      }
    });
  }

  get currentState(): Observable<PlaybackService.State> {
    return this.currentStateSubject.asObservable()
      .distinctUntilChanged();
  }

  play() {
    if (this.howl) {
      console.log('Playback started.');
      this.howl.play();
    } else {
      throw new Error('Audio is not initialized. Is playlist defined?');
    }
  }

  pause() {
    if (this.howl) {
      console.log('Playback paused.');
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
    this.currentStateSubject.next(PlaybackService.State.LOADING);
    this.howl = new Howl(<IHowlProperties>{
      src: [song.audioUrl],
      format: [song.fileExtension],
      html5: true,
      onload: () => {
        console.log('Audio loaded.');
        this.currentStateSubject.next(PlaybackService.State.READY);
      },
      onloaderror: (id, error) => {
        console.error(`Loading audio failed: "${error}".`);
        this.currentStateSubject.next(PlaybackService.State.ERROR);
      },
      onplay: () => this.currentStateSubject.next(PlaybackService.State.PLAYING),
      onplayerror: (id, error) => {
        console.error(`Playback failed: "${error}".`);
        this.currentStateSubject.next(PlaybackService.State.ERROR);
      },
      onend: () => this.currentStateSubject.next(PlaybackService.State.STOPPED),
      onpause: () => this.currentStateSubject.next(PlaybackService.State.PAUSED),
      onstop: () => this.currentStateSubject.next(PlaybackService.State.STOPPED),
    });
  }
}

export namespace PlaybackService {
  export enum State {
    INACTIVE,
    LOADING,
    ERROR,
    READY,
    PLAYING,
    PAUSED,
    STOPPED,
  }
}
