import {inject, Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import countries from 'flag-icons/country.json';
import {COUNTRY_DETAILS, CountryCode} from '../domain/country.model';

const COUNTRY_CODE_REMAP: Readonly<Record<string, string>> = {
  su: 'ru'
};

@Injectable({
  providedIn: 'root'
})
export class CountryService {

  private readonly translateService = inject(TranslateService);

  getCountries(): {code: CountryCode; countryName: string}[] {
    return Object.values(CountryCode)
      .map(code => ({code, countryName: this.getName(code) ?? COUNTRY_DETAILS[code].countryName}))
      .sort((first, second) => first.countryName.localeCompare(second.countryName));
  }

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
    const flagCode = code ? COUNTRY_CODE_REMAP[code] ?? code : undefined;
    return countries.find(country => country.code === flagCode)?.code;
  }
}
