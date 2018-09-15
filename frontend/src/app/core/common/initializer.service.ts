import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import * as Logger from 'js-logger';
import {EMPTY} from 'rxjs';
import 'rxjs/add/observable/empty';
import 'rxjs/add/observable/onErrorResumeNext';
import 'rxjs/add/operator/mergeMap';
import {InstallationService} from '../installation/installation.service';
import {AuthenticationService} from '../user/authentication.service';

@Injectable()
export class InitializerService {

  constructor(
    private translateService: TranslateService,
    private installationService: InstallationService,
    private authenticationService: AuthenticationService,
  ) {
  }

  initialize(): Promise<any> {

    Logger.useDefaults({
      logLevel: Logger.WARN,
      formatter: (messages, context) => {
        messages.unshift(':');
        messages.unshift('[' + (context.name || 'default') + ']');
        messages.unshift(context.level.name);
        messages.unshift(new Date().toISOString());
      }
    });
    Logger.info('Application started.');

    this.translateService.setDefaultLang('en');
    this.translateService.use('en');

    window.document.title = this.translateService.instant('noSongTitle');

    return this.installationService.getInstallationStatus()
      .flatMap(installationStatus => {
        if (installationStatus.installed) {
          return this.authenticationService.authenticate()
            .catch(() => EMPTY);
        } else {
          return EMPTY;
        }
      })
      .toPromise();
  }
}
