import {AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnDestroy, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

enum ImageLoaderState {
  PENDING, LOADING, LOADED, ERROR
}

class ImageLoader {

  private _state: ImageLoaderState = ImageLoaderState.PENDING;
  get state(): ImageLoaderState {
    return this._state;
  }

  readonly loading = new EventEmitter();
  readonly loaded = new EventEmitter();
  readonly error = new EventEmitter();

  private scroller: HTMLElement;
  private removeScrollListener: () => void;
  private timer: number;

  constructor(private imageElement: any, private url: string) {
    window.addEventListener('resize', () => this.onResize());
    this.imageElement.addEventListener('load', () => this.onLoad());
    this.imageElement.addEventListener('error', () => this.onError());
    this.scroller = this.findScroller();
    if (this.scroller) {
      this.scroller.addEventListener('scroll', () => this.lazyLoad());
    } else {
      console.log('Could not find scroller for image loader!');
    }
    this.lazyLoad();
  }

  destroy() {
    window.removeEventListener('resize', this.onResize);
    this.imageElement.removeEventListener('load', this.onLoad);
    this.imageElement.removeEventListener('error', this.onError);
    if (this.removeScrollListener) {
      this.removeScrollListener();
    }
    this.cancelTimer();
  }

  onResize() {
    this.lazyLoad();
  }

  onLoad() {
    if (this.state === ImageLoaderState.LOADING) {
      console.log(`Loaded image ${this.url}.`);
      this._state = ImageLoaderState.LOADED;
      this.loaded.emit();
    }
  }

  onError() {
    if (this.state === ImageLoaderState.LOADING) {
      console.log(`Could not load image ${this.url}.`);
      this._state = ImageLoaderState.ERROR;
      this.error.emit();
    }
  }

  private lazyLoad() {
    if (this.state === ImageLoaderState.PENDING && !this.timer) {
      this.timer = window.setTimeout(() => {
        this.timer = undefined;
        this.doLazyLoad();
      }, 50);
    }
  }

  private doLazyLoad() {
    let load = true;
    if (this.scroller) {
      const elementRect = this.imageElement.parentElement.getBoundingClientRect();
      const scrollerRect = this.scroller.getBoundingClientRect();
      load = (elementRect.top < scrollerRect.top + this.scroller.offsetHeight) &&
        (elementRect.top + this.imageElement.parentElement.offsetHeight > scrollerRect.top);
    }
    if (load) {
      console.log(`Loading image ${this.url}...`);
      this.imageElement.src = this.url;
      this._state = ImageLoaderState.LOADING;
      this.loading.emit();
    }
  }

  private findScroller() {
    let currentParent: HTMLElement = this.imageElement.parentElement;
    while (currentParent) {
      if (currentParent.hasAttribute('scrolling')) {
        return currentParent;
      } else {
        currentParent = currentParent.parentElement;
      }
    }
    return undefined;
  }

  private cancelTimer() {
    if (this.timer) {
      window.clearTimeout(this.timer);
      this.timer = undefined;
    }
  }
}

enum ImageLoaderComponentState {
  EMPTY, PENDING, LOADING, ERROR, LOADED
}

@Component({
  selector: 'pony-image-loader',
  templateUrl: './image-loader.component.html',
  styleUrls: ['./image-loader.component.scss']
})
export class ImageLoaderComponent implements AfterViewInit, OnDestroy {

  readonly State = ImageLoaderComponentState;

  state: ImageLoaderComponentState = ImageLoaderComponentState.EMPTY;

  @ViewChild('image') imageElement: ElementRef;

  private _url: string;

  @Input()
  get url(): string {
    return this._url;
  }
  set url(url: string) {
    this._url = url;
    this.state = url ? ImageLoaderComponentState.PENDING : ImageLoaderComponentState.EMPTY;
  }

  private imageLoader: ImageLoader;

  private loadingSubscription: Subscription;
  private loadedSubscription: Subscription;
  private errorSubscription: Subscription;

  constructor(private ngZone: NgZone) {
  }

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.imageLoader = new ImageLoader(this.imageElement.nativeElement, this.url);
    });
    this.loadingSubscription = this.imageLoader.loading.subscribe(() => this.onLoading());
    this.loadedSubscription = this.imageLoader.loaded.subscribe(() => this.onLoaded());
    this.errorSubscription = this.imageLoader.error.subscribe(() => this.onError());
  }

  ngOnDestroy(): void {
    this.loadingSubscription.unsubscribe();
    this.loadedSubscription.unsubscribe();
    this.errorSubscription.unsubscribe();
    if (this.imageLoader) {
      this.imageLoader.destroy();
    }
  }

  onLoading() {
    this.ngZone.run(() => {
      this.state = ImageLoaderComponentState.LOADING;
    });
  }

  onLoaded() {
    this.ngZone.run(() => {
      this.state = ImageLoaderComponentState.LOADED;
    });
  }

  onError() {
    this.ngZone.run(() => {
      this.state = ImageLoaderComponentState.ERROR;
    });
  }
}
