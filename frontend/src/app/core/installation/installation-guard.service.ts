import {CanLoad, Router} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import {InstallationService} from './installation.service';

@Injectable()
export class InstallationGuard implements CanLoad {

  constructor(private installationService: InstallationService,
              private router: Router) {
  }

  canLoad(): Observable<boolean> {
    return this.installationService.getInstallationStatus()
      .map(installationStatus => installationStatus.installed)
      .do(installed => {
        if (!installed) {
          this.router.navigate(['/install']);
        }
      });
  }
}
