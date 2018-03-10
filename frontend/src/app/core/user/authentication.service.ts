import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/error.dto';
import {AuthenticationDto} from './authentication.dto';
import {Credentials} from './credentials.model';
import {TokenStorageService} from './token-storage.service';
import {UserDto} from './user.dto';

@Injectable()
export class AuthenticationService {

  private _currentUser: UserDto;

  constructor(private tokenStorage: TokenStorageService, private httpClient: HttpClient) {
  }

  get isAuthenticated(): boolean {
    return this._currentUser !== undefined;
  }

  get currentUser(): UserDto {
    return this._currentUser;
  }

  authenticate(credentials?: Credentials): Observable<UserDto> {
    if (credentials) {
      return this.authenticateCredentials(credentials);
    } else {
      return this.authenticateToken();
    }
  }

  logout(): Observable<UserDto> {
    return this.httpClient.delete<UserDto>('/api/authentication')
      .do(() => {
        this._currentUser = undefined;
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
        this.tokenStorage.token = authentication.token;
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
