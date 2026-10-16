import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {DiscoveryJobDto, DiscoveryJobProgressDto, DiscoveryProgressDto, DiscoveryType} from '../../../domain/library.dto';
import {LoadingState} from '../../../domain/common.model';
import {CommonModule} from '@angular/common';
import {ErrorDto, PageDto} from '../../../domain/common.dto';
import {Subscription} from 'rxjs';
import {LibraryService} from '../../../service/library.service';
import {DiscoveryJobComponent} from './discovery-job.component';

@Component({
    imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent],
    selector: 'pony-discovery',
    templateUrl: './discovery.component.html',
    styleUrls: ['./discovery.component.scss']
})
export class DiscoveryComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
  private readonly modal = inject(NgbModal);

  LoadingState = LoadingState;
  Step = DiscoveryProgressDto.Step;
  DiscoveryJobStatus = DiscoveryJobDto.Status;
  DiscoveryType = DiscoveryType;

  discoveryJobProgressLoadingState = LoadingState.LOADING;
  discoveryJobsLoadingState = LoadingState.LOADING;

  discoveryJobProgress: DiscoveryJobProgressDto | undefined | null;
  discoveryJobs: DiscoveryJobDto[] = [];
  currentPage: PageDto | undefined;
  pageToLoad: PageDto | undefined;
  emptyDiscoveryJobRowCount = 5;
  discoveryProgressValue: number | null | undefined;

  private discoveryJobProgressSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;
  private discoveryJobsSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.discoveryJobProgressSubscription = this.libraryDiscoveryService.observeDiscoveryJobProgress().subscribe({
      next: discoveryJobProgress => {
        this.discoveryJobProgress = discoveryJobProgress;
        if (discoveryJobProgress) {
          if (discoveryJobProgress.discoveryProgress?.value && discoveryJobProgress.discoveryProgress.value.itemsTotal > 0) {
            this.discoveryProgressValue = discoveryJobProgress.discoveryProgress.value.itemsComplete * 100 / discoveryJobProgress.discoveryProgress.value.itemsTotal;
          } else {
            this.discoveryProgressValue = null;
          }
        } else {
          this.discoveryProgressValue = undefined;
        }
        this.discoveryJobProgressLoadingState = LoadingState.LOADED;
      },
      error: () => this.discoveryJobProgressLoadingState = LoadingState.ERROR
    });
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest().subscribe(() => {
      this.loadDiscoveryJobs(this.pageToLoad?.pageIndex ?? 0, this.pageToLoad?.pageSize ?? 5);
    });
    this.libraryDiscoveryService.updateAndScheduleDiscoveryJobProgressUpdate();
    this.discoveryJobsLoadingState = LoadingState.LOADING;
    this.loadDiscoveryJobs();
  }

  ngOnDestroy(): void {
    this.discoveryJobProgressSubscription?.unsubscribe();
    this.refreshRequestSubscription?.unsubscribe();
    this.discoveryJobsSubscription?.unsubscribe();
  }

  private loadDiscoveryJobs(pageIndex = 0, pageSize = 5) {
    this.pageToLoad = {pageIndex, pageSize, totalPages: this.currentPage?.totalPages ?? 0};
    this.discoveryJobsSubscription?.unsubscribe();
    this.discoveryJobsSubscription = this.libraryDiscoveryService.getDiscoveryJobs(pageIndex, pageSize).subscribe({
      next: discoveryJobPage => {
        this.discoveryJobs = discoveryJobPage.discoveryJobs;
        this.currentPage = this.pageToLoad = discoveryJobPage;
        this.emptyDiscoveryJobRowCount = Math.max(0, 5 - this.discoveryJobs.length);
        this.discoveryJobsLoadingState = this.discoveryJobs.length > 0 ? LoadingState.LOADED : LoadingState.EMPTY;
      },
      error: () => {
        this.discoveryJobsLoadingState = LoadingState.ERROR;
      }
    });
  }

  startFullDiscoveryJob() {
    // We will load first page next time to show the discovery job appeared.
    this.pageToLoad = {
      pageIndex: 0,
      pageSize: this.pageToLoad?.pageSize ?? 5,
      totalPages: this.pageToLoad?.totalPages ?? 0
    };
    this.discoveryJobProgressLoadingState = LoadingState.LOADING;
    this.libraryDiscoveryService.startFullDiscoveryJob().subscribe({
      next: () => {
        this.discoveryJobProgressLoadingState = LoadingState.LOADED;
      },
      error: (error: ErrorDto) => {
        if (error.code === ErrorDto.Code.CONCURRENT_LIBRARY_JOB) {
          this.discoveryJobProgressLoadingState = LoadingState.LOADED;
        } else {
          this.discoveryJobProgressLoadingState = LoadingState.ERROR;
        }
      }
    });
  }

  loadDiscoveryJobsPreviousPage() {
    if (this.currentPage && this.currentPage.pageIndex > 0) {
      this.loadDiscoveryJobs(this.currentPage.pageIndex - 1);
    }
  }

  loadDiscoveryJobsNextPage() {
    if (this.currentPage && this.currentPage.pageIndex < (this.currentPage.totalPages - 1)) {
      this.loadDiscoveryJobs(this.currentPage.pageIndex + 1);
    }
  }

  showDetails(discoveryJob: DiscoveryJobDto) {
    const modalRef = this.modal.open(DiscoveryJobComponent, {size: 'lg'});
    (modalRef.componentInstance as DiscoveryJobComponent).discoveryJobId = discoveryJob.id;
  }
}
