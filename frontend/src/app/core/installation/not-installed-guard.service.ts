import {CanLoad, Router} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import {InstallationService} from './installation.service';

@Injectable()
export class NotInstalledGuardService implements CanLoad {

  constructor(private installationService: InstallationService,
              private router: Router) {
  }

  canLoad(): Observable<boolean> {
    return this.installationService.getInstallationStatus()
      .do(installationStatus => {
        if (installationStatus.installed) {
          this.router.navigate(['/'], {skipLocationChange: true});
        }
      })
      .map(installationStatus => !installationStatus.installed);
  }
}
