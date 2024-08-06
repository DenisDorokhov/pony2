import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {EMPTY} from 'rxjs';
import {catchError, mergeMap, tap} from 'rxjs/operators';
import {InstallationService} from "./installation.service";
import {AuthenticationService} from "./authentication.service";

@Injectable({
  providedIn: 'root'
})
export class InitializerService {

  constructor(
    private translateService: TranslateService,
    private installationService: InstallationService,
    private authenticationService: AuthenticationService,
  ) {
  }

  initialize(): Promise<any> {

    console.info('Application started.');

    this.translateService.setDefaultLang('en');
    this.translateService.use('en');

    window.document.title = this.translateService.instant('noSongTitle');

    return this.installationService.getInstallationStatus()
      .pipe(
        mergeMap(installationStatus => {
          if (installationStatus.installed) {
            return this.authenticationService.authenticate()
              .pipe(catchError(() => EMPTY));
          } else {
            return EMPTY;
          }
        }),
        tap({
          next: () => {
            (window as any).ponyBootstrapSuccess = true;
          },
          error: () => {
            (window as any).ponyBootstrapError = true;
          }
        })
      )
      .toPromise();
  }
}
