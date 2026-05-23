import {inject, Injectable} from '@angular/core';
import {CanMatch, GuardResult, MaybeAsync, Router} from '@angular/router';
import {AuthenticationService} from './authentication.service';

@Injectable({
  providedIn: 'root'
})
export class NotAuthenticatedGuard implements CanMatch {

  private readonly authenticationService = inject(AuthenticationService);
  private readonly router = inject(Router);

  canMatch(): MaybeAsync<GuardResult> {
    if (!this.authenticationService.isAuthenticated) {
      return true;
    } else {
      this.router.navigate(['/library'], {replaceUrl: true});
      return false;
    }
  }
}
