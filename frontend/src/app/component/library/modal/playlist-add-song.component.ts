import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {Component, Input, OnInit} from '@angular/core';
import {LoadingState} from '../../../domain/common.model';
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
import {Playlist, Song} from '../../../domain/library.model';
import {NotificationService} from '../../../service/notification.service';
import {PlaylistEditComponent} from './playlist-edit.component';
import {FormsModule} from '@angular/forms';
import {PlaylistDto} from '../../../domain/library.dto';

@Component({
    imports: [TranslateModule, CommonModule, ErrorIndicatorComponent, LoadingIndicatorComponent, FormsModule, NgbDropdown, NgbDropdownButtonItem, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle],
    selector: 'pony-playlist-add-song',
    templateUrl: './playlist-add-song.component.html',
    styleUrls: ['./playlist-add-song.component.scss']
})
export class PlaylistAddSongComponent implements OnInit {

  readonly LoadingState = LoadingState;

  @Input()
  song!: Song;

  loadingState = LoadingState.LOADING;
  savingState = LoadingState.EMPTY;
  playlists: Playlist[] = [];
  selectedPlaylist: Playlist | undefined;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playlistService: PlaylistService,
    private readonly translateService: TranslateService,
    private readonly notificationService: NotificationService,
    private readonly modal: NgbModal,
  ) {
  }

  ngOnInit(): void {
    this.reload();
  }

  private reload() {
    this.loadingState = LoadingState.LOADING;
    this.playlistService.requestPlaylists().subscribe({
      next: playlists => {
        this.playlists = playlists.filter(next => next.type === PlaylistDto.Type.NORMAL);
        this.selectedPlaylist = this.playlists.length > 0 ? this.playlists[0] : undefined;
        this.loadingState = LoadingState.LOADED;
      },
      error: () => this.loadingState = LoadingState.ERROR,
    });
  }

  save() {
    this.savingState = LoadingState.LOADING;
    this.playlistService.addSongToPlaylist(this.selectedPlaylist!.id, this.song.id).subscribe({
      next: () => {
        this.savingState = LoadingState.LOADED;
        this.activeModal.close();
      },
      error: () => {
        this.savingState = LoadingState.LOADED;
        this.notificationService.error(
          this.translateService.instant('playlistAddSong.notificationTitle'),
          this.translateService.instant('playlistAddSong.notificationTextFailure'),
        );
      }
    });
  }

  createPlaylist() {
    const modalRef = this.modal.open(PlaylistEditComponent);
    const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
    playlistEditComponent.songs = [this.song];
    modalRef.closed.subscribe((playlist: Playlist | undefined) => {
      if (playlist) {
        this.activeModal.close();
      }
    });
  }

  selectPlaylist(playlist: Playlist) {
    this.selectedPlaylist = playlist;
  }
}
