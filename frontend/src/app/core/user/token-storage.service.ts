import {Injectable} from '@angular/core';
import {CookieService} from 'ngx-cookie';

@Injectable()
export class TokenStorageService {

  private static readonly ACCESS_TOKEN_LOCAL_STORAGE_KEY: string = 'pony2.AuthenticationService.accessToken';
  private static readonly STATIC_TOKEN_LOCAL_STORAGE_KEY: string = 'pony2.AuthenticationService.staticToken';

  private static readonly STATIC_TOKEN_COOKIE_KEY: string = 'pony2.staticToken';
  
  private _accessToken: string | undefined;
  private _staticToken: string | undefined;

  constructor(private cookieService: CookieService) {
    this._accessToken = localStorage.getItem(TokenStorageService.ACCESS_TOKEN_LOCAL_STORAGE_KEY);
    this._staticToken = localStorage.getItem(TokenStorageService.STATIC_TOKEN_LOCAL_STORAGE_KEY);
    this.cookieService.put(TokenStorageService.STATIC_TOKEN_COOKIE_KEY, this._staticToken);
  }

  get accessToken(): string {
    return this._accessToken;
  }

  set accessToken(accessToken: string) {
    if (accessToken) {
      localStorage.setItem(TokenStorageService.ACCESS_TOKEN_LOCAL_STORAGE_KEY, accessToken);
    } else {
      localStorage.removeItem(TokenStorageService.ACCESS_TOKEN_LOCAL_STORAGE_KEY);
    }
    this._accessToken = accessToken;
  }
  
  get staticToken(): string {
    return this._staticToken;
  }
  
  set staticToken(staticToken: string) {
    if (staticToken) {
      localStorage.setItem(TokenStorageService.STATIC_TOKEN_LOCAL_STORAGE_KEY, staticToken);
      this.cookieService.put(TokenStorageService.STATIC_TOKEN_COOKIE_KEY, staticToken);
    } else {
      localStorage.removeItem(TokenStorageService.STATIC_TOKEN_LOCAL_STORAGE_KEY);
      this.cookieService.remove(TokenStorageService.STATIC_TOKEN_COOKIE_KEY);
    }
    this._staticToken = staticToken;
  }
}
