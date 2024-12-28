import {Injectable} from '@angular/core';
import {TranslateLoader} from '@ngx-translate/core';
import {Observable, of} from 'rxjs';
import translationEn from '../translation/en';

@Injectable({
  providedIn: 'root'
})
export class TranslateLoaderService implements TranslateLoader {

  private translations = new Map<string, any>();

  constructor() {
    this.translations.set('en', translationEn);
  }

  public getTranslation(lang: string): Observable<any> {
    return of(this.translations.get(lang));
  }
}
