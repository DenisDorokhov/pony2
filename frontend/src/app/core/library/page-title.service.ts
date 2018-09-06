import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {interval, Subscription} from 'rxjs';
import {Song} from './library.model';

class StringShifter {

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

  shift(): string {
    do {
      this.doShift();
    } while (this._result.indexOf(' ') === 0);
    return this._result;
  }
  
  private doShift() {
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

@Injectable()
export class PageTitleService {
  
  private static readonly ANIMATION_FRAME_DELAY = 1000;
  
  private _song: Song | undefined;
  
  private titleShifter: StringShifter | undefined;
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
      
      this.titleShifter = new StringShifter(this.translateService.instant(
        'songTitleBody',
        { artistName, songName }
      ));
      const prefix = this.translateService.instant('songTitlePrefix');
      window.document.title = prefix + this.titleShifter.target;
      
      this.timerSubscription = interval(PageTitleService.ANIMATION_FRAME_DELAY)
        .do(() => {
          window.document.title = prefix + this.titleShifter.shift();
        })
        .subscribe();
      
    } else {
      window.document.title = this.translateService.instant('noSongTitle');
    }
  }
}
