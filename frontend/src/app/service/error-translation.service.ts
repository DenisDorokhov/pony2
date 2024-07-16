import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {ErrorDto} from "../domain/common.dto";

@Injectable({
  providedIn: 'root'
})
export class ErrorTranslationService {

  constructor(private translateService: TranslateService) {
  }

  translateError(error: ErrorDto): any {
    return this.translate('error.' + error.code, error.arguments, error.message);
  }

  translateFieldViolation(fieldViolation: ErrorDto.FieldViolation): any {
    return this.translate('fieldViolation.' + fieldViolation.code, fieldViolation.arguments, fieldViolation.message);
  }

  private translate(key: string, params: string[], fallback: string): string | any {
    const translation = this.translateService.instant(key, params);
    return translation !== key ? translation : fallback;
  }
}
