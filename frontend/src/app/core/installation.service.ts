import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/catch';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from './error.dto';
import {InstallationCommandDto} from './installation-command.dto';
import {InstallationStatusDto} from './installation-status.dto';
import {InstallationDto} from './installation.dto';

@Injectable()
export class InstallationService {

  private installationStatus: InstallationStatusDto;

  constructor(private httpClient: HttpClient) {
  }

  getInstallationStatus(): Observable<InstallationStatusDto> {
    if (this.installationStatus != null) {
      return Observable.of(this.installationStatus);
    } else {
      return this.httpClient.get<InstallationStatusDto>('/api/installation/status')
        .do(installationStatus => this.installationStatus = installationStatus);
    }
  }

  install(installationCommand: InstallationCommandDto): Observable<InstallationDto> {
    return this.httpClient.post<InstallationDto>('/api/installation', installationCommand)
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }
}
