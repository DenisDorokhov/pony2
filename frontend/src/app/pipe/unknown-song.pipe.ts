import {Pipe, PipeTransform} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';

@Pipe({
  standalone: true,
  name: 'ponyUnknownSong'
})
export class UnknownSongPipe implements PipeTransform {

  constructor(
    private readonly translateService: TranslateService,
  ) {
  }

  transform(value: any): any {
    return value ? value : this.translateService.instant('library.song.unknownLabel');
  }
}
