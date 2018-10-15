import {Injectable} from '@angular/core';
import {CanLoad, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {InstallationService} from './installation.service';

@Injectable()
export class NotInstalledGuard implements CanLoad {

  constructor(
    private installationService: InstallationService,
    private router: Router,
  ) {
  }

  canLoad(): Observable<boolean> {
    return this.installationService.getInstallationStatus()
      .pipe(
        tap(installationStatus => {
          if (installationStatus.installed) {
            this.router.navigate(['/library'], {replaceUrl: true});
          }
        }),
        map(installationStatus => !installationStatus.installed)
      );
  }
}
