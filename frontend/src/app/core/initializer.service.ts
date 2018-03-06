import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';

@Injectable()
export class InitializerService {

  constructor(private translateService: TranslateService) {
  }

  initialize(): Promise<any> {
    this.translateService.setDefaultLang('en');
    this.translateService.use('en');
    return Promise.resolve();
  }
}
