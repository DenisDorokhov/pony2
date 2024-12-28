import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject, defer, Observable, Subject, throwError} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
import {TokenStorageService} from './token-storage.service';
import {AuthenticationDto, UserDto} from '../domain/user.dto';
import {ErrorDto} from '../domain/common.dto';

export class Credentials {
  email: string | undefined;
  password: string | undefined;
}

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  private _currentUser: UserDto | undefined;

  private authenticationSubject = new BehaviorSubject<UserDto | undefined>(undefined);
  private logoutSubject = new Subject<UserDto | undefined>();

  constructor(private tokenStorage: TokenStorageService, private httpClient: HttpClient) {
  }

  get isAuthenticated(): boolean {
    return this._currentUser !== undefined;
  }

  get currentUser(): UserDto | undefined {
    return this._currentUser;
  }

  authenticate(credentials?: Credentials): Observable<UserDto> {
    let result: Observable<UserDto>;
    if (credentials) {
      result = this.authenticateCredentials(credentials);
    } else {
      result = this.authenticateAccessToken();
    }
    return result
      .pipe(tap(user => this.authenticationSubject.next(user)));
  }

  observeAuthentication(): Observable<UserDto | undefined> {
    return this.authenticationSubject.asObservable();
  }

  logout(): Observable<UserDto> {
    return this.httpClient.delete<UserDto>('/api/authentication')
      .pipe(
        tap(() => this.doLogout()),
        catchError(error => {
          this.doLogout();
          return throwError(() => error);
        })
      );
  }

  private doLogout() {
    this.tokenStorage.accessToken = undefined;
    this.tokenStorage.staticToken = undefined;
    const oldUser = this._currentUser;
    this._currentUser = undefined;
    this.authenticationSubject.next(undefined);
    this.logoutSubject.next(oldUser);
  }

  observeLogout(): Observable<UserDto | undefined> {
    return this.logoutSubject.asObservable();
  }

  private authenticateCredentials(credentials: Credentials): Observable<UserDto> {
    return defer(() => {
      const params: FormData = new FormData();
      params.set('email', credentials.email!);
      params.set('password', credentials.password!);
      return this.httpClient.post<AuthenticationDto>('/api/authentication', params)
        .pipe(
          map(authentication => {
            this.tokenStorage.accessToken = authentication.accessToken;
            this.tokenStorage.staticToken = authentication.staticToken;
            this._currentUser = authentication.user;
            return this._currentUser;
          }),
          catchError(ErrorDto.observableFromHttpErrorResponse)
        );
    });
  }

  private authenticateAccessToken(): Observable<UserDto> {
    return defer(() => {
      if (!this.tokenStorage.accessToken) {
        return throwError(() => ErrorDto.authenticationFailed());
      }
      return this.httpClient.get<UserDto>('/api/user')
        .pipe(
          tap(user => {
            this._currentUser = user;
            return this._currentUser;
          }),
          catchError(ErrorDto.observableFromHttpErrorResponse)
        );
    });
  }
}
