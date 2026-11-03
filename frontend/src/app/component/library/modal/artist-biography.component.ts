import {Component, ElementRef, HostListener, inject, Input, NgZone, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {ArtistBiography} from '../../../domain/library.model';
import {ArtistBiographyDto} from '../../../domain/library.dto';
import {LoadingState} from '../../../domain/common.model';
import {LibraryService} from '../../../service/library.service';
import {ThemeService} from '../../../service/theme.service';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {
  ArtistBiographyImageCrop,
  artistBiographyImageColor,
  ARTIST_BIOGRAPHY_LINKS,
  cropArtistBiographyImage
} from '../../../utils/artist-biography.utils';

@Component({
  imports: [TranslateModule, UnknownArtistPipe, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent],
  selector: 'pony-artist-biography',
  templateUrl: './artist-biography.component.html',
  styleUrls: ['./artist-biography.component.scss']
})
export class ArtistBiographyComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryService = inject(LibraryService);
  private readonly themeService = inject(ThemeService);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ngZone = inject(NgZone);

  @Input() artistId!: string;

  @ViewChild('hero') private heroElement?: ElementRef<HTMLElement>;
  @ViewChild('image') private imageElement?: ElementRef<HTMLImageElement>;

  readonly LoadingState = LoadingState;
  loadingState = LoadingState.LOADING;
  biography: ArtistBiography | undefined;
  links: (ArtistBiographyDto.Link & {label: string; icon: string})[] = [];
  crop: ArtistBiographyImageCrop | undefined;
  imageLoaded = false;
  imageFailed = false;
  imageGradient = '';

  private subscriptions: Subscription[] = [];
  private resizeObserver: ResizeObserver | undefined;
  private imageColor: number[] | undefined;

  ngOnInit(): void {
    this.subscriptions.push(this.themeService.observeTheme().subscribe(() => this.updateBackground()));
    this.subscriptions.push(this.libraryService.getArtistBiography(this.artistId).subscribe({
      next: biography => {
        this.biography = biography;
        this.links = biography.links.map(link => ({...link, ...ARTIST_BIOGRAPHY_LINKS[link.type]}));
        this.loadingState = LoadingState.LOADED;
      },
      error: () => this.loadingState = LoadingState.ERROR,
    }));
  }

  private updateBackground(): void {
    const base = getComputedStyle(this.element.nativeElement).getPropertyValue('--bs-body-bg-rgb')
      .split(',').map(channel => Number(channel.trim()));
    if (base.length !== 3 || !base.every(Number.isFinite)) {
      return;
    }
    const amount = this.imageColor ? .20 : 0;
    const mixed = base.map((channel, index) => Math.round(channel * (1 - amount) + (this.imageColor?.[index] ?? channel) * amount));
    this.element.nativeElement.style.setProperty('--biography-background', `rgb(${mixed.join(',')})`);
    this.element.nativeElement.style.setProperty('--biography-background-rgb', mixed.join(','));
  }

  onImageLoaded(image: HTMLImageElement): void {
    this.imageColor = artistBiographyImageColor(image);
    this.updateBackground();
    this.updateCrop();
    this.imageLoaded = true;
    this.resizeObserver = new ResizeObserver(() => this.ngZone.run(() => this.updateCrop()));
    if (this.heroElement) {
      this.resizeObserver.observe(this.heroElement.nativeElement);
    }
  }

  @HostListener('window:resize')
  protected updateCrop(): void {
    const image = this.imageElement?.nativeElement;
    // Keep the crop height independent of the scrollbar to avoid resize feedback.
    const width = this.element.nativeElement.clientWidth;
    const displayWidth = this.heroElement?.nativeElement.clientWidth;
    if (!image || !width || !displayWidth) {
      return;
    }
    const maxHeight = Math.min(300, window.innerHeight * .38);
    const targetHeight = Math.min(maxHeight, Math.max(180, width * .48));
    this.crop = cropArtistBiographyImage(image.naturalWidth, image.naturalHeight, width, targetHeight, maxHeight,
      this.biography?.faces);
    if (this.crop) {
      const scale = Math.min(1, displayWidth / this.crop.width);
      this.crop = {...this.crop, width: this.crop.width * scale, top: this.crop.top * scale};
      const start = this.crop.fadeStart;
      const rgb = 'var(--biography-background-rgb)';
      const stops = Array.from({length: 16}, (_, index) => {
        const progress = index / 15;
        const opacity = (1 - Math.cos(Math.PI * progress)) / 2;
        return `rgba(${rgb},${opacity}) ${start + (100 - start) * progress}%`;
      });
      this.imageGradient = `linear-gradient(to bottom, rgba(${rgb},0) 0%, ${stops.join(', ')})`;
    }
  }

  onImageError(): void {
    this.imageFailed = true;
    this.resizeObserver?.disconnect();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
    this.resizeObserver?.disconnect();
  }
}
