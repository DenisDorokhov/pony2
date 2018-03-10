import {Injectable} from '@angular/core';
import {TranslateLoader} from '@ngx-translate/core';
import {Observable} from 'rxjs/Observable';
import translationEn from '../../translation/en';

@Injectable()
export class TranslateLoaderService implements TranslateLoader {

  private translations: Map<string, any> = new Map();

  constructor() {
    this.translations.set('en', translationEn);
  }

  public getTranslation(lang: string): Observable<any> {
    return Observable.of(this.translations.get(lang));
  }
}
