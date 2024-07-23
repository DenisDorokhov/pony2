import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {
  BehaviorSubject,
  defer, delay,
  delayWhen,
  interval,
  mergeMap,
  Observable,
  of,
  repeat,
  Subscription,
  throwError
} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {ScanJobDto, ScanJobPageDto, ScanJobProgressDto, ScanStatisticsDto} from "../domain/library.dto";
import {AuthenticationService} from "./authentication.service";
import {ErrorDto} from "../domain/common.dto";
import Logger from "js-logger";

@Injectable({
  providedIn: 'root'
})
export class LibraryScanService {

  private scanStatisticsSubject = new BehaviorSubject<ScanStatisticsDto | undefined>(undefined);
  private scanJobProgressSubject = new BehaviorSubject<ScanJobProgressDto | undefined>(undefined);

  private scanJobProgressUpdateSubscription: Subscription | undefined;

  constructor(
    private httpClient: HttpClient,
    private authenticationService: AuthenticationService
  ) {
    this.scheduleScanJobProgressUpdate();
    if (this.authenticationService.isAuthenticated) {
      this.updateScanStatistics();
    }
    this.authenticationService.observeAuthentication()
      .subscribe(() => this.updateScanStatistics());
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
            return throwError(() => error);
          }
        })
      )
      .subscribe();
  }

  private scheduleScanJobProgressUpdate() {
    this.scanJobProgressUpdateSubscription?.unsubscribe();
    this.scanJobProgressUpdateSubscription = defer(() => {
      if (this.authenticationService.isAuthenticated) {
        return this.updateScanJobProgress();
      } else {
        return of(undefined);
      }
    })
      .pipe(
        catchError(error => {
          Logger.error(`Could not update scan job progress: ${JSON.stringify(error)}`);
          return of(undefined);
        }),
        delayWhen(() => this.scanJobProgressSubject.value !== undefined ? interval(1000) : interval(30000)),
        repeat()
      )
      .subscribe();
  }

  updateScanJobProgress(): Observable<ScanJobProgressDto | undefined> {
    return this.httpClient.get<ScanJobProgressDto>('/api/admin/library/scanJobProgress')
      .pipe(
        tap(scanJobProgress => {
          Logger.info('Scan job progress updated.');
          this.scanJobProgressSubject.next(scanJobProgress);
        }),
        catchError(httpError => {
          const error = ErrorDto.fromHttpErrorResponse(httpError);
          if (error.code === ErrorDto.Code.NOT_FOUND) {
            Logger.info('Scan job is not running.');
            this.scanJobProgressSubject.next(undefined)
            return of(undefined);
          } else {
            return throwError(() => error);
          }
        })
      );
  }

  observeScanStatistics(): Observable<ScanStatisticsDto | undefined> {
    return this.scanStatisticsSubject.asObservable();
  }

  observeScanJobProgress(): Observable<ScanJobProgressDto | undefined> {
    return this.scanJobProgressSubject.asObservable();
  }

  getScanJobs(pageIndex = 0, pageSize = 30): Observable<ScanJobPageDto> {
    return this.httpClient.get<ScanJobPageDto>('/api/admin/library/scanJobs', { params: {pageIndex, pageSize} })
      .pipe(
        catchError(error => {
          Logger.error(`Could not get scan jobs: ${JSON.stringify(error)}`);
          throw ErrorDto.fromHttpErrorResponse(error);
        })
      );
  }

  startScanJob(): Observable<ScanJobProgressDto | undefined> {
    return this.httpClient.post<ScanJobDto>('/api/admin/library/scanJobs', null)
      .pipe(
        delay(1000),
        mergeMap(() => this.updateScanJobProgress()),
        tap(() => this.scheduleScanJobProgressUpdate()),
        catchError(error => {
          Logger.error(`Could not start scan job: ${JSON.stringify(error)}`);
          throw ErrorDto.fromHttpErrorResponse(error);
        })
      );
  }
}
