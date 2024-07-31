import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {UserPageDto} from "../domain/user.dto";
import {HttpClient} from "@angular/common/http";

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
}
