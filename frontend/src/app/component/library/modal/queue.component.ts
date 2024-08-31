import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren} from "@angular/core";
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
import {ScrollingUtils} from "../../../utils/scrolling.utils";
import {CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList} from "@angular/cdk/drag-drop";
import scrollIntoElement = ScrollingUtils.scrollIntoElement;

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

  @ViewChildren('rows') rowElements!: QueryList<ElementRef>;

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
    if (this.playbackEvent?.song) {
      const index = this.queue.findIndex(song => song.id === this.playbackEvent?.song?.id);
      if (index >= 0) {
        const selectedElement = this.rowElements.toArray()[index];
        scrollIntoElement(selectedElement.nativeElement);
      }
    }
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
