import {Component, ElementRef, HostListener, inject, Input, NgZone, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {Artist, ArtistBiography} from '../../../domain/library.model';
import {LoadingState} from '../../../domain/common.model';
import {CountryService} from '../../../service/country.service';
import {LibraryService} from '../../../service/library.service';
import {ThemeService} from '../../../service/theme.service';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';
import {UnknownGenrePipe} from '../../../pipe/unknown-genre.pipe';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {ImageLoaderComponent} from '../../common/image-loader.component';
import {
  ArtistBiographyImageCrop,
  ArtistBiographyTextPart,
  artistBiographyImageColor,
  ARTIST_BIOGRAPHY_LINKS,
  cropArtistBiographyImage,
  formatArtistBiographyText
} from '../../../utils/artist-biography.utils';

@Component({
  imports: [TranslateModule, UnknownArtistPipe, UnknownGenrePipe, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, ImageLoaderComponent],
  selector: 'pony-artist-biography',
  templateUrl: './artist-biography.component.html',
  styleUrls: ['./artist-biography.component.scss']
})
export class ArtistBiographyComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryService = inject(LibraryService);
  private readonly countryService = inject(CountryService);
  private readonly themeService = inject(ThemeService);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly ngZone = inject(NgZone);

  @Input() artistId!: string;

  @ViewChild('hero') private heroElement?: ElementRef<HTMLElement>;
  @ViewChild('image') private imageElement?: ElementRef<HTMLImageElement>;
  @ViewChild('body') private bodyElement?: ElementRef<HTMLElement>;
  @ViewChild('similarArtists') private similarArtistsElement?: ElementRef<HTMLElement>;

  @ViewChild('biographyContent')
  private set biographyContent(element: ElementRef<HTMLElement> | undefined) {
    this.textResizeObserver?.disconnect();
    this.biographyContentElement = element;
    if (element) {
      this.textResizeObserver = new ResizeObserver(() => this.ngZone.run(() => {
        const lineHeight = parseFloat(getComputedStyle(element.nativeElement).lineHeight);
        this.biographyCanExpand = !!this.biography?.similarArtists.length &&
          element.nativeElement.scrollHeight > lineHeight * 9 + 1;
      }));
      this.textResizeObserver.observe(element.nativeElement);
    }
  }

  readonly LoadingState = LoadingState;
  loadingState = LoadingState.LOADING;
  biography: ArtistBiography | undefined;
  countryName: string | undefined;
  countryFlagCode: string | undefined;
  paragraphs: ArtistBiographyTextPart[][] = [];
  links: {url: string; label: string; icon: string}[] = [];
  crop: ArtistBiographyImageCrop | undefined;
  imageLoaded = false;
  imageFailed = false;
  imageGradient = '';
  biographyCanExpand = false;
  biographyExpanded = false;
  similarArtistsExpanded = false;
  readonly similarArtistPreviewCount = 4;

  private subscriptions: Subscription[] = [];
  private resizeObserver: ResizeObserver | undefined;
  private textResizeObserver: ResizeObserver | undefined;
  private biographyContentElement: ElementRef<HTMLElement> | undefined;
  private imageColor: number[] | undefined;

  get visibleSimilarArtists(): Artist[] {
    const artists = this.biography?.similarArtists ?? [];
    return this.similarArtistsExpanded ? artists : artists.slice(0, this.similarArtistPreviewCount);
  }

  toggleBiography(): void {
    this.biographyExpanded = !this.biographyExpanded;
    if (!this.biographyExpanded) {
      this.scrollBackTo(this.biographyContentElement?.nativeElement);
    }
  }

  private scrollBackTo(element: HTMLElement | undefined): void {
    const body = this.bodyElement?.nativeElement;
    if (element && body) {
      const top = element.getBoundingClientRect().top - body.getBoundingClientRect().top + body.scrollTop - 16;
      body.scrollTop = Math.min(body.scrollTop, Math.max(0, top));
    }
  }

  toggleSimilarArtists(): void {
    this.similarArtistsExpanded = !this.similarArtistsExpanded;
    if (!this.similarArtistsExpanded) {
      this.scrollBackTo(this.similarArtistsElement?.nativeElement);
    }
  }

  goToArtist(artist: Artist, event: MouseEvent): void {
    if (event.altKey) {
      event.preventDefault();
      return;
    }
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.requestScrollToArtist(artist);
    this.activeModal.dismiss();
  }

  ngOnInit(): void {
    this.subscriptions.push(this.themeService.observeTheme().subscribe(() => this.updateBackground()));
    this.subscriptions.push(this.libraryService.getArtistBiography(this.artistId).subscribe({
      next: biography => {
        this.biography = biography;
        this.countryFlagCode = this.countryService.getFlagCode(biography.artist.discoveryData?.effectiveCountryCode);
        this.countryName = this.countryService.getName(this.countryFlagCode);
        this.paragraphs = formatArtistBiographyText(biography.text);
        this.links = biography.links.map(link => ({...link, ...ARTIST_BIOGRAPHY_LINKS[link.type]}));
        const spotifyArtistId = biography.artist.discoveryData?.spotifyArtistId;
        if (spotifyArtistId) {
          this.links.push({url: `https://open.spotify.com/artist/${encodeURIComponent(spotifyArtistId)}`,
            label: 'Spotify', icon: 'bi-spotify'});
        }
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
    const width = this.element.nativeElement.getBoundingClientRect().width;
    const displayWidth = this.heroElement?.nativeElement.getBoundingClientRect().width;
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
      const imageBottom = image.naturalHeight * this.crop.width / image.naturalWidth - this.crop.top;
      const height = Math.min(this.crop.height, imageBottom);
      const fadeHeight = this.crop.height * (1 - this.crop.fadeStart / 100);
      this.crop = {...this.crop, height, fadeStart: Math.max(0, (height - fadeHeight) / height * 100)};
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
    this.textResizeObserver?.disconnect();
  }
}
