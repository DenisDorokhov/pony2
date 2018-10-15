import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import * as Logger from 'js-logger';
import {BehaviorSubject, Observable, of, throwError} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {ErrorDto} from '../common/common.dto';
import {AuthenticationService} from '../user/authentication.service';
import {ScanStatisticsDto} from './library.dto';

@Injectable()
export class LibraryScanService {
  
  private scanStatisticsSubject = new BehaviorSubject<ScanStatisticsDto | undefined>(undefined);

  constructor(
    private httpClient: HttpClient,
    private authenticationService: AuthenticationService
  ) {
    if (this.authenticationService.isAuthenticated) {
      this.updateScanStatistics();
    }
    this.authenticationService.observeAuthentication()
      .subscribe(() => this.updateScanStatistics());
  }
  
  observeScanStatistics(): Observable<ScanStatisticsDto> {
    return this.scanStatisticsSubject.asObservable();
  }
  
  private updateScanStatistics() {
    Logger.info('Updating scan statistics...');
    return this.httpClient.get<ScanStatisticsDto>('/api/library/scanStatistics')
      .pipe(
        tap(scanStatistics => {
          Logger.info('Scan statistics updated.');
          this.scanStatisticsSubject.next(scanStatistics);
        }),
        catchError(httpError => {
          const error = ErrorDto.fromHttpErrorResponse(httpError);
          if (error.code === ErrorDto.Code.NOT_FOUND) {
            Logger.info('Library was not scanned yet.');
            return of(undefined);
          } else {
            throwError(error);
          }
        })
      )
      .subscribe();
  }
}
