import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs/add/observable/of';
import 'rxjs/add/observable/throw';
import 'rxjs/add/operator/catch';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/common.dto';
import {InstallationCommandDto, InstallationDto, InstallationStatusDto} from './installation.model';

@Injectable()
export class InstallationService {

  private installationStatus: InstallationStatusDto;

  constructor(private httpClient: HttpClient) {
  }

  getInstallationStatus(): Observable<InstallationStatusDto> {
    if (this.installationStatus) {
      return Observable.of(this.installationStatus);
    } else {
      return this.httpClient.get<InstallationStatusDto>('/api/installation/status')
        .do(installationStatus => this.installationStatus = installationStatus)
        .catch(ErrorDto.observableFromHttpErrorResponse);
    }
  }

  install(installationCommand: InstallationCommandDto): Observable<InstallationDto> {
    return this.httpClient.post<InstallationDto>('/api/installation', installationCommand)
      .do(installation => this.installationStatus = undefined)
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }
}
