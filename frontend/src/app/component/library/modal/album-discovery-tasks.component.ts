import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs';
import {DiscoveryStatusDto, DiscoveryTaskPageDto} from '../../../domain/library.dto';
import {AbstractDiscoveryTasksComponent, DISCOVERY_TASKS_IMPORTS} from './abstract-discovery-tasks.component';
import {AllDiscoveryTasksComponent} from './all-discovery-tasks.component';

@Component({
  imports: DISCOVERY_TASKS_IMPORTS,
  selector: 'pony-album-discovery-tasks',
  templateUrl: './discovery-tasks.component.html',
  styleUrls: ['./discovery-tasks.component.scss']
})
export class AlbumDiscoveryTasksComponent extends AbstractDiscoveryTasksComponent {

  override readonly titleKey = 'discoveryTasks.albumHeader';
  override readonly allTasksComponent = AllDiscoveryTasksComponent;

  @Input()
  albumId!: string;

  protected override get hasDiscoveryTarget(): boolean {
    return !!this.albumId;
  }

  protected override loadTasks(pageIndex: number, pageSize: number): Observable<DiscoveryTaskPageDto> {
    return this.libraryDiscoveryService.getAlbumDiscoveryTasks(this.albumId, this.taskResult, this.taskType, pageIndex, pageSize);
  }

  protected override startDiscoveryJob(): Observable<DiscoveryStatusDto> {
    return this.libraryDiscoveryService.startAlbumDiscoveryJob(this.albumId);
  }
}
