import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {InstallationCommandDto, InstallationDto, InstallationStatusDto} from '../domain/installation.dto';
import {ErrorDto} from '../domain/common.dto';

@Injectable({
  providedIn: 'root'
})
export class InstallationService {

  private _installationStatus: InstallationStatusDto | undefined;

  constructor(private httpClient: HttpClient) {
  }

  get installationStatus(): InstallationStatusDto | undefined {
    return this._installationStatus;
  }

  requestInstallationStatus(): Observable<InstallationStatusDto> {
    if (this._installationStatus) {
      return of(this._installationStatus);
    } else {
      return this.httpClient.get<InstallationStatusDto>('/api/installation/status')
        .pipe(
          tap(installationStatus => this._installationStatus = installationStatus)
        );
    }
  }

  install(installationCommand: InstallationCommandDto): Observable<InstallationDto> {
    return this.httpClient.post<InstallationDto>('/api/installation', installationCommand)
      .pipe(
        tap(() => this._installationStatus = undefined),
        catchError(ErrorDto.observableFromHttpErrorResponse)
      );
  }
}
