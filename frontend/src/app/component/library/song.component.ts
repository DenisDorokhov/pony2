import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import {fromEvent, Subscription} from 'rxjs';
import {Playlist, Song} from '../../domain/library.model';
import {PlaybackService} from '../../service/playback.service';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {LibraryService} from '../../service/library.service';
import {ScrollingUtils} from '../../utils/scrolling.utils';
import {UnknownSongPipe} from '../../pipe/unknown-song.pipe';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {PlaybackEvent, PlaybackState} from '../../service/audio-player.service';
import {UnknownGenrePipe} from '../../pipe/unknown-genre.pipe';
import {PlaylistService} from '../../service/playlist.service';
import {NotificationService} from '../../service/notification.service';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbModal
} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistEditComponent} from './modal/playlist-edit.component';
import {PlaylistAddSongComponent} from './modal/playlist-add-song.component';
import {PlaylistDto} from '../../domain/library.dto';

@Component({
    imports: [TranslateModule, UnknownSongPipe, UnknownArtistPipe, UnknownGenrePipe, NgbDropdown, NgbDropdownButtonItem, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle],
    selector: 'pony-song',
    templateUrl: './song.component.html',
    styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy, AfterViewInit {

  private readonly libraryService = inject(LibraryService);
  private readonly playbackService = inject(PlaybackService);
  private readonly playlistService = inject(PlaylistService);
  private readonly notificationService = inject(NotificationService);
  private readonly translateService = inject(TranslateService);
  private readonly modal = inject(NgbModal);

  PlaybackState = PlaybackState;

  @ViewChild('menuDropdown') menuDropdown!: NgbDropdown;
  @ViewChild('menuContainer') menuContainerElement!: ElementRef;

  private _song!: Song;

  isMouseOver = false;
  topPlaylists: Playlist[] = [];
  isLikedSong = false;

  get isTopTrack(): boolean {
    return this.libraryService.isTopTrack(this.song);
  }

  get song(): Song {
    return this._song;
  }

  @Input()
  set song(song: Song) {
    this._song = song;
    this.selected = this.libraryService.selectedSong?.id === this.song.id;
    this.refreshLikeState();
    if (this.playbackService.lastPlaybackEvent.song?.id === this.song.id) {
      this.lastPlaybackEvent = this.playbackService.lastPlaybackEvent;
    } else {
      this.lastPlaybackEvent = undefined;
    }
  }

  @Input() showArtist = false;

  @Output() switchQueueRequested = new EventEmitter<Song>();

  @ViewChild('container') containerElement!: ElementRef;
  selected = false;

  lastPlaybackEvent: PlaybackEvent | undefined;

  private subscriptions: Subscription[] = [];

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeSelectedSong()
      .subscribe(songSelection => {
        this.selected = songSelection != null && songSelection.song.id === this.song.id;
        if (this.selected) {
          if (songSelection?.play) {
            if (this.song.id !== this.playbackService.currentSong?.id) {
              this.libraryService.requestSongPlayback(this.song);
            } else if (this.playbackService.currentPlaybackState !== PlaybackState.PLAYING) {
              this.playbackService.playOrPause();
            }
          }
          if (this.containerElement && !this.menuDropdown?.isOpen()) {
            this.containerElement.nativeElement.focus();
          }
          this.libraryService.finishSongSelection();
        }
      }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.id === this.song.id) {
          this.lastPlaybackEvent = playbackEvent;
        } else {
          this.lastPlaybackEvent = undefined;
        }
      }));
    this.subscriptions.push(this.playlistService.observePlaylists()
      .subscribe(() => this.topPlaylists = this.playlistService.getTopPlaylists(PlaylistDto.Type.NORMAL)));
    this.subscriptions.push(this.playlistService.observeLikePlaylist()
      .subscribe(() => this.refreshLikeState()));
  }

  private refreshLikeState() {
    this.isLikedSong = !this.song || this.playlistService.isLikedSong(this.song.id);
  }

  ngAfterViewInit(): void {
    if (this.selected) {
      this.containerElement.nativeElement.focus();
    }
    this.subscriptions.push(this.libraryService.observeScrollToSongRequest()
      .subscribe(request => {
        if (request.song.id === this.song.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToSong();
        }
      }));
    this.subscriptions.push(fromEvent<KeyboardEvent>(this.containerElement.nativeElement, 'keydown').subscribe(event => {
      if (event.code === 'Enter') {
        this.select();
        this.libraryService.requestSongPlayback(this.song);
        event.preventDefault();
      }
    }));
    this.subscriptions.push(fromEvent(window.document.body, 'touchstart').subscribe(event => {
      if (!this.menuContainerElement.nativeElement.contains(event.target as Node)) {
        this.menuDropdown.close();
      }
    }));
    this.subscriptions.push(fromEvent(window.document.body, 'wheel').subscribe(() =>
      this.menuDropdown.close()));
    this.subscriptions.push(fromEvent(window, 'resize').subscribe(() =>
      this.menuDropdown.close()));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  onDoubleClick(event: MouseEvent) {
    let checkElement: Element | undefined = event.target as Element;
    let buttonClick = false;
    do {
      buttonClick = checkElement?.tagName === 'BUTTON';
      checkElement = checkElement.parentNode as Element;
    } while (!buttonClick && checkElement);
    if (!buttonClick) {
      this.libraryService.requestSongPlayback(this.song);
    }
  }

  onMouseDown(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  onMouseMove() {
    this.isMouseOver = true;
  }

  onMouseLeave() {
    this.isMouseOver = false;
  }

  playNext() {
    this.playbackService.playNext(this.song);
  }

  addToQueue() {
    this.playbackService.addToQueue(this.song);
  }

  createQueue() {
    this.playbackService.createQueue(this.song);
  }

  switchQueue() {
    this.switchQueueRequested.emit(this.song);
  }

  onPlaybackClick() {
    if (this.lastPlaybackEvent?.song?.id === this.song.id) {
      this.playbackService.playOrPause();
    } else {
      this.libraryService.requestSongPlayback(this.song);
    }
  }

  selectOrCreatePlaylist() {
    if (this.topPlaylists.length > 0) {
      const modalRef = this.modal.open(PlaylistAddSongComponent);
      const playlistAddSongComponent: PlaylistAddSongComponent = modalRef.componentInstance;
      playlistAddSongComponent.songs = [this.song];
    } else {
      const modalRef = this.modal.open(PlaylistEditComponent);
      const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
      playlistEditComponent.songs = [this.song];
    }
  }

  addToPlaylist(playlist: Playlist) {
    this.playlistService.addSongToPlaylist(playlist.id, this.song.id).subscribe({
      error: () => this.notificationService.error(
        this.translateService.instant('library.song.addToPlaylistNotificationTitle'),
        this.translateService.instant('library.song.addToPlaylistNotificationTextFailure'),
      ),
    });
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

  download() {
    this.libraryService.downloadSong(this.song!.id);
  }
}
