import {Injectable} from '@angular/core';
import {CanLoad, Router} from '@angular/router';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';
import {Observable} from 'rxjs/Observable';
import {InstallationService} from './installation.service';

@Injectable()
export class InstalledGuard implements CanLoad {

  constructor(
    private installationService: InstallationService,
    private router: Router,
  ) {
  }

  canLoad(): Observable<boolean> {
    return this.installationService.getInstallationStatus()
      .do(installationStatus => {
        if (!installationStatus.installed) {
          this.router.navigate(['/install'], {skipLocationChange: true});
        }
      })
      .map(installationStatus => installationStatus.installed);
  }
}
