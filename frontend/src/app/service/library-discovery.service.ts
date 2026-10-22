import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
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
import {
  DiscoveryJobDetailsDto,
  DiscoveryJobDto,
  DiscoveryJobPageDto,
  DiscoveryJobProgressDto,
  DiscoveryTaskDto,
  DiscoveryTaskPageDto,
  DiscoveryTaskResultFilter,
  DiscoveryTaskType
} from '../domain/library.dto';
import {AuthenticationService} from './authentication.service';
import {ErrorDto, OptionalResponseDto} from '../domain/common.dto';
import {LibraryService} from './library.service';
import {NotificationService} from './notification.service';
import {TranslateService} from '@ngx-translate/core';
import {UserDto} from '../domain/user.dto';
import Status = DiscoveryJobDto.Status;
import Role = UserDto.Role;

@Injectable({
  providedIn: 'root'
})
export class LibraryDiscoveryService {

  private readonly libraryService = inject(LibraryService);
  private readonly httpClient = inject(HttpClient);
  private readonly authenticationService = inject(AuthenticationService);
  private readonly notificationService = inject(NotificationService);
  private readonly translateService = inject(TranslateService);

  private discoveryJobProgressSubject = new BehaviorSubject<DiscoveryJobProgressDto | undefined | null>(undefined);

  private discoveryJobProgressUpdateSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

  constructor() {
    this.authenticationService.observeAuthentication().subscribe(user => {
      if (user?.role === Role.ADMIN) {
        this.updateAndScheduleDiscoveryJobProgressUpdate();
      } else {
        this.discoveryJobProgressUpdateSubscription?.unsubscribe();
        this.discoveryJobProgressUpdateSubscription = undefined;
        this.discoveryJobProgressSubject.next(undefined);
        this.refreshRequestSubscription?.unsubscribe();
        this.refreshRequestSubscription = undefined;
      }
    });
  }

  updateAndScheduleDiscoveryJobProgressUpdate() {
    this.discoveryJobProgressUpdateSubscription?.unsubscribe();
    this.discoveryJobProgressUpdateSubscription = defer(() => {
      if (this.authenticationService.currentUser?.role === Role.ADMIN) {
        return this.updateDiscoveryJobProgress();
      } else {
        return of(undefined);
      }
    })
      .pipe(
        catchError(error => {
          console.error(`Could not update discovery job progress: ${JSON.stringify(error)}`);
          return of(undefined);
        }),
        delayWhen(() => this.discoveryJobProgressSubject.value ? interval(1000) : interval(30000)),
        repeat()
      )
      .subscribe();
  }

  private updateDiscoveryJobProgress(): Observable<DiscoveryJobProgressDto | null> {
    return this.httpClient.get<OptionalResponseDto<DiscoveryJobProgressDto>>('/api/admin/library/discoveryJobProgress')
      .pipe(
        map(optionalResponse => {
          if (optionalResponse.present) {
            if (!this.refreshRequestSubscription) {
              console.info('Scheduling auto-refresh during discovery job running.');
              this.refreshRequestSubscription = timer(0, 10000).pipe(
                tap(() => {
                  this.libraryService.requestRefresh();
                })
              ).subscribe();
            }
            this.discoveryJobProgressSubject.next(optionalResponse.value!);
            return optionalResponse.value!;
          } else {
            if (this.discoveryJobProgressSubject.value) {
              const oldDiscoveryJob = this.discoveryJobProgressSubject.value!.discoveryJob;
              this.discoveryJobProgressSubject.next(null);
              this.showDiscoveryJobEndedNotification(oldDiscoveryJob!.id!);
            }
            if (this.refreshRequestSubscription) {
              console.info('Discovery job finished, cancelling auto-refresh.');
              this.libraryService.requestRefresh();
              this.refreshRequestSubscription.unsubscribe();
              this.refreshRequestSubscription = undefined;
            }
            return null;
          }
        })
      );
  }

  private showDiscoveryJobEndedNotification(discoveryJobId: string) {
    this.getDiscoveryJob(discoveryJobId).subscribe(discoveryJob => {
      if (discoveryJob.status === Status.FAILED) {
        this.showDiscoveryJobFailedNotification();
      } else if (discoveryJob.status === Status.INTERRUPTED) {
        this.notificationService.warning(
          this.translateService.instant('notification.discoveryJobTitle'),
          this.translateService.instant('notification.discoveryJobInterruptedText')
        );
      } else if (discoveryJob.status === Status.MODERATE) {
        this.notificationService.warning(
          this.translateService.instant('notification.discoveryJobTitle'),
          this.translateService.instant('notification.discoveryJobModerateText')
        );
      } else if (discoveryJob.status === Status.COMPLETE) {
        this.notificationService.success(
          this.translateService.instant('notification.discoveryJobTitle'),
          this.translateService.instant('notification.discoveryJobFinishedText')
        );
      }
    });
  }

