import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {
  NgbActiveModal,
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbModal
} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistService} from '../../../service/playlist.service';
import {Subscription} from 'rxjs';
import {Genre, Playlist, PlaylistSong, PlaylistSongs, Song} from '../../../domain/library.model';
import {PlaylistEditComponent} from './playlist-edit.component';
import {FormsModule} from '@angular/forms';
import {LoadingState} from '../../../domain/common.model';
import {NotificationService} from '../../../service/notification.service';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {LargeSongComponent} from './common/large-song.component';
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {CdkDrag, CdkDragDrop, CdkDragStart, CdkDropList, moveItemInArray} from '@angular/cdk/drag-drop';
import {isMobileBrowser} from '../../../utils/mobile.utils';
import {PlaybackService} from '../../../service/playback.service';
import {LibraryService} from '../../../service/library.service';
import {PlaybackEvent} from '../../../service/audio-player.service';
import {PlaylistDto, PlaylistUpdateCommandDto} from '../../../domain/library.dto';
import {formatDuration} from '../../../utils/format.utils';
import {UnknownGenrePipe} from '../../../pipe/unknown-genre.pipe';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, FormsModule, NoContentIndicatorComponent, ErrorIndicatorComponent, LoadingIndicatorComponent, LargeSongComponent, CdkFixedSizeVirtualScroll, CdkVirtualScrollViewport, CdkDropList, CdkDrag, CdkVirtualForOf, NgbDropdown, NgbDropdownMenu, NgbDropdownToggle, NgbDropdownButtonItem, NgbDropdownItem, UnknownGenrePipe],
  selector: 'pony-playlist',
  templateUrl: './playlist.component.html',
  styleUrls: ['./playlist.component.scss']
})
export class PlaylistComponent implements OnInit, OnDestroy {

  readonly LoadingState = LoadingState;
  readonly PlaylistDto = PlaylistDto;

  readonly rowHeight = LargeSongComponent.HEIGHT;

