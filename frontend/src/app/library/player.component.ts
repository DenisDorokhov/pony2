import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {Song} from '../core/library/library.model';
import {PlaybackService, PlaybackState} from '../core/library/playback.service';
import {PlaylistService} from '../core/library/playlist.service';

@Component({
  selector: 'pony-player',
  templateUrl: './player.component.html',
  styleUrls: ['./player.component.scss']
})
export class PlayerComponent implements OnInit, OnDestroy {

  song: Song | undefined;
  playbackState: PlaybackState;
  isPauseAvailable = false;
  progress = 0.0; // 0.0 - 1.0.

  private playlistService: PlaylistService | undefined;

  private currentSongSubscription: Subscription | undefined;
  private playlistServiceSubscription: Subscription;
  private playbackStateSubscription: Subscription;

  constructor(private playbackService: PlaybackService) {
  }

  get timeInMinutes(): string {
    return '00:00';
  }

  get durationInMinutes(): string {
    return '00:00';
  }

  ngOnInit(): void {
    this.playlistServiceSubscription = this.playbackService.currentPlaylistService.subscribe(playlistService =>
      this.updatePlaylistService(playlistService));
    this.playbackStateSubscription = this.playbackService.currentState.subscribe(state =>
      this.updatePlaybackState(state));
  }

  ngOnDestroy(): void {
    this.playbackStateSubscription.unsubscribe();
    this.playlistServiceSubscription.unsubscribe();
  }

  switchToPreviousSong() {
    // TODO: implement
  }

  playOrPause() {
    if (this.playbackState !== PlaybackState.INACTIVE) {
      if (this.playbackState === PlaybackState.PLAYING) {
        this.playbackService.pause();
      } else {
        this.playbackService.play();
      }
    }
  }

  switchToNextSong() {
    this.playlistService.switchToNextSong().subscribe((song) => {
      if (song) {
        this.playbackService.play();
      }
    });
  }

  seek(event: MouseEvent) {
    const progressBar = event.currentTarget as Element;
    const progressBarRect = progressBar.getBoundingClientRect();
    this.progress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
  }

  private updatePlaylistService(playlistService: PlaylistService) {
    if (this.currentSongSubscription) {
      this.currentSongSubscription.unsubscribe();
    }
    this.playlistService = playlistService;
    this.updateSong(undefined);
    if (playlistService) {
      this.currentSongSubscription = playlistService.currentSong.subscribe(song => {
        this.updateSong(song);
      });
      this.playbackService.play();
    }
  }

  private updatePlaybackState(state: PlaybackState) {
    this.playbackState = state;
    this.isPauseAvailable = this.playbackState === PlaybackState.LOADING || this.playbackState === PlaybackState.PLAYING;
    if (this.playbackState === PlaybackState.ENDED) {
      this.switchToNextSong();
    }
  }

  private updateSong(song: Song) {
    this.song = song;
    // TODO: implement
  }
}
