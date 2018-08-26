import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
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

  songTitle: string;
  artworkUrl: string | undefined;
  isPauseAvailable = false;

  progress = 0.0; // 0.0 - 1.0.
  currentTime: string;
  duration: string;

  private song: Song | undefined;
  private playlistService: PlaylistService | undefined;
  private playbackState: PlaybackState;

  private currentSongSubscription: Subscription | undefined;
  private playlistServiceSubscription: Subscription;
  private playbackStateSubscription: Subscription;
  private currentSongProgressSubscription: Subscription;

  constructor(private playbackService: PlaybackService, private translateService: TranslateService) {
  }

  ngOnInit(): void {
    this.playlistServiceSubscription = this.playbackService.currentPlaylistService.subscribe(playlistService =>
      this.updatePlaylistService(playlistService));
    this.playbackStateSubscription = this.playbackService.currentState.subscribe(state =>
      this.updatePlaybackState(state));
    this.currentSongProgressSubscription = this.playbackService.currentSongProgress
      .subscribe(progress => this.updateProgress(progress));
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
    this.playlistService.switchToNextSong().subscribe();
  }

  seek(event: MouseEvent) {
    const progressBar = event.currentTarget as Element;
    const progressBarRect = progressBar.getBoundingClientRect();
    const progress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
    this.playbackService.seek(progress);
  }

  private updatePlaylistService(playlistService: PlaylistService) {
    if (this.currentSongSubscription) {
      this.currentSongSubscription.unsubscribe();
    }
    this.playlistService = playlistService;
    if (playlistService) {
      this.currentSongSubscription = playlistService.currentSong.subscribe(song => {
        this.updateSong(song);
      });
    } else {
      this.updateSong(undefined);
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
    if (song) {
      let artistName = song ? song.artistName : undefined;
      let songName = song ? song.name : undefined;
      if (!artistName) {
        artistName = this.translateService.instant('library.artist.unknownLabel');
      }
      if (!songName) {
        songName = this.translateService.instant('library.song.unknownLabel');
      }
      this.songTitle = this.translateService.instant('player.songTitle', {artistName, songName});
      this.artworkUrl = song.album.largeArtworkUrl;
    } else {
      this.songTitle = this.translateService.instant('player.noSongTitle');
      this.artworkUrl = undefined;
    }
  }

  private updateProgress(progress: number) {
    this.progress = progress;
    this.currentTime = this.song ? this.song.relativeDurationInMinutes(this.progress) : '0:00';
    this.duration = this.song ? this.song.durationInMinutes : '0:00';
  }
}
