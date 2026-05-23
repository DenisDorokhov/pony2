import {inject, Pipe, PipeTransform} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';

@Pipe({
  standalone: true,
  name: 'ponyUnknownArtist'
})
export class UnknownArtistPipe implements PipeTransform {

  private readonly translateService = inject(TranslateService);

  transform(value: any): any {
    return value ? value : this.translateService.instant('library.artist.unknownLabel');
  }
}
