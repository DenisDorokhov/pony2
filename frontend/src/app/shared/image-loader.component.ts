import {AfterViewInit, Component, ElementRef, Input, NgZone, OnDestroy, ViewChild} from '@angular/core';
import * as Logger from 'js-logger';
import {fromEvent} from 'rxjs';
import {Scheduler} from 'rxjs-compat';
import 'rxjs/add/operator/debounce';
import 'rxjs/add/operator/filter';
import {Observable} from 'rxjs/Observable';
import {timer} from 'rxjs/observable/timer';
import {Subscription} from 'rxjs/Subscription';

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

  private isIntersecting = false;

  private visibilityChangeSubscription: Subscription | undefined;
  private intersectionSubscription: Subscription | undefined;

  @Input()
  get url(): string {
    return this._url;
  }

  set url(url: string) {
    this._url = url;
    if (url) {
      this.state = ImageLoaderComponentState.PENDING;
      if (this.isIntersecting) {
        this.startLoading();
      }
    } else {
      this.state = ImageLoaderComponentState.EMPTY;
    }
  }

  constructor(private ngZone: NgZone) {
  }

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      if (!document.hidden) {
        this.intersectionSubscription = this.subscribeToIntersection();
      } else {
        this.visibilityChangeSubscription = fromEvent(document, 'visibilitychange')
          .observeOn(Scheduler.animationFrame)
          .subscribe(() => {
            if (!this.intersectionSubscription && !document.hidden) {
              this.intersectionSubscription = this.subscribeToIntersection();
            }
          });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.visibilityChangeSubscription) {
      this.visibilityChangeSubscription.unsubscribe();
    }
    if (this.intersectionSubscription) {
      this.intersectionSubscription.unsubscribe();
    }
  }

  onLoaded() {
    this.state = ImageLoaderComponentState.LOADED;
  }

  onError() {
    Logger.error(`Could not load image '${this._url}'.`);
    this.state = ImageLoaderComponentState.ERROR;
  }

  private subscribeToIntersection(): Subscription {
    return this.createIntersectionObservable({
      root: this.findScroller(),
      rootMargin: '0px',
      threshold: 0
    })
      .debounce(() => timer(50))
      .filter(entry => {
        this.isIntersecting = entry.isIntersecting;
        return entry.isIntersecting;
      })
      .subscribe(() => {
        if (this.state === ImageLoaderComponentState.PENDING) {
          this.startLoading();
        }
      });
  }

  private findScroller(): HTMLElement {
    let currentParent: HTMLElement = this.imageElement.nativeElement.parentElement;
    while (currentParent) {
      if (currentParent.hasAttribute('data-image-loader-scroller')) {
        return currentParent;
      } else {
        currentParent = currentParent.parentElement;
      }
    }
    return undefined;
  }

  private createIntersectionObservable(intersectionOptions: any): Observable<IntersectionObserverEntry> {
    return Observable.create(subscriber => {
      const intersectionObserver = new IntersectionObserver(
        (entries) => subscriber.next(entries[0]),
        intersectionOptions
      );
      intersectionObserver.observe(this.imageElement.nativeElement);
      return () => {
        intersectionObserver.disconnect();
      };
    });
  }

  private startLoading() {
    this.ngZone.run(() => {
      this.imageElement.nativeElement.setAttribute('src', this.url);
      this.state = ImageLoaderComponentState.LOADING;
    });
  }
}
