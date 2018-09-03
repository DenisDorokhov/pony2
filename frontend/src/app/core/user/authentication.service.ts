import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import 'rxjs/add/observable/defer';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/common.dto';
import {TokenStorageService} from './token-storage.service';
import {AuthenticationDto, UserDto} from './user.dto';

export class Credentials {
  email: string;
  password: string;
}

@Injectable()
export class AuthenticationService {

  private _currentUser: UserDto | undefined;

  private _authenticationSubject = new Subject<UserDto>();
  private _logoutSubject = new Subject<UserDto>();

  constructor(private tokenStorage: TokenStorageService, private httpClient: HttpClient) {
  }

  get isAuthenticated(): boolean {
    return this._currentUser !== undefined;
  }

  get currentUser(): UserDto | undefined {
    return this._currentUser;
  }

  authenticate(credentials?: Credentials): Observable<UserDto> {
    let result;
    if (credentials) {
      result = this.authenticateCredentials(credentials);
    } else {
      result = this.authenticateAccessToken();
    }
    return result.do(user => this._authenticationSubject.next(user));
  }

  observeAuthentication(): Observable<UserDto> {
    return this._authenticationSubject.asObservable();
  }

  logout(): Observable<UserDto> {
    return this.httpClient.delete<UserDto>('/api/authentication')
      .do(() => {
        this.tokenStorage.accessToken = undefined;
        const oldUser = this._currentUser;
        this._currentUser = undefined;
        this._logoutSubject.next(oldUser);
      })
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }

  observeLogout(): Observable<UserDto> {
    return this._logoutSubject.asObservable();
  }

  private authenticateCredentials(credentials: Credentials): Observable<UserDto> {
    return Observable.defer(() => {
      const params = {
        'email': credentials.email,
        'password': credentials.password
      };
      return this.httpClient.post<AuthenticationDto>('/api/authentication', null, {params: params})
        .map(authentication => {
          this.tokenStorage.accessToken = authentication.accessToken;
          this._currentUser = authentication.user;
          return this._currentUser;
        })
        .catch(ErrorDto.observableFromHttpErrorResponse);
    });
  }

  private authenticateAccessToken(): Observable<UserDto> {
    return Observable.defer(() => {
      if (!this.tokenStorage.accessToken) {
        return Observable.throwError(ErrorDto.authenticationFailed());
      }
      return this.httpClient.get<UserDto>('/api/user')
        .do(user => {
          this._currentUser = user;
          return this._currentUser;
        })
        .catch(ErrorDto.observableFromHttpErrorResponse);
    });
  }
}
