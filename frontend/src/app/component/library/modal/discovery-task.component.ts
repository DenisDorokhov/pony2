import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Subscription} from 'rxjs';
import {Highlight} from 'ngx-highlightjs';
import FileSaver from 'file-saver';
import {AlbumDto, ArtistDto, DiscoveryTaskDto, DiscoveryTaskSummaryDto, DiscoveryTaskType} from '../../../domain/library.dto';
import {Album, Artist} from '../../../domain/library.model';
import {LoadingState} from '../../../domain/common.model';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LibraryService} from '../../../service/library.service';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {JsonDetailsComponent} from '../../common/json-details.component';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';
import {UnknownAlbumPipe} from '../../../pipe/unknown-album.pipe';
import {formatFileSize} from '../../../utils/format.utils';

const JSON_PREVIEW_LENGTH = 1000;
const JSON_DETAILS_MAX_LENGTH = 20_000;

@Component({
  imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent, Highlight,
    UnknownArtistPipe, UnknownAlbumPipe],
  selector: 'pony-discovery-task',
  templateUrl: './discovery-task.component.html',
  styleUrls: ['./discovery-task.component.scss']
})
export class DiscoveryTaskComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
  private readonly modal = inject(NgbModal);

  readonly LoadingState = LoadingState;
  readonly TaskStatus = DiscoveryTaskDto.Status;
  readonly TaskType = DiscoveryTaskType;

  @Input()
  discoveryTaskId!: string;

  @Input()
  taskSummary: DiscoveryTaskSummaryDto | undefined;

  discoveryTask: DiscoveryTaskDto | undefined;
  loadingState = LoadingState.LOADING;

  resultPreview = '—';
  resultSize = '';
  rawRequestsPreview = '—';
  rawRequestsSize = '';
  rawResponsesPreview = '—';
  rawResponsesSize = '';

  private taskSubscription: Subscription | undefined;
  private refreshSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.refreshSubscription = this.libraryService.observeRefreshRequest()
      .subscribe(() => this.loadTask());
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
        this.resultPreview = this.createJsonPreview(task.result);
        this.resultSize = this.formatJsonSize(task.result);
        this.rawRequestsPreview = this.createJsonPreview(task.rawRequests);
        this.rawRequestsSize = this.formatJsonSize(task.rawRequests);
        this.rawResponsesPreview = this.createJsonPreview(task.rawResponses);
        this.rawResponsesSize = this.formatJsonSize(task.rawResponses);
        this.loadingState = LoadingState.LOADED;
        if (task.status !== DiscoveryTaskDto.Status.STARTED) {
          this.refreshSubscription?.unsubscribe();
        }
      },
      error: () => this.loadingState = LoadingState.ERROR
    });
  }

  private createJsonPreview(value: string | undefined): string {
    if (value == null) {
      return '—';
    }
    return value.length > JSON_PREVIEW_LENGTH ? value.slice(0, JSON_PREVIEW_LENGTH) + '…' : value;
  }

  private formatJsonSize(value: string | undefined): string {
    return formatFileSize(new Blob([value ?? '']).size);
  }

  goToArtist(artistDto: ArtistDto): void {
    const artist = new Artist(artistDto);
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.requestScrollToArtist(artist);
    this.modal.dismissAll();
  }

  goToAlbum(albumDto: AlbumDto, artistDto: ArtistDto): void {
    const album = new Album(albumDto, new Artist(artistDto));
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.requestScrollToAlbum(album);
    this.modal.dismissAll();
  }

  showDetails(value: string | undefined, title: string, field: string): void {
    if (value == null) {
      return;
    }
    if (value.length > JSON_DETAILS_MAX_LENGTH) {
      const file = new File([value], `discovery-task-${this.discoveryTaskId}-${field}.json`, {
        type: 'application/json;charset=utf-8'
      });
      FileSaver.saveAs(file);
      return;
    }
    const modalRef = this.modal.open(JsonDetailsComponent, {size: 'xl', scrollable: true});
    const details = modalRef.componentInstance as JsonDetailsComponent;
    details.title = title;
    details.value = value;
  }
}
