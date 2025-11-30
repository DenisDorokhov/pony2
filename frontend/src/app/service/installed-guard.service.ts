import {Injectable} from '@angular/core';
import {CanMatch, GuardResult, MaybeAsync, Router} from '@angular/router';
import {map, tap} from 'rxjs/operators';
import {InstallationService} from './installation.service';

@Injectable({
  providedIn: 'root'
})
export class InstalledGuard implements CanMatch {

  constructor(
    private installationService: InstallationService,
    private router: Router,
  ) {
  }

  canMatch(): MaybeAsync<GuardResult> {
    return this.installationService.requestInstallationStatus()
      .pipe(
        tap(installationStatus => {
          if (!installationStatus.installed) {
            this.router.navigate(['/install'], {replaceUrl: true});
          }
        }),
        map(installationStatus => installationStatus.installed)
      );
  }
}
