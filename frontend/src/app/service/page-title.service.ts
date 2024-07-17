import {Injectable, NgZone} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {Subscription, timer} from 'rxjs';
import {takeWhile, tap} from 'rxjs/operators';
import {Song} from "../domain/library.model";

class StringScroller {

  private _offset = 0;
  private _result: string;

  constructor(public readonly target: string) {
    this._result = target;
  }

  get offset(): number {
    return this._offset;
  }

  get result(): string {
    return this._result;
  }

  willRestart(): boolean {
    return this._offset === this.target.length;
  }

  scroll(): string {
    do {
      this.doScroll();
    } while (this._result.indexOf(' ') === 0);
    return this._result;
  }

  private doScroll() {
    if (this.target.length === 0) {
      return;
    }
    if (this._offset === this.target.length) {
      this._offset = 0;
    }
    this._offset++;
    const prefix = this.target.substring(0, this._offset);
    const suffix = this.target.substring(this._offset);
    this._result = suffix + prefix;
  }
}

@Injectable({
  providedIn: 'root'
})
export class PageTitleService {

  private static readonly ANIMATION_INITIAL_DELAY = 3000;
  private static readonly ANIMATION_FRAME_DELAY = 1000;

  private _song: Song | undefined;

  private titleShifter: StringScroller | undefined;
  private timerSubscription: Subscription | undefined;

  constructor(
    private translateService: TranslateService,
    private ngZone: NgZone
  ) {
    this.startScrolling();
  }

  get song(): Song | undefined {
    return this._song;
  }

  set song(song: Song | undefined) {
    this._song = song;
    this.startScrolling();
  }

  private startScrolling() {
    this.titleShifter = undefined;
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
      this.timerSubscription = undefined;
    }
    if (this._song) {

      let artistName = this._song ? this._song.artistName : undefined;
      let songName = this._song ? this._song.name : undefined;
      if (!artistName) {
        artistName = this.translateService.instant('library.artist.unknownLabel');
      }
      if (!songName) {
        songName = this.translateService.instant('library.song.unknownLabel');
      }

      this.titleShifter = new StringScroller(this.translateService.instant(
        'songTitleBody',
        {artistName, songName}
      ));
      const prefix = this.translateService.instant('songTitlePrefix');
      window.document.title = prefix + this.titleShifter.target;

      this.ngZone.runOutsideAngular(() => {
        this.timerSubscription = timer(PageTitleService.ANIMATION_INITIAL_DELAY, PageTitleService.ANIMATION_FRAME_DELAY)
          .pipe(
            tap(() => window.document.title = prefix + this.titleShifter!.scroll()),
            takeWhile(() => !this.titleShifter!.willRestart())
          )
          .subscribe({
            complete: () => this.startScrolling()
          });
      });

    } else {
      window.document.title = this.translateService.instant('noSongTitle');
    }
  }
}
