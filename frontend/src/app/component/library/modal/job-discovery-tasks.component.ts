import {Component, Input} from '@angular/core';
import {EMPTY, Observable} from 'rxjs';
import {mergeMap, tap} from 'rxjs/operators';
import {DiscoveryJobDto, DiscoveryStatusDto, DiscoveryTaskPageDto, DiscoveryType} from '../../../domain/library.dto';
import {AbstractDiscoveryTasksComponent, DISCOVERY_TASKS_IMPORTS} from './abstract-discovery-tasks.component';

@Component({
  imports: DISCOVERY_TASKS_IMPORTS,
  selector: 'pony-job-discovery-tasks',
  templateUrl: './discovery-tasks.component.html',
  styleUrls: ['./discovery-tasks.component.scss']
})
export class JobDiscoveryTasksComponent extends AbstractDiscoveryTasksComponent {

  override readonly titleKey = 'discoveryTasks.jobHeader';
  protected override readonly refreshOnDiscoveryEnd = false;

  @Input()
  jobId!: string;

  private discoveryJob: DiscoveryJobDto | undefined;

  override get startDiscoveryDisabled(): boolean {
    return super.startDiscoveryDisabled || this.shouldRefreshTasks;
  }

  protected override loadTasks(pageIndex: number, pageSize: number): Observable<DiscoveryTaskPageDto> {
    return this.libraryDiscoveryService.getDiscoveryJob(this.jobId).pipe(
      mergeMap(job => this.libraryDiscoveryService.getDiscoveryTasks(this.jobId, this.taskResult, this.taskType, pageIndex, pageSize).pipe(
        tap(() => this.discoveryJob = job)
      ))
    );
  }

  protected override get shouldRefreshTasks(): boolean {
    return !this.discoveryJob || this.discoveryJob.status === DiscoveryJobDto.Status.STARTING ||
      this.discoveryJob.status === DiscoveryJobDto.Status.STARTED;
  }

  protected override get hasDiscoveryTarget(): boolean {
    switch (this.discoveryJob?.discoveryType) {
      case DiscoveryType.FULL:
        return true;
      case DiscoveryType.ARTIST:
        return !!this.discoveryJob.artist;
      case DiscoveryType.ALBUM:
        return !!this.discoveryJob.album;
      default:
        return false;
    }
  }

  protected override startDiscoveryJob(): Observable<DiscoveryStatusDto> {
    switch (this.discoveryJob?.discoveryType) {
      case DiscoveryType.FULL:
        return this.libraryDiscoveryService.startFullDiscoveryJob();
      case DiscoveryType.ARTIST:
        return this.libraryDiscoveryService.startArtistDiscoveryJob(this.discoveryJob.artist!.id);
      case DiscoveryType.ALBUM:
        return this.libraryDiscoveryService.startAlbumDiscoveryJob(this.discoveryJob.album!.id);
      default:
        return EMPTY;
    }
  }
}
