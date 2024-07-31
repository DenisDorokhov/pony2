import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {UserCreateCommandDto, UserDto, UserPageDto, UserUpdateCommandDto} from "../domain/user.dto";
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

  getAllUsers(pageIndex = 0, pageSize = 30): Observable<UserPageDto> {
    return this.httpClient.get<UserPageDto>('/api/admin/users', { params: {pageIndex, pageSize} });
  }

  updateUser(command: UserUpdateCommandDto): Observable<UserDto> {
    return this.httpClient.put<UserDto>('/api/admin/users/' + command.id, command).pipe(
      catchError(ErrorDto.observableFromHttpErrorResponse)
    );
  }

  createUser(command: UserCreateCommandDto): Observable<UserDto> {
    return this.httpClient.post<UserDto>('/api/admin/users', command).pipe(
      catchError(ErrorDto.observableFromHttpErrorResponse)
    );
  }
}
