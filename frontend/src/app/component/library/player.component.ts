import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {fromEvent, Subscription} from 'rxjs';
import {PlaybackService} from "../../service/playback.service";
import {Song} from "../../domain/library.model";
import {LibraryService} from "../../service/library.service";
import {PageTitleService} from "../../service/page-title.service";
import {ImageLoaderComponent} from "../common/image-loader.component";
import {isMobileBrowser} from "../../utils/mobile.utils";
import {PlaybackEvent, PlaybackState} from "../../service/audio-player.service";

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
  isError = false;
  hasPreviousSong = false;
  hasNextSong = false;
  progress = 0.0; // 0.0 - 1.0
  mouseProgress: number | undefined; // 0.0 - 1.0
  formattedProgress: string | undefined;
  formattedDuration: string | undefined;
  formattedMousePosition: string | undefined;
  queue: Song[] = [];

  private subscriptions: Subscription[] = [];

  constructor(
    private playbackService: PlaybackService,
    private libraryService: LibraryService,
    private translateService: TranslateService,
    private pageTitleService: PageTitleService,
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.playbackService.observeCurrentSong()
      .subscribe(song => this.handleSongSwitch(song)));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.handlePlaybackEvent(playbackEvent)));
    this.subscriptions.push(this.playbackService.observeQueue()
      .subscribe(queue => {
        this.queue = queue;
        this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 1;
        this.hasNextSong = this.playbackService.hasNextSong();
      }));
    this.subscriptions.push(this.playbackService.observeMode()
      .subscribe(() => {
        this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 1;
        this.hasNextSong = this.playbackService.hasNextSong();
      }));
    this.subscriptions.push(fromEvent<KeyboardEvent>(window.document.body,'keydown').subscribe(event => {
      const formElements: string[] = [
        'INPUT', 'LABEL', 'SELECT', 'TEXTAREA', 'BUTTON', 'FIELDSET', 'LEGEND', 'DATALIST', 'OUTPUT', 'OPTION', 'OPTGROUP',
      ];
      if (!document.activeElement || formElements.indexOf(document.activeElement?.tagName) < 0) {
        if (event.code === 'Space') {
          this.playbackService.playOrPause();
          event.preventDefault();
        }
        if (event.key === 'ArrowRight') {
          if (this.playbackService.hasNextSong()) {
            this.playbackService.switchToNextSong().subscribe();
          }
          event.preventDefault();
        }
        if (event.key === 'ArrowLeft') {
          if (this.playbackService.hasPreviousSong()) {
            this.playbackService.rewindToBeginningOrSwitchToPreviousSong().subscribe();
          }
          event.preventDefault();
        }
      }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  switchToPreviousSong() {
    this.playbackService.rewindToBeginningOrSwitchToPreviousSong().subscribe();
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
      this.formattedProgress = song.getRelativeDurationInMinutes(this.progress);
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

  private handlePlaybackEvent(playbackEvent: PlaybackEvent) {
    this.isPlaying = playbackEvent.state === PlaybackState.LOADING || playbackEvent.state === PlaybackState.PLAYING;
    this.isLoading = playbackEvent.state === PlaybackState.LOADING;
    this.isError = playbackEvent.state === PlaybackState.ERROR;
    this.progress = playbackEvent.progress || 0;
    this.formattedProgress = playbackEvent.song ? playbackEvent.song.getRelativeDurationInMinutes(this.progress) : '0:00';
    this.formattedDuration = playbackEvent.song ? playbackEvent.song.durationInMinutes : '0:00';
  }

  private handleSongSwitch(song: Song | undefined) {
    this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 1;
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

  onProgressMouseMove(event: MouseEvent) {
    if (isMobileBrowser()) {
      return;
    }
    const song = this.playbackService.lastPlaybackEvent.song;
    if (song) {
      const progressBar = event.currentTarget as Element;
      const progressBarRect = progressBar.getBoundingClientRect();
      this.mouseProgress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
      this.formattedMousePosition = song.getRelativeDurationInMinutes(this.mouseProgress);
    } else {
      this.mouseProgress = undefined;
      this.formattedMousePosition = undefined;
    }
  }

  onProgressMouseLeave() {
    if (isMobileBrowser()) {
      return;
    }
    this.mouseProgress = undefined;
    this.formattedMousePosition = undefined;
  }
}
