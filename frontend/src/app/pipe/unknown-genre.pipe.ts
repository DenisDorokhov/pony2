import {Pipe, PipeTransform} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";

@Pipe({
  standalone: true,
  name: 'ponyUnknownGenre'
})
export class UnknownGenrePipe implements PipeTransform {

  constructor(
    private readonly translateService: TranslateService,
  ) {
  }

  transform(value: any, ...args: any[]): any {
    return value ? value : this.translateService.instant('library.genre.unknownLabel');
  }
}
