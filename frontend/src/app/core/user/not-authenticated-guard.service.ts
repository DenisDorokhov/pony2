import {Injectable} from '@angular/core';
import {CanLoad, Route, Router} from '@angular/router';
import {AuthenticationService} from './authentication.service';

@Injectable()
export class NotAuthenticatedGuard implements CanLoad {

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
  ) {
  }

  canLoad(route: Route): boolean {
    if (!this.authenticationService.isAuthenticated) {
      return true;
    } else {
      this.router.navigate(['/library'], {replaceUrl: true});
      return false;
    }
  }
}
