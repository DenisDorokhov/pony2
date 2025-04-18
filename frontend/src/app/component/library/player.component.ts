import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {fromEvent, Subscription} from 'rxjs';
import {PlaybackService} from '../../service/playback.service';
import {Song} from '../../domain/library.model';
import {LibraryService} from '../../service/library.service';
import {PageTitleService} from '../../service/page-title.service';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {isMobileBrowser} from '../../utils/mobile.utils';
import {PlaybackEvent, PlaybackState} from '../../service/audio-player.service';
import {PlaylistService} from '../../service/playlist.service';
import {NotificationService} from '../../service/notification.service';
import {ArtworkComponent} from './modal/artwork.component';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    imports: [ImageLoaderComponent, TranslateModule],
    selector: 'pony-player',
    templateUrl: './player.component.html',
    styleUrls: ['./player.component.scss']
})
export class PlayerComponent implements OnInit, OnDestroy {

  song: Song | undefined;
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
  isLikedSong = false;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly playbackService: PlaybackService,
    private readonly libraryService: LibraryService,
    private readonly translateService: TranslateService,
    private readonly pageTitleService: PageTitleService,
    private readonly playlistService: PlaylistService,
    private readonly notificationService: NotificationService,
    private readonly modal: NgbModal,
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
        this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 0;
        this.hasNextSong = this.playbackService.hasNextSong();
      }));
    this.subscriptions.push(this.playbackService.observeMode()
      .subscribe(() => {
        this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 0;
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
            this.playbackService.switchToNextSong();
          }
          event.preventDefault();
        }
        if (event.key === 'ArrowLeft') {
          if (this.playbackService.hasPreviousSong()) {
            this.playbackService.rewindToBeginningOrSwitchToPreviousSong();
          }
          event.preventDefault();
        }
      }
    }));
    this.subscriptions.push(this.playlistService.observeLikePlaylist()
      .subscribe(() => this.refreshLikeState()));
  }

  private refreshLikeState() {
    this.isLikedSong = !this.song || this.playlistService.isLikedSong(this.song.id);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  switchToPreviousSong() {
    this.playbackService.rewindToBeginningOrSwitchToPreviousSong();
  }

  playOrPause() {
    if (this.playbackService.lastPlaybackEvent.state === PlaybackState.STOPPED) {
      this.libraryService.requestSongPlayback();
    } else {
      this.playbackService.playOrPause();
    }
  }

  switchToNextSong() {
    this.playbackService.switchToNextSong();
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
      this.libraryService.requestScrollToSong(song);
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
    this.hasPreviousSong = this.playbackService.hasPreviousSong() || this.queue.length > 0;
    this.hasNextSong = this.playbackService.hasNextSong();
    this.song = song;
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
    this.refreshLikeState();
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

  onLikeClick() {
    if (this.isLikedSong) {
      this.isLikedSong = false;
      this.playlistService.unlikeSong(this.song!.id).subscribe({
        error: () => {
          this.isLikedSong = true;
          this.notificationService.error(
            this.translateService.instant('library.song.unlikeNotificationTitle'),
            this.translateService.instant('library.song.unlikeNotificationTextFailure')
          );
        }
      });
    } else {
      this.isLikedSong = true;
      this.playlistService.likeSong(this.song!.id).subscribe({
        error: () => {
          this.isLikedSong = false;
          this.notificationService.error(
            this.translateService.instant('library.song.likeNotificationTitle'),
            this.translateService.instant('library.song.likeNotificationTextFailure')
          );
        }
      });
    }
  }

  openArtwork() {
    if (this.artworkUrl) {
      const modalRef = this.modal.open(ArtworkComponent, {size: '400px'});
      const userComponent: ArtworkComponent = modalRef.componentInstance;
      userComponent.url = this.artworkUrl;
    }
  }
}
