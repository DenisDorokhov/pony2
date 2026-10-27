import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {AlbumDto, ArtistDto, DiscoveryJobDetailsDto, DiscoveryJobDto, DiscoveryType, GenreDto, LogMessageDto, SongDto} from '../../../domain/library.dto';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Subscription} from 'rxjs';
import {LibraryDiscoveryService} from '../../../service/library-discovery.service';
import {LoadingState} from '../../../domain/common.model';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {JobDiscoveryTasksComponent} from './job-discovery-tasks.component';
import {Album, Artist, Genre} from '../../../domain/library.model';
import {LibraryService} from '../../../service/library.service';
import {UnknownArtistPipe} from '../../../pipe/unknown-artist.pipe';
import {UnknownAlbumPipe} from '../../../pipe/unknown-album.pipe';
import {UnknownGenrePipe} from '../../../pipe/unknown-genre.pipe';
import {UnknownSongPipe} from '../../../pipe/unknown-song.pipe';

@Component({
    imports: [TranslateModule, CommonModule, LoadingIndicatorComponent, ErrorIndicatorComponent,
      UnknownArtistPipe, UnknownAlbumPipe, UnknownGenrePipe, UnknownSongPipe],
    selector: 'pony-discovery-job',
    templateUrl: './discovery-job.component.html',
    styleUrls: ['./discovery-job.component.scss']
})
export class DiscoveryJobComponent implements OnInit, OnDestroy {

  readonly activeModal = inject(NgbActiveModal);

  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly libraryService = inject(LibraryService);
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
    this.refreshSubscription = this.libraryService.observeRefreshRequest()
      .subscribe(() => this.loadDiscoveryJob());
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

  goToGenre(genreDto: GenreDto) {
    this.libraryService.requestFilterByGenre(new Genre(genreDto));
    this.modal.dismissAll();
  }

  goToSong(songDto: SongDto) {
    this.libraryService.getSongs([songDto.id]).subscribe(songs => {
      if (songs.length > 0) {
        this.libraryService.requestScrollToSong(songs[0]);
        this.modal.dismissAll();
      }
    });
  }

  showTasks() {
    const modalRef = this.modal.open(JobDiscoveryTasksComponent, {size: 'xl'});
    (modalRef.componentInstance as JobDiscoveryTasksComponent).jobId = this.discoveryJobId;
  }
}
