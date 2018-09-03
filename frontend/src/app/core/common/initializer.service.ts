import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {EMPTY} from 'rxjs';
import {InstallationService} from '../installation/installation.service';
import {AuthenticationService} from '../user/authentication.service';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/observable/onErrorResumeNext';
import 'rxjs/add/observable/empty';

@Injectable()
export class InitializerService {

  constructor(
    private translateService: TranslateService,
    private installationService: InstallationService,
    private authenticationService: AuthenticationService,
  ) {
  }

  initialize(): Promise<any> {
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
