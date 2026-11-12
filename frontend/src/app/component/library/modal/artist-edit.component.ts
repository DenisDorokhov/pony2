import {Component, DestroyRef, inject, Input, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {Artist} from '../../../domain/library.model';
import {ArtistDiscoveryDto} from '../../../domain/library.dto';
import {CountryCode} from '../../../domain/country.model';
import {ErrorDto} from '../../../domain/common.dto';
import {LoadingState} from '../../../domain/common.model';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LibraryService} from '../../../service/library.service';
import {CountryService} from '../../../service/country.service';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {ImageLoaderComponent} from '../../common/image-loader.component';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';

@Component({
  imports: [ReactiveFormsModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, ErrorContainerComponent, ImageLoaderComponent, UnknownArtistPipe],
  selector: 'pony-artist-edit',
  templateUrl: './artist-edit.component.html',
  styleUrls: ['./artist-edit.component.scss']
})
export class ArtistEditComponent implements OnInit {

  readonly activeModal = inject(NgbActiveModal);

  private readonly discoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
  private readonly countryService = inject(CountryService);
  private readonly destroyRef = inject(DestroyRef);

  @Input() artist!: Artist;

  readonly LoadingState = LoadingState;
  readonly countries = this.countryService.getCountries();
  readonly form = new FormGroup({
    overrideCountryCode: new FormControl<CountryCode | null>(null)
  });

  discovery: ArtistDiscoveryDto | undefined;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADING;

  ngOnInit(): void {
    this.discoveryService.getArtistDiscovery(this.artist.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: discovery => {
          this.discovery = discovery;
          this.form.patchValue({overrideCountryCode: discovery.overrideCountryCode ?? null});
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
    this.discoveryService.updateArtistDiscovery({
      artistId: this.artist.id,
      overrideCountryCode: this.form.getRawValue().overrideCountryCode ?? undefined
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
