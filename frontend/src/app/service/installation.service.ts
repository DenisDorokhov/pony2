import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {InstallationCommandDto, InstallationDto, InstallationStatusDto} from '../domain/installation.dto';
import {ErrorDto} from "../domain/common.dto";

@Injectable({
  providedIn: 'root'
})
export class InstallationService {

  private installationStatus: InstallationStatusDto | undefined;

  constructor(private httpClient: HttpClient) {
  }

  getInstallationStatus(): Observable<InstallationStatusDto> {
    if (this.installationStatus) {
      return of(this.installationStatus);
    } else {
      return this.httpClient.get<InstallationStatusDto>('/api/installation/status')
        .pipe(
          tap(installationStatus => this.installationStatus = installationStatus)
        );
    }
  }

  install(installationCommand: InstallationCommandDto): Observable<InstallationDto> {
    return this.httpClient.post<InstallationDto>('/api/installation', installationCommand)
      .pipe(
        tap(() => this.installationStatus = undefined),
        catchError(ErrorDto.observableFromHttpErrorResponse)
      );
  }
}
