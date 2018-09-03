import {Injectable} from '@angular/core';
import {CookieService} from 'ngx-cookie';

@Injectable()
export class TokenStorageService {

  private static readonly TOKEN_KEY: string = 'pony2.AuthenticationService.token';
  private static readonly COOKIE_KEY: string = 'pony2.staticToken';

  constructor(private cookieService: CookieService) {
  }

  get accessToken(): string {
    return localStorage.getItem(TokenStorageService.TOKEN_KEY);
  }

  set accessToken(token: string) {
    if (token) {
      localStorage.setItem(TokenStorageService.TOKEN_KEY, token);
      this.cookieService.put(TokenStorageService.COOKIE_KEY, token);
    } else {
      localStorage.removeItem(TokenStorageService.TOKEN_KEY);
      this.cookieService.remove(TokenStorageService.COOKIE_KEY);
    }
  }
}
