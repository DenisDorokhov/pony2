import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {CommonModule} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal, NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {PlaybackEvent, PlaybackService, PlaybackState} from "../../../service/playback.service";
import {Subscription} from "rxjs";
import {Song} from "../../../domain/library.model";
import {UnknownArtistPipe} from "../../../pipe/unknown-artist.pipe";
import {UnknownSongPipe} from "../../../pipe/unknown-song.pipe";
import {ImageLoaderComponent} from "../../common/image-loader.component";
import {LibraryService} from "../../../service/library.service";
import {LoadingState} from "../../../domain/common.model";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";
import {UnknownAlbumPipe} from "../../../pipe/unknown-album.pipe";
import {CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPreview, CdkDropList} from "@angular/cdk/drag-drop";
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from "@angular/cdk/scrolling";

@Component({
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    UnknownArtistPipe,
    UnknownSongPipe,
    ImageLoaderComponent,
    NoContentIndicatorComponent,
    UnknownAlbumPipe,
    NgbDropdownModule,
    CdkDropList,
    CdkDrag,
    CdkDragHandle,
    CdkDragPreview,
    CdkVirtualScrollViewport,
    CdkFixedSizeVirtualScroll,
    CdkVirtualForOf,
  ],
  selector: 'pony-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent implements OnInit, OnDestroy, AfterViewInit {

  protected readonly PlaybackState = PlaybackState;
  protected readonly LoadingState = LoadingState;

  queue: Song[] = [];
  playbackEvent: PlaybackEvent | undefined;

  @ViewChild(CdkVirtualScrollViewport) viewPort!: CdkVirtualScrollViewport;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playbackService: PlaybackService,
    private readonly libraryService: LibraryService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(this.playbackService.observeQueue()
      .subscribe(queue => this.queue = queue));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.playbackEvent = playbackEvent));
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() => {
      if (this.playbackService.currentSongIndex >= 0) {
        this.viewPort.scrollToOffset(this.playbackService.currentSongIndex * 76 - 230 + 38 + 16);
      }
    });
  }

  playSongOnDoubleClick(event: MouseEvent, index: number) {
    let checkElement: Node | null = event.target as Node;
    let isButtonClick = false;
    do {
      isButtonClick = checkElement.nodeName === 'BUTTON';
      checkElement = (checkElement as Node).parentNode;
    } while (!isButtonClick && checkElement);
    if (!isButtonClick) {
      this.playbackService.play(index);
    }
  }

  preventDoubleClickDefault(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  goToSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.startScrollToSong(song);
    this.activeModal.close();
  }

  removeSong(index: number) {
    this.playbackService.removeSongFromQueue(index);
  }

  dropRow(event: CdkDragDrop<any, any>) {
    this.playbackService.moveSongInQueue(event.previousIndex, event.currentIndex);
  }
}
