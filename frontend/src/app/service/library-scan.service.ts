import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {
  BehaviorSubject,
  defer,
  delayWhen,
  interval,
  mergeMap,
  Observable,
  of,
  repeat,
  Subscription,
  throwError,
  timer
} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
import {ScanJobDto, ScanJobPageDto, ScanJobProgressDto, ScanStatisticsDto} from "../domain/library.dto";
import {AuthenticationService} from "./authentication.service";
import {ErrorDto, OptionalResponseDto} from "../domain/common.dto";
import {LibraryService} from "./library.service";
import {NotificationService} from "./notification.service";
import {TranslateService} from "@ngx-translate/core";
import {UserDto} from "../domain/user.dto";
import Status = ScanJobDto.Status;
import Role = UserDto.Role;

@Injectable({
  providedIn: 'root'
})
export class LibraryScanService {

  private scanStatisticsSubject = new BehaviorSubject<ScanStatisticsDto | undefined | null>(undefined);
  private scanJobProgressSubject = new BehaviorSubject<ScanJobProgressDto | undefined | null>(undefined);

  private scanJobProgressUpdateSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

  constructor(
    private libraryService: LibraryService,
    private httpClient: HttpClient,
    private authenticationService: AuthenticationService,
    private notificationService: NotificationService,
    private translateService: TranslateService
  ) {
    this.libraryService.observeRefreshRequest().pipe(
      mergeMap(() => {
        if (this.authenticationService.isAuthenticated) {
          return this.updateScanStatistics();
        }
        return of(undefined);
      })
    ).subscribe();
    this.authenticationService.observeAuthentication().subscribe(user => {
      if (user) {
        this.updateAndScheduleScanJobProgressUpdate();
      } else {
        this.scanJobProgressSubject.next(undefined);
        this.refreshRequestSubscription?.unsubscribe();
        this.refreshRequestSubscription = undefined;
      }
    });
  }

  updateAndScheduleScanJobProgressUpdate() {
    this.scanJobProgressUpdateSubscription?.unsubscribe();
    this.scanJobProgressUpdateSubscription = defer(() => {
      if (this.authenticationService.currentUser?.role === Role.ADMIN) {
        return this.updateScanJobProgress();
      } else {
        return of(undefined);
      }
    })
      .pipe(
        catchError(error => {
          console.error(`Could not update scan job progress: ${JSON.stringify(error)}`);
          return of(undefined);
        }),
        delayWhen(() => this.scanJobProgressSubject.value ? interval(1000) : interval(30000)),
        repeat()
      )
      .subscribe();
  }

  private updateScanJobProgress(): Observable<ScanJobProgressDto | null> {
    return this.httpClient.get<OptionalResponseDto<ScanJobProgressDto>>('/api/admin/library/scanJobProgress')
      .pipe(
        map(optionalResponse => {
          if (optionalResponse.present) {
            console.debug('Scan job progress updated.');
            if (!this.refreshRequestSubscription) {
              console.info("Scheduling auto-refresh during scan job running.");
              this.refreshRequestSubscription = timer(0, 10000).pipe(
                tap(() => {
                  this.libraryService.requestRefresh();
                })
              ).subscribe();
            }
            this.scanJobProgressSubject.next(optionalResponse.value!);
            return optionalResponse.value!;
          } else {
            console.debug('Scan job is not running.');
            if (this.scanJobProgressSubject.value) {
              const oldScanJob = this.scanJobProgressSubject.value!.scanJob;
              this.scanJobProgressSubject.next(null);
              this.showScanJobEndedNotification(oldScanJob!.id!);
            }
            if (this.refreshRequestSubscription) {
              console.info("Scan job finished, cancelling auto-refresh.");
              this.libraryService.requestRefresh();
              this.refreshRequestSubscription.unsubscribe();
              this.refreshRequestSubscription = undefined;
            }
            return null;
          }
        })
      );
  }

  updateScanStatistics(): Observable<ScanStatisticsDto | null> {
    console.info('Updating scan statistics...');
    return this.httpClient.get<OptionalResponseDto<ScanStatisticsDto>>('/api/library/scanStatistics')
      .pipe(
        map(optionalResponse => {
          if (optionalResponse.present) {
            console.info('Scan statistics updated.');
            this.scanStatisticsSubject.next(optionalResponse.value!);
            return optionalResponse.value!;
          } else {
            if (this.scanStatisticsSubject.value !== null) {
              this.scanStatisticsSubject.next(null);
            }
            return null;
          }
        })
      );
  }

  private showScanJobEndedNotification(scanJobId: string) {
    this.getScanJob(scanJobId).subscribe(scanJob => {
      if (scanJob.status === Status.FAILED) {
        this.showScanJobFailedNotification();
      } else if (scanJob.status === Status.INTERRUPTED) {
        this.notificationService.warning(
          this.translateService.instant('notification.scanJobTitle'),
          this.translateService.instant('notification.scanJobInterruptedText')
        );
      } else {
        this.notificationService.success(
          this.translateService.instant('notification.scanJobTitle'),
          this.translateService.instant('notification.scanJobFinishedText')
        );
      }
    });
  }

  private showScanJobFailedNotification() {
    this.notificationService.error(
      this.translateService.instant('notification.scanJobTitle'),
      this.translateService.instant('notification.scanJobFailedText')
    );
  }

  private getScanJob(id: string): Observable<ScanJobDto> {
    return this.httpClient.get<ScanJobDto>('/api/admin/library/scanJobs/' + id);
  }

  observeScanStatistics(): Observable<ScanStatisticsDto | undefined | null> {
    return this.scanStatisticsSubject.asObservable();
  }

  observeScanJobProgress(): Observable<ScanJobProgressDto | undefined | null> {
    return this.scanJobProgressSubject.asObservable();
  }

  getScanJobs(pageIndex = 0, pageSize = 30): Observable<ScanJobPageDto> {
    return this.httpClient.get<ScanJobPageDto>('/api/admin/library/scanJobs', { params: {pageIndex, pageSize} });
  }

  startScanJob(): Observable<ScanJobProgressDto | null> {
    return this.httpClient.post<ScanJobDto>('/api/admin/library/scanJobs', null)
      .pipe(
        mergeMap(scanJob => this.updateScanJobProgress().pipe(
          tap(scanJobProgress => {
            // If scan job finished too fast.
            if (!scanJobProgress) {
              this.showScanJobEndedNotification(scanJob.id);
              this.libraryService.requestRefresh();
            }
          })
        )),
        tap(() => this.updateAndScheduleScanJobProgressUpdate()),
        catchError(httpError => {
          const error = ErrorDto.fromHttpErrorResponse(httpError);
          if (error.code === ErrorDto.Code.CONCURRENT_SCAN) {
            console.warn('Scan job is already running.');
            this.updateAndScheduleScanJobProgressUpdate()
          } else {
            this.showScanJobFailedNotification();
          }
          return throwError(() => error);
        })
      );
  }
}
