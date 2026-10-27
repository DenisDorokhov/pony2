import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import countries from 'flag-icons/country.json';

@Injectable({
  providedIn: 'root'
})
export class CountryService {

  private readonly translateService = inject(TranslateService);

  getName(countryCode: string | undefined): string | undefined {
    const code = countryCode?.toLowerCase();
    if (!code) {
      return undefined;
    }
    const key = 'country.' + code;
    const translation = this.translateService.instant(key);
    return translation !== key ? translation : countries.find(country => country.code === code)?.name;
  }

  getFlagCode(countryCode: string | undefined): string | undefined {
    const code = countryCode?.toLowerCase();
    return countries.find(country => country.code === code)?.code;
  }
}