  playlists!: Playlist[];
  selectedPlaylist: Playlist | undefined;
  selectedPlaylistSongs: PlaylistSongs | undefined;
  primaryLoadingState = LoadingState.LOADING;
  secondaryLoadingState = LoadingState.LOADED;
  selectedIndex = -1;
  dragEnabled = true;
  lastPlaybackEvent: PlaybackEvent | undefined;
  duration: string | undefined;
  genres: Genre[] = [];
  selectedGenre: Genre | undefined;
  genreCounter: Record<string, number> = {};
  filteredSongs: PlaylistSong[] = [];

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
      const oldSelectedPlaylist = this.selectedPlaylist;
      this.selectedPlaylist = this.playlists.length > 0 ? this.playlistService.getTopPlaylists()[0] : undefined;
      this.loadSongs(!this.selectedPlaylist || this.selectedPlaylist.id !== oldSelectedPlaylist?.id);
    }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.lastPlaybackEvent = playbackEvent));
    this.subscriptions.push(this.libraryService.observeGenres().subscribe(genres => {
      this.genres = genres;
      this.selectedGenre = this.genres.filter(genre => genre.id === this.selectedGenre?.id)[0];
    }));
  }

  private loadSongs(primaryLoading = true) {
    if (this.selectedPlaylist) {
      if (primaryLoading) {
        this.primaryLoadingState = LoadingState.LOADING;
      } else {
        this.secondaryLoadingState = LoadingState.LOADING;
      }
      this.playlistService.getPlaylist(this.selectedPlaylist.id).subscribe({
        next: playlistSongs => {
          this.primaryLoadingState = this.secondaryLoadingState = LoadingState.LOADED;
          this.selectedPlaylistSongs = playlistSongs;
          this.reloadGenreCounter();
          this.filterSongs();
          this.duration = formatDuration(playlistSongs.songs.reduce((result: number, song: PlaylistSong) => result + song.song.duration, 0), this.translateService);
        },
        error: () => {
          if (primaryLoading) {
            this.primaryLoadingState = LoadingState.ERROR;
          } else {
            this.secondaryLoadingState = LoadingState.ERROR;
          }
        }
      });
    } else {
      this.primaryLoadingState = LoadingState.LOADED;
      this.selectedPlaylistSongs = undefined;
      this.reloadGenreCounter();
      this.filterSongs();
    }
  }

  private filterSongs() {
    if (!this.selectedGenre || !this.selectedPlaylistSongs) {
      this.filteredSongs = this.selectedPlaylistSongs?.songs ?? [];
    } else {
      this.filteredSongs = this.selectedPlaylistSongs!.songs
        .filter(next => next.song.genreId === this.selectedGenre?.id);
    }
  }

  edit() {
    const modalRef = this.modal.open(PlaylistEditComponent);
    const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
    playlistEditComponent.playlist = this.selectedPlaylist;
  }

  delete() {
    if (window.confirm(this.translateService.instant('playlist.deletionConfirmation'))) {
      this.secondaryLoadingState = LoadingState.LOADING;
      this.playlistService.deleteNormalPlaylist(this.selectedPlaylist!.id).subscribe({
        next: () => {
          this.secondaryLoadingState = LoadingState.LOADED;
          this.notificationService.success(
            this.translateService.instant('playlist.notificationTitle'),
            this.translateService.instant('playlist.notificationTextDeletionSuccess'),
          );
        },
        error: () => this.secondaryLoadingState = LoadingState.ERROR
      });
    }
  }

  onPlaylistChange() {
    this.selectGenre(undefined);
    this.loadSongs();
  }

  selectGenre(genre: Genre | undefined) {
    this.selectedGenre = genre;
    this.filterSongs();
  }

  private reloadGenreCounter() {
    this.genreCounter = {};
    if (this.selectedPlaylistSongs) {
      for (const song of this.selectedPlaylistSongs.songs) {
        if (this.genreCounter[song.song.genreId] === undefined) {
          this.genreCounter[song.song.genreId] = 0;
        }
        this.genreCounter[song.song.genreId]++;
      }
    }
  }

  playSongOnDoubleClick(index: number) {
    this.playbackService.switchQueue(this.filteredSongs.map(next => next.song), index);
  }

  goToSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.requestScrollToSong(song);
    this.activeModal.close();
  }

  removeSong(index: number) {
    const command: PlaylistUpdateCommandDto = {
      id: this.selectedPlaylistSongs!.playlist.id,
      overrideName: this.selectedPlaylistSongs!.playlist.name!,
      overriddenSongIds: this.selectedPlaylistSongs!.songs
        .filter((_, nextIndex) => index !== nextIndex)
        .map(next => {
          return {
            id: next.id,
            songId: next.song.id
          } as PlaylistUpdateCommandDto.SongId;
        })
    };
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.updatePlaylist(command).subscribe({
      next: playlistSongs => {
        this.secondaryLoadingState = LoadingState.LOADED;
        this.selectedPlaylistSongs = playlistSongs;
        this.reloadGenreCounter();
        this.filterSongs();
        if (this.selectedIndex === index) {
          this.selectedIndex = -1;
        }
      },
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  onDropListDropped(event: CdkDragDrop<any, any>) {
    const toIndex = this.dragFromIndex! - event.previousIndex + event.currentIndex;
    const songs = [...this.selectedPlaylistSongs!.songs];
    moveItemInArray(songs, this.dragFromIndex!, toIndex);
    this.dragFromIndex = undefined;
    this.selectedPlaylistSongs!.songs = songs;
    this.selectedIndex = toIndex;
    const command: PlaylistUpdateCommandDto = {
      id: this.selectedPlaylistSongs!.playlist.id,
      overrideName: this.selectedPlaylistSongs!.playlist.name!,
      overriddenSongIds: this.selectedPlaylistSongs!.songs
        .map(next => {
          return {
            id: next.id,
            songId: next.song.id
          } as PlaylistUpdateCommandDto.SongId;
        })
    };
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.updatePlaylist(command).subscribe({
      next: () => this.secondaryLoadingState = LoadingState.LOADED,
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  private dragFromIndex: number | undefined;

  onDragStarted(event: CdkDragStart) {
    this.dragFromIndex = this.resolveDragItemIndex(event.source.element.nativeElement.id);
  }

  private resolveDragItemIndex(id: string): number {
    return Number(id.replaceAll('playlistSong_', ''));
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
