import {inject, Injectable} from '@angular/core';
import {CanMatch, GuardResult, MaybeAsync, Router} from '@angular/router';
import {AuthenticationService} from './authentication.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticatedGuard implements CanMatch {

  private readonly authenticationService = inject(AuthenticationService);
  private readonly router = inject(Router);

  canMatch(): MaybeAsync<GuardResult> {
    if (this.authenticationService.isAuthenticated) {
      return true;
    } else {
      this.router.navigate(['/login'], {replaceUrl: true});
      return false;
    }
  }
}
