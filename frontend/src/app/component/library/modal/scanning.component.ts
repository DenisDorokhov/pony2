import {Component, OnDestroy, OnInit} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {TranslateModule} from "@ngx-translate/core";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";
import {LibraryScanService} from "../../../service/library-scan.service";
import {ScanJobDto, ScanJobProgressDto, ScanProgressDto} from "../../../domain/library.dto";
import {LoadingState} from "../../../domain/common.model";
import Logger from "js-logger";
import {CommonModule} from "@angular/common";
import {PageDto} from "../../../domain/common.dto";
import {Subscription} from "rxjs";
import {LibraryService} from "../../../service/library.service";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent],
  selector: 'pony-scanning',
  templateUrl: './scanning.component.html',
  styleUrls: ['./scanning.component.scss']
})
export class ScanningComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;
  Step = ScanProgressDto.Step;
  ScanJobStatus = ScanJobDto.Status;

  scanJobProgressLoadingState = LoadingState.LOADING;
  scanJobsLoadingState = LoadingState.LOADING;

  scanJobProgress: ScanJobProgressDto | undefined | null;
  scanJobs: ScanJobDto[] = [];
  page: PageDto | undefined;
  emptyScanJobRowCount = 5;
  scanProgressValue: number | null | undefined;

  private scanJobProgressSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

  constructor(
    private libraryScanService: LibraryScanService,
    private libraryService: LibraryService,
    public readonly activeModal: NgbActiveModal
  ) {}

  ngOnInit(): void {
    this.scanJobProgressLoadingState = LoadingState.LOADING;
    this.scanJobProgressSubscription = this.libraryScanService.observeScanJobProgress().subscribe({
      next: scanJobProgress => {
        this.scanJobProgress = scanJobProgress;
        if (scanJobProgress) {
          if (scanJobProgress.scanProgress.value) {
            this.scanProgressValue = scanJobProgress?.scanProgress.value.itemsComplete * 100 / scanJobProgress?.scanProgress.value.itemsTotal;
          } else {
            this.scanProgressValue = null;
          }
        } else {
          this.scanProgressValue = undefined;
        }
        this.scanJobProgressLoadingState = LoadingState.LOADED;
      },
      error: error => {
        this.scanJobProgressLoadingState = LoadingState.ERROR;
        Logger.error(`Could not load scan job progress: "${error.message}".`);
      }
    });
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest().subscribe(() => {
      this.loadScanJobs(this.page?.pageIndex ?? 0, this.page?.pageSize ?? 5);
    });
    this.libraryScanService.updateScanJobProgress().subscribe();
    this.scanJobsLoadingState = LoadingState.LOADING;
    this.loadScanJobs();
  }

  ngOnDestroy(): void {
    this.scanJobProgressSubscription?.unsubscribe();
    this.refreshRequestSubscription?.unsubscribe();
  }

  private loadScanJobs(pageIndex = 0, pageSize = 5) {
    this.libraryScanService.getScanJobs(pageIndex, pageSize).subscribe({
      next: scanJobPage => {
        this.scanJobs = scanJobPage.scanJobs;
        this.page = scanJobPage;
        this.emptyScanJobRowCount = Math.max(0, 5 - this.scanJobs.length);
        this.scanJobsLoadingState = this.scanJobs.length > 0 ? LoadingState.LOADED : LoadingState.EMPTY;
      },
      error: () => {
        this.scanJobsLoadingState = LoadingState.ERROR;
      }
    });
  }

  startScanJob() {
    this.scanJobProgressLoadingState = LoadingState.LOADING;
    this.libraryScanService.startScanJob().subscribe({
      next: () => {
        this.scanJobProgressLoadingState = LoadingState.LOADED;
      },
      error: () => {
        this.scanJobProgressLoadingState = LoadingState.ERROR;
      }
    });
  }

  loadScanJobsPreviousPage() {
    if (this.page && this.page.pageIndex > 0) {
      this.loadScanJobs(this.page.pageIndex - 1);
    }
  }

  loadScanJobsNextPage() {
    if (this.page && this.page.pageIndex < (this.page.totalPages - 1)) {
      this.loadScanJobs(this.page.pageIndex + 1);
    }
  }
}
