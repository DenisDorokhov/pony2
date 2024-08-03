import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {ConfigDto} from "../domain/config.dto";
import {HttpClient} from "@angular/common/http";
import {catchError} from "rxjs/operators";
import {ErrorDto} from "../domain/common.dto";

@Injectable({
  providedIn: 'root'
})
export class ConfigService {

  constructor(private httpClient: HttpClient) {
  }

  getConfig(): Observable<ConfigDto> {
    return this.httpClient.get<ConfigDto>('/api/admin/config');
  }

  saveConfig(config: ConfigDto): Observable<ConfigDto> {
    return this.httpClient.put<ConfigDto>('/api/admin/config', config).pipe(
      catchError(ErrorDto.observableFromHttpErrorResponse)
    );
  }
}
