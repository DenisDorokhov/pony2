import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {UnknownArtistPipe} from '../../../../pipe/unknown-artist.pipe';
import {UnknownSongPipe} from '../../../../pipe/unknown-song.pipe';
import {ImageLoaderComponent} from '../../../common/image-loader.component';
import {UnknownAlbumPipe} from '../../../../pipe/unknown-album.pipe';
import {UnknownGenrePipe} from '../../../../pipe/unknown-genre.pipe';
import {Playlist, Song} from '../../../../domain/library.model';
import {PlaybackState} from '../../../../service/audio-player.service';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle, NgbModal
} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistService} from '../../../../service/playlist.service';
import {fromEvent, Subscription} from 'rxjs';
import {NotificationService} from '../../../../service/notification.service';
import {PlaybackService} from '../../../../service/playback.service';
import {PlaylistDto} from '../../../../domain/library.dto';
import {PlaylistAddSongComponent} from '../playlist-add-song.component';
import {PlaylistEditComponent} from '../playlist-edit.component';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    UnknownArtistPipe,
    UnknownSongPipe,
    ImageLoaderComponent,
    UnknownAlbumPipe,
    UnknownGenrePipe,
    NgbDropdown,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    NgbDropdownMenu,
    NgbDropdownToggle,
  ],
  selector: 'pony-large-song',
  templateUrl: './large-song.component.html',
  styleUrls: ['./large-song.component.scss']
})
export class LargeSongComponent implements OnInit, OnDestroy {

  static readonly HEIGHT = 76;

  readonly PlaybackState = PlaybackState;

  @Input()
  index!: number;
  @Input()
  overrideIndex: number | undefined;
  @Input()
  selected = false;
  @Input()
  isCurrentSong = false;

  get song(): Song {
    return this._song;
  }

  @Input()
  set song(song: Song) {
    this._song = song;
    this.refreshLikeState();
  }

  @Output()
  doubleClick = new EventEmitter<number>();
  @Output()
  playOrPauseRequested = new EventEmitter<number>();
  @Output()
  goToSongRequested = new EventEmitter<number>();
  @Output()
  removalRequested = new EventEmitter<number>();

  @ViewChild('menuDropdown')
  menuDropdown!: NgbDropdown;

  playbackState: PlaybackState | undefined;
  isMouseOver = false;
  isLikedSong = false;
  topPlaylists: Playlist[] = [];

  private _song!: Song;
  private subscriptions: Subscription[] = [];

  constructor(
    private readonly playlistService: PlaylistService,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
    private readonly playbackService: PlaybackService,
    private readonly modal: NgbModal,
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.playbackState = playbackEvent.state));
    this.subscriptions.push(this.playlistService.observeLikePlaylist()
      .subscribe(() => this.refreshLikeState()));
    this.subscriptions.push(this.playlistService.observePlaylists()
      .subscribe(() => this.topPlaylists = this.playlistService.getTopPlaylists(PlaylistDto.Type.NORMAL)));
    this.subscriptions.push(fromEvent(window.document.body, 'mousewheel').subscribe(() =>
      this.menuDropdown.close()));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  private refreshLikeState() {
    this.isLikedSong = !this.song || this.playlistService.isLikedSong(this.song.id);
  }

  onMouseMove() {
    this.isMouseOver = true;
  }

  onMouseLeave() {
    this.isMouseOver = false;
  }

  onPlaybackClick() {
    this.playOrPauseRequested.emit(this.index);
  }

  goToSong() {
    this.goToSongRequested.emit(this.index);
  }

  removeSong() {
    this.removalRequested.emit(this.index);
  }

  onDoubleClick(event: MouseEvent) {
    let checkElement: Node | null = event.target as Node;
    let isButtonClick = false;
    do {
      isButtonClick = checkElement.nodeName === 'BUTTON';
      checkElement = (checkElement as Node).parentNode;
    } while (!isButtonClick && checkElement);
    if (!isButtonClick) {
      this.doubleClick.emit(this.index);
    }
  }

  preventDoubleClickDefault(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  onLikeClick() {
    if (this.isLikedSong) {
      this.isLikedSong = false;
      this.playlistService.unlikeSong(this.song.id).subscribe({
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
      this.playlistService.likeSong(this.song.id).subscribe({
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

  playNext() {
    this.playbackService.playNext(this.song);
  }

  addToQueue() {
    this.playbackService.addToQueue(this.song);
  }

  createQueue() {
    this.playbackService.createQueue(this.song);
  }

  addToPlaylist(playlist: Playlist) {
    this.playlistService.addSongToPlaylist(playlist.id, this.song.id).subscribe({
      error: () => this.notificationService.error(
        this.translateService.instant('library.song.addToPlaylistNotificationTitle'),
        this.translateService.instant('library.song.addToPlaylistNotificationTextFailure'),
      ),
    });
  }

  selectOrCreatePlaylist() {
    if (this.topPlaylists.length > 0) {
      const modalRef = this.modal.open(PlaylistAddSongComponent);
      const playlistAddSongComponent: PlaylistAddSongComponent = modalRef.componentInstance;
      playlistAddSongComponent.song = this.song;
    } else {
      const modalRef = this.modal.open(PlaylistEditComponent);
      const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
      playlistEditComponent.songs = [this.song];
    }
  }
}
