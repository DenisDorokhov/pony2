import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {InstallationStatus} from './installation-status.model';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of'

@Injectable()
export class InstallationService {

  private installationStatus: InstallationStatus;

  constructor(private http: HttpClient) {
  }

  getInstallationStatus(): Observable<InstallationStatus> {
    if (this.installationStatus != null) {
      return Observable.of(this.installationStatus);
    } else {
      return this.http.get<InstallationStatus>('/api/installation/status')
        .do(installationStatus => this.installationStatus = installationStatus);
    }
  }
}
