import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {interval, Subscription} from 'rxjs';
import {Song} from './library.model';

export class TitleScroller {

  private _result: string;

  private _offset: number;
  private _normalizedOffset: number;

  constructor(public readonly target: string) {
    this.offset = 0;
  }

  get offset(): number {
    return this._offset;
  }

  set offset(offset: number) {
    this._offset = offset;
    this._normalizedOffset = this.normalizeOffset(offset);
    this.updateResult();
  }

  get normalizedOffset(): number {
    return this._normalizedOffset;
  }

  get result(): string {
    return this._result;
  }

  private normalizeOffset(offset: number): number {
    let normalizedOffset = offset;
    if (normalizedOffset > 0) { // 1.13 -> 0.13
      normalizedOffset = normalizedOffset - Math.floor(normalizedOffset);
    } else if (normalizedOffset < 0) { // -1.13 -> 0.87
      normalizedOffset = -offset;
      normalizedOffset = normalizedOffset - Math.floor(normalizedOffset);
      normalizedOffset = 1 - normalizedOffset;
    }
    return normalizedOffset;
  }

  private updateResult() {
    const characterOffset = Math.round(this.normalizedOffset * this.target.length);
    const beginning = this.target.substring(characterOffset);
    const ending = this.target.substring(0, characterOffset);
    this._result = beginning + ending;
  }
}

@Injectable()
export class PageTitleService {
  
  private static readonly ANIMATION_FRAME_DELAY = 1000;
  
  private _song: Song | undefined;
  
  private titleScroller: TitleScroller | undefined;
  private timerSubscription: Subscription | undefined;
  
  constructor(private translateService: TranslateService) {
    this.updateSong();
  }
  
  get song(): Song | undefined {
    return this._song;
  }
  
  set song(song: Song | undefined) {
    this._song = song;
    this.updateSong();
  }
  
  private updateSong() {
    this.titleScroller = undefined;
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
      
      this.titleScroller = new TitleScroller(this.translateService.instant(
        'songTitleBody',
        { artistName, songName }
      ));
      const prefix = this.translateService.instant('songTitlePrefix');
      window.document.title = prefix + this.titleScroller.result;
      
      // noinspection JSDeprecatedSymbols
      this.timerSubscription = interval(PageTitleService.ANIMATION_FRAME_DELAY)
        .do(() => {

          // Spaces are automatically trimmed and united by the browser, here we avoid animation pause of repeating spaces.
          let body = this.titleScroller.result;
          const numberOfStartingSpaces = this.getNumberOfStartingChars(body, ' ');
          let stepsCount = 1;
          if (prefix.endsWith(" ") && numberOfStartingSpaces > 0) {
            body = body.trim();
            stepsCount += numberOfStartingSpaces;
          }

          window.document.title = prefix + body;
          this.titleScroller.offset = this.titleScroller.normalizedOffset + stepsCount * (1.0 / this.titleScroller.target.length);
        })
        .subscribe();
      
    } else {
      window.document.title = this.translateService.instant('noSongTitle');
    }
  }

  private getNumberOfStartingChars(source: string, char: string): number {
    let result = 0;
    for (let i = 0; i < source.length; i++) {
      if (source.charAt(i) === char) {
        result++;
      } else {
        return result;
      }
    }
    return result;
  }
}