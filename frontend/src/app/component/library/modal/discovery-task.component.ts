import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {interval, Subscription} from 'rxjs';
import {DiscoveryTaskDto} from '../../../domain/library.dto';
import {LoadingState} from '../../../domain/common.model';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';

@Component({
  imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent],
  selector: 'pony-discovery-task',
  templateUrl: './discovery-task.component.html',
  styleUrls: ['./discovery-task.component.scss']
})
export class DiscoveryTaskComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);

  readonly LoadingState = LoadingState;
  readonly TaskStatus = DiscoveryTaskDto.Status;

  @Input()
  discoveryTaskId!: string;

  discoveryTask: DiscoveryTaskDto | undefined;
  loadingState = LoadingState.LOADING;

  private taskSubscription: Subscription | undefined;
  private refreshSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.refreshSubscription = interval(15000).subscribe(() => this.loadTask());
    this.loadTask();
  }

  ngOnDestroy(): void {
    this.taskSubscription?.unsubscribe();
    this.refreshSubscription?.unsubscribe();
  }

  private loadTask() {
    this.taskSubscription?.unsubscribe();
    this.taskSubscription = this.libraryDiscoveryService.getDiscoveryTask(this.discoveryTaskId).subscribe({
      next: task => {
        this.discoveryTask = task;
        this.loadingState = LoadingState.LOADED;
        if (task.status !== DiscoveryTaskDto.Status.STARTED) {
          this.refreshSubscription?.unsubscribe();
        }
      },
      error: () => this.loadingState = LoadingState.ERROR
    });
  }
}
