import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {InstallationStatus} from './installation-status.model';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class InstallationService {

  constructor(private http: HttpClient) {
  }

  getInstallationStatus(): Observable<InstallationStatus> {
    return this.http.get<InstallationStatus>('/api/installation/status');
  }
}
