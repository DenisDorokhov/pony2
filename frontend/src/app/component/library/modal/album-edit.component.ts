import {Component, DestroyRef, inject, Input, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {DatePipe} from '@angular/common';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {Album} from '../../../domain/library.model';
import {AlbumDiscoveryDataDto, AlbumDiscoveryDto} from '../../../domain/library.dto';
import {ErrorDto} from '../../../domain/common.dto';
import {LoadingState} from '../../../domain/common.model';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LibraryService} from '../../../service/library.service';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {UnknownAlbumPipe} from '../../../pipe/unknown-album.pipe';

@Component({
  imports: [ReactiveFormsModule, DatePipe, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, ErrorContainerComponent, UnknownAlbumPipe],
  selector: 'pony-album-edit',
  templateUrl: './album-edit.component.html',
  styleUrls: ['./album-edit.component.scss']
})
export class AlbumEditComponent implements OnInit {

  readonly activeModal = inject(NgbActiveModal);

  private readonly discoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
  private readonly destroyRef = inject(DestroyRef);

  @Input() album!: Album;

  readonly LoadingState = LoadingState;
  readonly releaseTypes = Object.values(AlbumDiscoveryDataDto.ReleaseType);
  readonly form = new FormGroup({
    overrideReleaseType: new FormControl<AlbumDiscoveryDataDto.ReleaseType | null>(null)
  });

  discovery: AlbumDiscoveryDto | undefined;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADING;

  ngOnInit(): void {
    this.discoveryService.getAlbumDiscovery(this.album.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: discovery => {
          this.discovery = discovery;
          this.form.patchValue({overrideReleaseType: discovery.overrideReleaseType ?? null});
          this.loadingState = LoadingState.LOADED;
        },
        error: error => {
          this.error = error;
          this.loadingState = LoadingState.ERROR;
        }
      });
  }

  save() {
    if (!this.discovery || this.loadingState === LoadingState.LOADING) {
      return;
    }
    this.loadingState = LoadingState.LOADING;
    this.error = undefined;
    this.discoveryService.updateAlbumDiscovery({
      albumId: this.album.id,
      overrideReleaseType: this.form.getRawValue().overrideReleaseType ?? undefined
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadingState = LoadingState.LOADED;
          this.libraryService.requestRefresh();
          this.activeModal.close();
        },
        error: error => {
          this.error = error;
          this.loadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
        }
      });
  }
}
