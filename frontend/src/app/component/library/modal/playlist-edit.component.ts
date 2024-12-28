import {Component, Input, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {LoadingState} from '../../../domain/common.model';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {Playlist, PlaylistSongs, Song} from '../../../domain/library.model';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ErrorDto} from '../../../domain/common.dto';
import {PlaylistService} from '../../../service/playlist.service';
import {Observable} from 'rxjs';
import {PlaylistCreateCommandDto, PlaylistUpdateCommandDto} from '../../../domain/library.dto';
import {ErrorContainerComponent} from '../../common/error-container.component';
import SongId = PlaylistUpdateCommandDto.SongId;

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, FormsModule, ReactiveFormsModule, ErrorIndicatorComponent, LoadingIndicatorComponent, ErrorContainerComponent],
  selector: 'pony-playlist-edit',
  templateUrl: './playlist-edit.component.html',
  styleUrls: ['./playlist-edit.component.scss']
})
export class PlaylistEditComponent implements OnInit {

  readonly LoadingState = LoadingState;

  @Input()
  playlist: Playlist | undefined;
  @Input()
  songs: Song[] | undefined;

  form!: FormGroup;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADED;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private playlistService: PlaylistService,
  ) {
  }

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      id: this.playlist?.id ?? undefined,
      name: this.playlist?.name ?? '',
    });
  }

  save() {
    const formValue = this.form.value;
    let observable: Observable<PlaylistSongs>;
    if (this.playlist) {
      const command: PlaylistUpdateCommandDto = {
        id: formValue.id,
        name: formValue.name,
        overriddenSongIds: this.songs?.map(song => {
          return {
            id: undefined,
            songId: song.id
          } as SongId;
        }),
      };
      observable = this.playlistService.updatePlaylist(command);
    } else {
      const command: PlaylistCreateCommandDto = {
        name: formValue.name,
        songIds: this.songs?.map(song => song.id) ?? [],
      };
      observable = this.playlistService.createPlaylist(command);
    }
    this.loadingState = LoadingState.LOADING;
    observable.subscribe({
      next: playlist => {
        this.loadingState = LoadingState.LOADED;
        this.activeModal.close(playlist);
      },
      error: error => {
        this.error = error;
        this.loadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
      }
    });
  }
}
