import {Component, ElementRef, inject, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NgbActiveModal, NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {interval, Subscription} from 'rxjs';
import {map, mergeMap} from 'rxjs/operators';
import {AlbumDto, ArtistDto, DiscoveryJobDto, DiscoveryTaskDto, DiscoveryTaskResultFilter, DiscoveryTaskSummaryDto, DiscoveryTaskType, GenreDto, SongDto} from '../../../domain/library.dto';
import {Album, Artist} from '../../../domain/library.model';
import {PageDto} from '../../../domain/common.dto';
import {LoadingState} from '../../../domain/common.model';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LibraryService} from '../../../service/library.service';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {formatCompactDuration} from '../../../utils/format.utils';
import {DiscoveryTaskComponent} from './discovery-task.component';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';
import {UnknownAlbumPipe} from '../../../pipe/unknown-album.pipe';

class DiscoveryTaskWithTarget implements DiscoveryTaskSummaryDto {

  id!: string;
  creationDate!: string;
  updateDate: string | undefined;
  status!: DiscoveryTaskDto.Status;
  type!: DiscoveryTaskType;
  duration: number | undefined;
  genre: GenreDto | undefined;
  artist: ArtistDto | undefined;
  album: AlbumDto | undefined;
  song: SongDto | undefined;
  target!: string;
  durationText!: string;

  constructor(task: DiscoveryTaskSummaryDto, translateService: TranslateService) {
    this.id = task.id;
    this.creationDate = task.creationDate;
    this.updateDate = task.updateDate;
    this.status = task.status;
    this.type = task.type;
    this.duration = task.duration;
    this.genre = task.genre;
    this.artist = task.artist;
    this.album = task.album;
    this.song = task.song;
    this.target = this.formatTarget(translateService);
    this.durationText = task.duration !== undefined ? formatCompactDuration(task.duration, translateService) : '—';
  }

  private formatTarget(translateService: TranslateService): string {
    if (this.type === DiscoveryTaskType.MUSICBRAINZ_ALBUM_DATA) {
      if (!this.album) {
        return translateService.instant('discoveryTasks.deletedAlbum');
      }
      const artist = this.artist?.name || translateService.instant('library.artist.unknownLabel');
      const album = this.album.name || translateService.instant('library.album.unknownLabel');
      return artist + ' - ' + album + (this.album.year ? ' (' + this.album.year + ')' : '');
    }
    if (!this.artist) {
      return translateService.instant('discoveryTasks.deletedArtist');
    }
    return this.artist.name || translateService.instant('library.artist.unknownLabel');
  }
}

@Component({
  imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent,
    NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownItem, UnknownArtistPipe, UnknownAlbumPipe],
  selector: 'pony-discovery-tasks',
  templateUrl: './discovery-tasks.component.html',
  styleUrls: ['./discovery-tasks.component.scss']
})
export class DiscoveryTasksComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
  private readonly translateService = inject(TranslateService);
  private readonly modal = inject(NgbModal);

  readonly LoadingState = LoadingState;
  readonly TaskStatus = DiscoveryTaskDto.Status;
  readonly TaskResultFilter = DiscoveryTaskResultFilter;
  readonly taskTypes = Object.values(DiscoveryTaskType);

  @Input()
  discoveryJobId!: string;

  @ViewChild('scroller')
  containerElement!: ElementRef;

  loadingState = LoadingState.LOADING;
  taskResult: DiscoveryTaskResultFilter | undefined;
  taskType: DiscoveryTaskType | undefined;
  discoveryTasks: DiscoveryTaskWithTarget[] = [];
  page: PageDto | undefined;

  private pageToLoad: PageDto | undefined;
  private resetScrollOnLoad = false;
  private tasksSubscription: Subscription | undefined;
  private refreshSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.refreshSubscription = interval(15000).subscribe(() =>
      this.loadPage(this.pageToLoad?.pageIndex ?? 0, this.pageToLoad?.pageSize ?? 30, false));
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.tasksSubscription?.unsubscribe();
    this.refreshSubscription?.unsubscribe();
  }

  private loadPage(pageIndex = 0, pageSize = 30, resetScroll = true) {
    this.pageToLoad = {pageIndex, pageSize, totalPages: this.page?.totalPages ?? 0};
    this.resetScrollOnLoad = this.resetScrollOnLoad || resetScroll;
    if (resetScroll) {
      this.loadingState = LoadingState.LOADING;
    }
    this.tasksSubscription?.unsubscribe();
    this.tasksSubscription = this.libraryDiscoveryService.getDiscoveryJob(this.discoveryJobId).pipe(
      mergeMap(discoveryJob => this.libraryDiscoveryService.getDiscoveryTasks(this.discoveryJobId, this.taskResult, this.taskType, pageIndex, pageSize).pipe(
        map(page => ({discoveryJob, page}))
      ))
    ).subscribe({
      next: ({discoveryJob, page}) => {
        this.discoveryTasks = page.discoveryTasks.map(task => new DiscoveryTaskWithTarget(task, this.translateService));
        this.page = this.pageToLoad = page;
        if (this.resetScrollOnLoad) {
          this.containerElement.nativeElement.scrollTop = 0;
          this.resetScrollOnLoad = false;
        }
        this.loadingState = this.discoveryTasks.length > 0 ? LoadingState.LOADED : LoadingState.EMPTY;
        if (discoveryJob.status !== DiscoveryJobDto.Status.STARTING && discoveryJob.status !== DiscoveryJobDto.Status.STARTED) {
          this.refreshSubscription?.unsubscribe();
        }
      },
      error: () => this.loadingState = LoadingState.ERROR
    });
  }

  applyResultFilter(result?: DiscoveryTaskResultFilter) {
    this.taskResult = result;
    this.loadPage(0, this.pageToLoad?.pageSize ?? 30);
  }

  applyTypeFilter(type?: DiscoveryTaskType) {
    this.taskType = type;
    this.loadPage(0, this.pageToLoad?.pageSize ?? 30);
  }

  loadPreviousPage() {
    if (this.page && this.page.pageIndex > 0) {
      this.loadPage(this.page.pageIndex - 1, this.page.pageSize);
    }
  }

  loadNextPage() {
    if (this.page && this.page.pageIndex < this.page.totalPages - 1) {
      this.loadPage(this.page.pageIndex + 1, this.page.pageSize);
    }
  }

  goToArtist(artistDto: ArtistDto) {
    const artist = new Artist(artistDto);
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.requestScrollToArtist(artist);
    this.modal.dismissAll();
  }

  goToAlbum(albumDto: AlbumDto, artistDto: ArtistDto) {
    const album = new Album(albumDto, new Artist(artistDto));
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.requestScrollToAlbum(album);
    this.modal.dismissAll();
  }

  showDetails(task: DiscoveryTaskSummaryDto) {
    const modalRef = this.modal.open(DiscoveryTaskComponent, {size: 'lg'});
    const component = modalRef.componentInstance as DiscoveryTaskComponent;
    component.discoveryTaskId = task.id;
    component.taskSummary = task;
  }
}
