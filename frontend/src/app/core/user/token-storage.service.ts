import {Injectable} from '@angular/core';
import {CookieService} from 'ngx-cookie';

@Injectable()
export class TokenStorageService {

  private static readonly LOCAL_STORAGE_KEY: string = 'pony2.AuthenticationService.token';
  private static readonly COOKIE_KEY: string = 'pony2.staticToken';
  
  private _accessToken: string | undefined;

  constructor(private cookieService: CookieService) {
    this._accessToken = localStorage.getItem(TokenStorageService.LOCAL_STORAGE_KEY);
    this.synchronizeCookies();
  }

  get accessToken(): string {
    return this._accessToken;
  }

  set accessToken(accessToken: string) {
    if (accessToken) {
      localStorage.setItem(TokenStorageService.LOCAL_STORAGE_KEY, accessToken);
    } else {
      localStorage.removeItem(TokenStorageService.LOCAL_STORAGE_KEY);
    }
    this._accessToken = accessToken;
    this.synchronizeCookies();
  }
  
  private synchronizeCookies() {
    if (this._accessToken) {
      this.cookieService.put(TokenStorageService.COOKIE_KEY, this._accessToken);
    } else {
      this.cookieService.remove(TokenStorageService.COOKIE_KEY);
    }
  }
}
