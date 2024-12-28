import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistService} from '../../../service/playlist.service';
import {Subscription} from 'rxjs';
import {Playlist, PlaylistSongs, Song} from '../../../domain/library.model';
import {PlaylistEditComponent} from './playlist-edit.component';
import {FormsModule} from '@angular/forms';
import {LoadingState} from '../../../domain/common.model';
import {NotificationService} from '../../../service/notification.service';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {LargeSongComponent} from './common/large-song.component';
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {CdkDrag, CdkDragDrop, CdkDragStart, CdkDropList} from '@angular/cdk/drag-drop';
import {isMobileBrowser} from '../../../utils/mobile.utils';
import {PlaybackService} from '../../../service/playback.service';
import {LibraryService} from '../../../service/library.service';
import {PlaybackEvent} from '../../../service/audio-player.service';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, FormsModule, NoContentIndicatorComponent, ErrorIndicatorComponent, LoadingIndicatorComponent, LargeSongComponent, CdkFixedSizeVirtualScroll, CdkVirtualScrollViewport, CdkDropList, CdkDrag, CdkVirtualForOf],
  selector: 'pony-playlist',
  templateUrl: './playlist.component.html',
  styleUrls: ['./playlist.component.scss']
})
export class PlaylistComponent implements OnInit, OnDestroy {

  readonly LoadingState = LoadingState;

  readonly rowHeight = LargeSongComponent.HEIGHT;

  playlists!: Playlist[];
  selectedPlaylist: Playlist | undefined;
  selectedPlaylistSongs: PlaylistSongs | undefined;
  primaryLoadingState = LoadingState.LOADING;
  secondaryLoadingState = LoadingState.LOADED;
  selectedIndex = -1;
  dragEnabled = true;
  lastPlaybackEvent: PlaybackEvent | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playlistService: PlaylistService,
    private readonly translateService: TranslateService,
    private readonly notificationService: NotificationService,
    private readonly playbackService: PlaybackService,
    private readonly libraryService: LibraryService,
    private readonly modal: NgbModal,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.dragEnabled = !isMobileBrowser();
    this.subscriptions.push(this.playlistService.observePlaylists().subscribe(playlists => {
      this.playlists = playlists;
      this.selectedPlaylist = this.playlists.length > 0 ? this.playlistService.getTopPlaylists()[0] : undefined;
      this.loadSongs();
    }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.lastPlaybackEvent = playbackEvent));
  }

  private loadSongs() {
    this.primaryLoadingState = LoadingState.LOADING;
    this.playlistService.getPlaylist(this.selectedPlaylist!.id).subscribe({
      next: playlistSongs => {
        this.primaryLoadingState = LoadingState.LOADED;
        this.selectedPlaylistSongs = playlistSongs;
      },
      error: () => this.primaryLoadingState = LoadingState.ERROR
    });
  }

  edit() {
    const modalRef = this.modal.open(PlaylistEditComponent);
    const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
    playlistEditComponent.playlist = this.selectedPlaylist;
  }

  delete() {
    if (window.confirm(this.translateService.instant('playlist.deletionConfirmation'))) {
      this.secondaryLoadingState = LoadingState.LOADING;
      this.playlistService.deletePlaylist(this.selectedPlaylist!.id).subscribe({
        next: () => {
          this.secondaryLoadingState = LoadingState.LOADED;
          this.notificationService.success(
            this.translateService.instant('playlist.notificationTitle'),
            this.translateService.instant('playlist.notificationTextDeletionSuccess'),
          );
        },
        error: () => {
          this.secondaryLoadingState = LoadingState.LOADED;
          this.notificationService.error(
            this.translateService.instant('playlist.notificationTitle'),
            this.translateService.instant('playlist.notificationTextDeletionFailure'),
          );
        }
      });
    }
  }

  onPlaylistChange() {
    this.loadSongs();
  }

  createPlaylist() {
    this.modal.open(PlaylistEditComponent);
  }

  playSongOnDoubleClick(event: MouseEvent, index: number) {
    let checkElement: Node | null = event.target as Node;
    let isButtonClick = false;
    do {
      isButtonClick = checkElement.nodeName === 'BUTTON';
      checkElement = (checkElement as Node).parentNode;
    } while (!isButtonClick && checkElement);
    if (!isButtonClick) {
      this.playbackService.switchQueue(this.selectedPlaylistSongs!.songs.map(next => next.song), index);
    }
  }

  preventDoubleClickDefault(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  goToSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.requestScrollToSong(song);
    this.activeModal.close();
  }

  removeSong(index: number) {
    // TODO: implement
    if (this.selectedIndex === index) {
      this.selectedIndex = -1;
    }
  }

  onDropListDropped(event: CdkDragDrop<any, any>) {
    const toIndex = this.dragFromIndex! - event.previousIndex + event.currentIndex;
    // TODO: implement
    this.dragFromIndex = undefined;
    this.selectedIndex = toIndex;
  }

  private dragFromIndex: number | undefined;

  onDragStarted(event: CdkDragStart) {
    this.dragFromIndex = this.resolveDragItemIndex(event.source.element.nativeElement.id);
  }

  private resolveDragItemIndex(id: string): number {
    return Number(id.replaceAll('queueSong_', ''));
  }

  onWheel(event: WheelEvent) {
    if (this.dragFromIndex !== undefined) {
      event.preventDefault();
    }
  }

  onTouchmove(event: TouchEvent) {
    if (this.dragFromIndex !== undefined) {
      event.preventDefault();
    }
  }

  selectIndex(i: number) {
    this.selectedIndex = i;
  }

  onPlaybackClick(index: number) {
    const playlistSong = this.selectedPlaylistSongs!.songs[index];
    if (playlistSong.song.id === this.lastPlaybackEvent?.song?.id) {
      this.playbackService.playOrPause();
    } else {
      this.playbackService.switchQueue(this.selectedPlaylistSongs!.songs.map(next => next.song), index);
    }
  }
}
