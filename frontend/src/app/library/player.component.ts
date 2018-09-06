import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs/Subscription';
import {NotificationService, NotificationType} from '../core/common/notification.service';
import {Song} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';
import {PageTitleService} from '../core/library/page-title.service';
import {PlaybackEvent, PlaybackService, PlaybackState} from '../core/library/playback.service';

@Component({
  selector: 'pony-player',
  templateUrl: './player.component.html',
  styleUrls: ['./player.component.scss']
})
export class PlayerComponent implements OnInit, OnDestroy {

  songTitle: string;
  artworkUrl: string | undefined;
  isPlaying = false;
  hasPreviousSong = false;
  hasNextSong = false;
  progress = 0.0; // 0.0 - 1.0.
  formattedProgress: string;
  formattedDuration: string;

  private currentSongSubscription: Subscription;
  private playbackEventSubscription: Subscription;

  constructor(
    private playbackService: PlaybackService,
    private libraryService: LibraryService,
    private translateService: TranslateService,
    private notificationService: NotificationService,
    private pageTitleService: PageTitleService,
  ) {
  }

  ngOnInit(): void {
    this.currentSongSubscription = this.playbackService.observeCurrentSong()
      .subscribe(song => this.handleSongSwitch(song));
    this.playbackEventSubscription = this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.handlePlaybackEvent(playbackEvent));
  }

  ngOnDestroy(): void {
    this.playbackEventSubscription.unsubscribe();
    this.currentSongSubscription.unsubscribe();
  }

  switchToPreviousSong() {
    this.playbackService.switchToPreviousSong().subscribe();
  }

  playOrPause() {
    if (this.playbackService.lastPlaybackEvent.state === PlaybackState.STOPPED) {
      this.libraryService.requestSongPlayback(this.libraryService.selectedSong);
    } else {
      this.playbackService.playOrPause();
    }
  }

  switchToNextSong() {
    this.playbackService.switchToNextSong().subscribe();
  }

  seek(event: MouseEvent) {
    const progressBar = event.currentTarget as Element;
    const progressBarRect = progressBar.getBoundingClientRect();
    const progress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
    this.playbackService.seek(progress);
  }

  selectCurrentSong() {
    const song = this.playbackService.lastPlaybackEvent.song;
    if (song) {
      this.libraryService.selectArtist(song.album.artist);
      this.libraryService.selectSong(song);
      this.libraryService.startScrollToSong(song);
    }
  }
  
  @HostListener('window:beforeunload', ['$event'])
  confirmWindowClosing(event: Event) {
    if (this.isPlaying) {
      event.returnValue = this.translateService.instant('player.windowCloseConfirmation');
    }
  }

  private handlePlaybackEvent(playbackEvent: PlaybackEvent) {
    this.isPlaying = playbackEvent.state === PlaybackState.LOADING || playbackEvent.state === PlaybackState.PLAYING;
    this.progress = playbackEvent.progress || 0;
    this.formattedProgress = playbackEvent.song ? playbackEvent.song.relativeDurationInMinutes(this.progress) : '0:00';
    this.formattedDuration = playbackEvent.song ? playbackEvent.song.durationInMinutes : '0:00';
    if (playbackEvent.state === PlaybackState.ERROR) {
      this.notificationService.showNotification({
        text: this.translateService.instant('player.playbackFailed'),
        type: NotificationType.ERROR
      });
    }
  }

  private handleSongSwitch(song: Song) {
    this.hasPreviousSong = this.playbackService.hasPreviousSong();
    this.hasNextSong = this.playbackService.hasNextSong();
    this.pageTitleService.song = song;
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
}
