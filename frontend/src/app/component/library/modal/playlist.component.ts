import {Component, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistService} from '../../../service/playlist.service';
import {Subscription} from 'rxjs';
import {Playlist, PlaylistSongs} from '../../../domain/library.model';
import {PlaylistEditComponent} from './playlist-edit.component';
import {FormsModule} from '@angular/forms';
import {LoadingState} from '../../../domain/common.model';
import {NotificationService} from '../../../service/notification.service';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, FormsModule, NoContentIndicatorComponent, ErrorIndicatorComponent, LoadingIndicatorComponent],
  selector: 'pony-playlist',
  templateUrl: './playlist.component.html',
  styleUrls: ['./playlist.component.scss']
})
export class PlaylistComponent implements OnInit, OnDestroy {

  readonly LoadingState = LoadingState;

  playlists!: Playlist[];
  selectedPlaylist: Playlist | undefined;
  selectedPlaylistSongs: PlaylistSongs | undefined;
  songsLoadingState = LoadingState.LOADING;
  deletionLoadingState = LoadingState.LOADED;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playlistService: PlaylistService,
    private readonly translateService: TranslateService,
    private readonly notificationService: NotificationService,
    private readonly modal: NgbModal,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(this.playlistService.observePlaylists().subscribe(playlists => {
      this.playlists = playlists;
      this.selectedPlaylist = this.playlists.length > 0 ? this.playlistService.getTopPlaylists()[0] : undefined;
      this.loadSongs();
    }));
  }

  private loadSongs() {
    this.songsLoadingState = LoadingState.LOADING;
    this.playlistService.getPlaylist(this.selectedPlaylist!.id).subscribe({
      next: playlistSongs => {
        this.songsLoadingState = LoadingState.LOADED;
        this.selectedPlaylistSongs = playlistSongs;
      },
      error: () => this.songsLoadingState = LoadingState.ERROR
    });
  }

  edit() {
    const modalRef = this.modal.open(PlaylistEditComponent);
    const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
    playlistEditComponent.playlist = this.selectedPlaylist;
  }

  delete() {
    if (window.confirm(this.translateService.instant('playlist.deletionConfirmation'))) {
      this.deletionLoadingState = LoadingState.LOADING;
      this.playlistService.deletePlaylist(this.selectedPlaylist!.id).subscribe({
        next: () => {
          this.deletionLoadingState = LoadingState.LOADED;
          this.notificationService.success(
            this.translateService.instant('playlist.notificationTitle'),
            this.translateService.instant('playlist.notificationTextDeletionSuccess'),
          );
        },
        error: () => {
          this.deletionLoadingState = LoadingState.LOADED;
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
}
