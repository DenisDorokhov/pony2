import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import countries from 'flag-icons/country.json';

@Injectable({
  providedIn: 'root'
})
export class CountryService {

  private readonly translateService = inject(TranslateService);

  getName(countryCode: string | undefined): string | undefined {
    const normalizedCode = countryCode?.trim().toLowerCase();
    if (!normalizedCode) {
      return undefined;
    }
    const key = 'country.' + normalizedCode;
    const translation = this.translateService.instant(key);
    return translation !== key ? translation : countries.find(country => country.code === normalizedCode)?.name;
  }
}
