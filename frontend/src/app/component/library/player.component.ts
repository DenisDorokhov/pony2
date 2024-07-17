import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {PlaybackEvent, PlaybackService, PlaybackState} from "../../service/playback.service";
import {Song} from "../../domain/library.model";
import {LibraryService} from "../../service/library.service";
import {PageTitleService} from "../../service/page-title.service";
import {ImageLoaderComponent} from "../common/image-loader.component";

@Component({
  standalone: true,
  imports: [ImageLoaderComponent],
  selector: 'pony-player',
  templateUrl: './player.component.html',
  styleUrls: ['./player.component.scss']
})
export class PlayerComponent implements OnInit, OnDestroy {

  songTitle: string | undefined;
  artworkUrl: string | undefined;
  isPlaying = false;
  isLoading = false;
  hasPreviousSong = false;
  hasNextSong = false;
  progress = 0.0; // 0.0 - 1.0.
  formattedProgress: string | undefined;
  formattedDuration: string | undefined;

  private currentSongSubscription: Subscription | undefined;
  private playbackEventSubscription: Subscription | undefined;

  constructor(
    private playbackService: PlaybackService,
    private libraryService: LibraryService,
    private translateService: TranslateService,
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
    this.playbackEventSubscription?.unsubscribe();
    this.currentSongSubscription?.unsubscribe();
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
    const song = this.playbackService.lastPlaybackEvent.song;
    if (song) {
      const progressBar = event.currentTarget as Element;
      const progressBarRect = progressBar.getBoundingClientRect();
      const progress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
      this.progress = progress || 0;
      this.isLoading = true;
      this.formattedProgress = song.relativeDurationInMinutes(this.progress);
      this.playbackService.seek(this.progress);
    }
  }

  selectCurrentSong() {
    const song = this.playbackService.lastPlaybackEvent.song;
    if (song) {
      this.libraryService.selectArtistAndMakeDefault(song.album.artist);
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
    this.isLoading = playbackEvent.state === PlaybackState.LOADING;
    this.progress = playbackEvent.progress || 0;
    this.formattedProgress = playbackEvent.song ? playbackEvent.song.relativeDurationInMinutes(this.progress) : '0:00';
    this.formattedDuration = playbackEvent.song ? playbackEvent.song.durationInMinutes : '0:00';
  }

  private handleSongSwitch(song: Song | undefined) {
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
