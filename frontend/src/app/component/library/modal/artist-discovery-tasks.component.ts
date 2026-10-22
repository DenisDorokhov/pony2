import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs';
import {DiscoveryJobProgressDto, DiscoveryTaskPageDto} from '../../../domain/library.dto';
import {AbstractDiscoveryTasksComponent, DISCOVERY_TASKS_IMPORTS} from './abstract-discovery-tasks.component';

@Component({
  imports: DISCOVERY_TASKS_IMPORTS,
  selector: 'pony-artist-discovery-tasks',
  templateUrl: './discovery-tasks.component.html',
  styleUrls: ['./discovery-tasks.component.scss']
})
export class ArtistDiscoveryTasksComponent extends AbstractDiscoveryTasksComponent {

  override readonly titleKey = 'discoveryTasks.artistHeader';

  @Input()
  artistId!: string;

  protected override get hasDiscoveryTarget(): boolean {
    return !!this.artistId;
  }

  protected override loadTasks(pageIndex: number, pageSize: number): Observable<DiscoveryTaskPageDto> {
    return this.libraryDiscoveryService.getArtistDiscoveryTasks(this.artistId, this.taskResult, this.taskType, pageIndex, pageSize);
  }

  protected override startDiscoveryJob(): Observable<DiscoveryJobProgressDto | null> {
    return this.libraryDiscoveryService.startArtistDiscoveryJob(this.artistId);
  }
}
