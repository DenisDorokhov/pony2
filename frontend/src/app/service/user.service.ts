import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {UserDto} from "../domain/user.dto";
import {HttpClient} from "@angular/common/http";
import {catchError} from "rxjs/operators";
import {ErrorDto} from "../domain/common.dto";

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private httpClient: HttpClient
  ) {
  }

  getAllUsers(): Observable<UserDto[]> {
    return this.httpClient.get<UserDto[]>('/api/admin/users')
      .pipe(
        catchError(ErrorDto.observableFromHttpErrorResponse)
      );
  }
}
