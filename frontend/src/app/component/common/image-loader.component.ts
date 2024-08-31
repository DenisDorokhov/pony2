import {AfterViewInit, Component, ElementRef, Input, NgZone, OnDestroy, ViewChild} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {filter} from 'rxjs/operators';
import {CommonModule} from "@angular/common";

enum ImageLoaderComponentState {
  EMPTY, PENDING, LOADING, ERROR, LOADED
}

@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'pony-image-loader',
  templateUrl: './image-loader.component.html',
  styleUrls: ['./image-loader.component.scss']
})
export class ImageLoaderComponent implements AfterViewInit, OnDestroy {

  private static LOADED_URLS = new Set<string>();

  readonly State = ImageLoaderComponentState;

  state: ImageLoaderComponentState = ImageLoaderComponentState.EMPTY;
  animate = true;

  @ViewChild('container') containerElement!: ElementRef;

  private _url: string | undefined;

  private scroller: HTMLElement | undefined;
  private isIntersecting = false;

  private intersectionSubscription: Subscription | undefined;

  @Input()
  get url(): string | undefined {
    return this._url;
  }

  set url(url: string | undefined) {
    if (this._url !== url) {
      this._url = url;
      if (url) {
        if (ImageLoaderComponent.LOADED_URLS.has(url)) {
          this.state = ImageLoaderComponentState.LOADED;
        } else {
          this.state = ImageLoaderComponentState.PENDING;
          if (this.isIntersecting) {
            this.startLoading();
          }
        }
      } else {
        this.state = ImageLoaderComponentState.EMPTY;
      }
    }
  }

  constructor(private ngZone: NgZone) {
  }

  ngAfterViewInit(): void {
    this.scroller = this.findScroller();
    this.subscribeToIntersections();
    // Fix for a bug of intersection not detected during application bootstrap.
    setTimeout(() => {
      if (this.state !== ImageLoaderComponentState.LOADING && this.state !== ImageLoaderComponentState.LOADED) {
        this.subscribeToIntersections();
      }
    }, 50);
  }

  private subscribeToIntersections() {
    this.ngZone.runOutsideAngular(() => {
      this.intersectionSubscription?.unsubscribe();
      this.intersectionSubscription = this.createIntersectionObservable({
        root: this.scroller,
        rootMargin: '0px',
        threshold: 0
      }).pipe(
        filter(entry => {
          this.isIntersecting = entry.isIntersecting;
          return entry.isIntersecting;
        })
      ).subscribe(() => {
        if (this.state === ImageLoaderComponentState.PENDING) {
          this.startLoading();
        }
      });
    });
  }

  private findScroller(): HTMLElement | undefined {
    let currentParent: HTMLElement | undefined = this.containerElement!.nativeElement.parentElement;
    while (currentParent) {
      if (currentParent.hasAttribute('data-pony-image-loader-scroller')) {
        return currentParent;
      } else {
        currentParent = currentParent.parentElement ?? undefined;
      }
    }
    return undefined;
  }

  private createIntersectionObservable(intersectionOptions: any): Observable<IntersectionObserverEntry> {
    return new Observable(subscriber => {
      const intersectionObserver = new IntersectionObserver(
        (entries) => subscriber.next(entries[0]),
        intersectionOptions
      );
      intersectionObserver.observe(this.containerElement!.nativeElement);
      return () => {
        intersectionObserver.disconnect();
      };
    });
  }

  private startLoading() {
    this.ngZone.run(() => {
      // console.log(`Loading ${new Date()} -> '${this.url}'.`);
      this.state = ImageLoaderComponentState.LOADING;
      this.intersectionSubscription?.unsubscribe();
      this.intersectionSubscription = undefined;
    });
  }

  ngOnDestroy(): void {
    this.intersectionSubscription?.unsubscribe();
  }

  onLoaded() {
    // console.log(`Loaded ${new Date()} -> '${this.url}'.`);
    this.state = ImageLoaderComponentState.LOADED;
    ImageLoaderComponent.LOADED_URLS.add(this._url!);
  }

  onError() {
    console.error(`Could not load image ${new Date()} -> '${this._url}'.`);
    this.state = ImageLoaderComponentState.ERROR;
  }

  onAnimationEnd() {
    this.animate = false;
  }
}
