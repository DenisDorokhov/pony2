import {HttpClient} from '@angular/common/http';
import {EventEmitter, Injectable} from '@angular/core';
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

  private _authenticated = new EventEmitter<UserDto>();
  private _loggedOut = new EventEmitter<UserDto>();

  constructor(private tokenStorage: TokenStorageService, private httpClient: HttpClient) {
  }

  get isAuthenticated(): boolean {
    return this._currentUser !== undefined;
  }

  get currentUser(): UserDto | undefined {
    return this._currentUser;
  }

  get authenticated(): Observable<UserDto> {
    return this._authenticated.asObservable();
  }

  get loggedOut(): Observable<UserDto> {
    return this._loggedOut.asObservable();
  }

  authenticate(credentials?: Credentials): Observable<UserDto> {
    let result;
    if (credentials) {
      result = this.authenticateCredentials(credentials);
    } else {
      result = this.authenticateToken();
    }
    return result.do(user => this._authenticated.emit(user));
  }

  logout(): Observable<UserDto> {
    return this.httpClient.delete<UserDto>('/api/authentication')
      .do(() => {
        this.tokenStorage.accessToken = undefined;
        const oldUser = this._currentUser;
        this._currentUser = undefined;
        this._loggedOut.emit(oldUser);
      })
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }

  private authenticateCredentials(credentials: Credentials): Observable<UserDto> {
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
  }

  private authenticateToken(): Observable<UserDto> {
    return this.httpClient.get<UserDto>('/api/user')
      .do(user => {
        this._currentUser = user;
        return this._currentUser;
      })
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }
}
