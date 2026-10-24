import {Component} from '@angular/core';
import {Observable} from 'rxjs';
import {DiscoveryJobProgressDto, DiscoveryTaskPageDto} from '../../../domain/library.dto';
import {AbstractDiscoveryTasksComponent, DISCOVERY_TASKS_IMPORTS} from './abstract-discovery-tasks.component';

@Component({
  imports: DISCOVERY_TASKS_IMPORTS,
  selector: 'pony-all-discovery-tasks',
  templateUrl: './discovery-tasks.component.html',
  styleUrls: ['./discovery-tasks.component.scss']
})
export class AllDiscoveryTasksComponent extends AbstractDiscoveryTasksComponent {

  override readonly titleKey = 'discoveryTasks.allHeader';

  protected override get hasDiscoveryTarget(): boolean {
    return true;
  }

  protected override loadTasks(pageIndex: number, pageSize: number): Observable<DiscoveryTaskPageDto> {
    return this.libraryDiscoveryService.getAllDiscoveryTasks(this.taskResult, this.taskType, pageIndex, pageSize);
  }

  protected override startDiscoveryJob(): Observable<DiscoveryJobProgressDto | null> {
    return this.libraryDiscoveryService.startFullDiscoveryJob();
  }
}
