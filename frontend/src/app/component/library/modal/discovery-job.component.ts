import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {DiscoveryJobDetailsDto, DiscoveryJobDto, DiscoveryType, LogMessageDto} from '../../../domain/library.dto';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {interval, Subscription} from 'rxjs';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LoadingState} from '../../../domain/common.model';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {DiscoveryTasksComponent} from './discovery-tasks.component';

@Component({
    imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent],
    selector: 'pony-discovery-job',
    templateUrl: './discovery-job.component.html',
    styleUrls: ['./discovery-job.component.scss']
})
export class DiscoveryJobComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly modal = inject(NgbModal);

  DiscoveryJobStatus = DiscoveryJobDto.Status;
  DiscoveryType = DiscoveryType;
  LogMessageLevel = LogMessageDto.Level;
  LoadingState = LoadingState;

  @Input()
  discoveryJobId!: string;

  discoveryJobDetails: DiscoveryJobDetailsDto | undefined;
  exception: string | undefined;
  loadingState = LoadingState.LOADING;

  private discoveryJobSubscription: Subscription | undefined;
  private refreshSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.refreshSubscription = interval(15000).subscribe(() => this.loadDiscoveryJob());
    this.loadDiscoveryJob();
  }

  ngOnDestroy(): void {
    this.discoveryJobSubscription?.unsubscribe();
    this.refreshSubscription?.unsubscribe();
  }

  private loadDiscoveryJob() {
    this.discoveryJobSubscription?.unsubscribe();
    this.discoveryJobSubscription = this.libraryDiscoveryService.getDiscoveryJobDetails(this.discoveryJobId).subscribe({
      next: details => {
        this.discoveryJobDetails = details;
        this.exception = LogMessageDto.extractException(details.discoveryJob.logMessage);
        this.loadingState = LoadingState.LOADED;
        if (details.discoveryJob.status !== DiscoveryJobDto.Status.STARTING && details.discoveryJob.status !== DiscoveryJobDto.Status.STARTED) {
          this.refreshSubscription?.unsubscribe();
        }
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  showTasks() {
    const modalRef = this.modal.open(DiscoveryTasksComponent, {size: 'xl'});
    (modalRef.componentInstance as DiscoveryTasksComponent).discoveryJobId = this.discoveryJobId;
  }
}
