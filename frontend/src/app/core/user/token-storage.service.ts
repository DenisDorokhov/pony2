import {Injectable} from '@angular/core';
import {CookieService} from 'ngx-cookie';

@Injectable()
export class TokenStorageService {

  private static readonly TOKEN_KEY: string = 'pony2.AuthenticationService.token';
  private static readonly COOKIE_KEY: string = 'pony2.token';

  constructor(private cookieService: CookieService) {
  }

  get token(): string {
    return localStorage.getItem(TokenStorageService.TOKEN_KEY);
  }

  set token(token: string) {
    if (token) {
      localStorage.setItem(TokenStorageService.TOKEN_KEY, token);
      this.cookieService.put(TokenStorageService.COOKIE_KEY, token);
    } else {
      localStorage.removeItem(TokenStorageService.TOKEN_KEY);
      this.cookieService.get(TokenStorageService.COOKIE_KEY);
    }
  }
}