  private showDiscoveryJobFailedNotification() {
    this.notificationService.error(
      this.translateService.instant('notification.discoveryJobTitle'),
      this.translateService.instant('notification.discoveryJobFailedText')
    );
  }

  getDiscoveryJob(id: string): Observable<DiscoveryJobDto> {
    return this.httpClient.get<DiscoveryJobDto>('/api/admin/library/discoveryJobs/' + id);
  }

  getDiscoveryJobDetails(id: string): Observable<DiscoveryJobDetailsDto> {
    return this.httpClient.get<DiscoveryJobDetailsDto>(`/api/admin/library/discoveryJobs/${id}/details`);
  }

  getDiscoveryTasks(jobId: string, result?: DiscoveryTaskResultFilter, type?: DiscoveryTaskType, pageIndex = 0, pageSize = 30): Observable<DiscoveryTaskPageDto> {
    return this.getDiscoveryTasksPage(`/api/admin/library/discoveryJobs/${jobId}/tasks`, result, type, pageIndex, pageSize);
  }

  private getDiscoveryTasksPage(url: string, result: DiscoveryTaskResultFilter | undefined, type: DiscoveryTaskType | undefined, pageIndex: number, pageSize: number): Observable<DiscoveryTaskPageDto> {
    const params: Record<string, string | number> = {pageIndex, pageSize};
    if (result) {
      params['result'] = result;
    }
    if (type) {
      params['type'] = type;
    }
    return this.httpClient.get<DiscoveryTaskPageDto>(url, {params});
  }

  getArtistDiscoveryTasks(artistId: string, result?: DiscoveryTaskResultFilter, type?: DiscoveryTaskType, pageIndex = 0, pageSize = 30): Observable<DiscoveryTaskPageDto> {
    return this.getDiscoveryTasksPage(`/api/admin/library/artists/${artistId}/discoveryTasks`, result, type, pageIndex, pageSize);
  }

  getAlbumDiscoveryTasks(albumId: string, result?: DiscoveryTaskResultFilter, type?: DiscoveryTaskType, pageIndex = 0, pageSize = 30): Observable<DiscoveryTaskPageDto> {
    return this.getDiscoveryTasksPage(`/api/admin/library/albums/${albumId}/discoveryTasks`, result, type, pageIndex, pageSize);
  }

  getDiscoveryTask(id: string): Observable<DiscoveryTaskDto> {
    return this.httpClient.get<DiscoveryTaskDto>('/api/admin/library/discoveryTasks/' + id);
  }

  observeDiscoveryJobProgress(): Observable<DiscoveryJobProgressDto | undefined | null> {
    return this.discoveryJobProgressSubject.asObservable();
  }

  getDiscoveryJobs(pageIndex = 0, pageSize = 30): Observable<DiscoveryJobPageDto> {
    return this.httpClient.get<DiscoveryJobPageDto>('/api/admin/library/discoveryJobs', { params: {pageIndex, pageSize} });
  }

  startFullDiscoveryJob(cacheEnabled = true): Observable<DiscoveryJobProgressDto | null> {
    return this.startDiscoveryJob('/api/admin/library/discoveryJobs/full', cacheEnabled);
  }

  startArtistDiscoveryJob(artistId: string, cacheEnabled = true): Observable<DiscoveryJobProgressDto | null> {
    return this.startDiscoveryJob(`/api/admin/library/discoveryJobs/artist/${artistId}`, cacheEnabled);
  }

  startAlbumDiscoveryJob(albumId: string, cacheEnabled = true): Observable<DiscoveryJobProgressDto | null> {
    return this.startDiscoveryJob(`/api/admin/library/discoveryJobs/album/${albumId}`, cacheEnabled);
  }

  private startDiscoveryJob(url: string, cacheEnabled: boolean): Observable<DiscoveryJobProgressDto | null> {
    return this.httpClient.post<DiscoveryJobDto>(url, null, {params: {cacheEnabled}})
      .pipe(
        mergeMap(discoveryJob => this.updateDiscoveryJobProgress().pipe(
          tap(discoveryJobProgress => {
            // If discovery job finished too fast.
            if (!discoveryJobProgress) {
              this.showDiscoveryJobEndedNotification(discoveryJob.id);
              this.libraryService.requestRefresh();
            }
          })
        )),
        tap(() => this.updateAndScheduleDiscoveryJobProgressUpdate()),
        catchError(httpError => {
          const error = ErrorDto.fromHttpErrorResponse(httpError);
          if (error.code === ErrorDto.Code.CONCURRENT_LIBRARY_JOB) {
            this.updateAndScheduleDiscoveryJobProgressUpdate();
          }
          this.showDiscoveryJobFailedNotification();
          return throwError(() => error);
        })
      );
  }
}
