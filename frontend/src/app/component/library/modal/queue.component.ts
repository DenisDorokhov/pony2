import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren
} from "@angular/core";
import {CommonModule} from "@angular/common";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {NgbActiveModal, NgbDropdown, NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {PlaybackService} from "../../../service/playback.service";
import {fromEvent, Subscription} from "rxjs";
import {Song} from "../../../domain/library.model";
import {UnknownArtistPipe} from "../../../pipe/unknown-artist.pipe";
import {UnknownSongPipe} from "../../../pipe/unknown-song.pipe";
import {ImageLoaderComponent} from "../../common/image-loader.component";
import {LibraryService} from "../../../service/library.service";
import {LoadingState} from "../../../domain/common.model";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";
import {UnknownAlbumPipe} from "../../../pipe/unknown-album.pipe";
import {CdkDrag, CdkDragDrop, CdkDragStart, CdkDropList} from "@angular/cdk/drag-drop";
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {formatDuration} from "../../../utils/format.utils";
import {PlaybackEvent, PlaybackState} from "../../../service/audio-player.service";
import {isMobileBrowser} from "../../../utils/mobile.utils";
import {UnknownGenrePipe} from "../../../pipe/unknown-genre.pipe";

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
    CdkVirtualScrollViewport,
    CdkFixedSizeVirtualScroll,
    CdkVirtualForOf,
    UnknownGenrePipe,
  ],
  selector: 'pony-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent implements OnInit, OnDestroy, AfterViewInit {

  protected readonly PlaybackState = PlaybackState;
  protected readonly LoadingState = LoadingState;

  protected readonly rowHeight = 76;
  protected readonly viewPortHeight = 532;
  protected readonly viewPortPadding = 16;

  queue: Song[] = [];
  lastPlaybackEvent: PlaybackEvent | undefined;
  currentSongIndex = -1;
  currentSongShown = false;
  selectedIndex = -1;
  duration: string | undefined;
  mouseOverIndex: number | undefined;
  dragEnabled = true;

  @ViewChild(CdkVirtualScrollViewport) viewPort!: CdkVirtualScrollViewport;
  @ViewChildren('songElements') linkElements!: QueryList<ElementRef>;
  @ViewChildren(NgbDropdown) optionsDropDowns!: QueryList<NgbDropdown>;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playbackService: PlaybackService,
    private readonly libraryService: LibraryService,
    private readonly translateService: TranslateService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.dragEnabled = !isMobileBrowser();
    this.subscriptions.push(this.playbackService.observeQueue()
      .subscribe(queue => {
        this.queue = queue;
        this.duration = formatDuration(queue.reduce((result: number, song: Song) => result + song.duration, 0), this.translateService);
        if (this.viewPort) {
          this.viewPort.checkViewportSize();
        }
      }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.lastPlaybackEvent = playbackEvent));
    this.subscriptions.push(this.playbackService.observeCurrentSong()
      .subscribe(_ => {
        this.currentSongIndex = this.playbackService.currentSongIndex;
        this.checkIfCurrentSongShown();
      }));
  }

  ngAfterViewInit(): void {
    this.subscriptions.push(fromEvent(window.document.body, 'mousewheel').subscribe(() =>
      this.closeOptionsDropDowns()));
    this.subscriptions.push(fromEvent(window.document.body, 'touchstart').subscribe(() =>
      this.closeOptionsDropDowns()));
    this.subscriptions.push(this.viewPort.renderedRangeStream.subscribe(() =>
      requestAnimationFrame(() => this.checkIfCurrentSongShown())));
    requestAnimationFrame(() => this.scrollToCurrentSong());
  }

  private closeOptionsDropDowns() {
    this.optionsDropDowns.toArray().forEach(dropDown => {
      dropDown.close();
    });
  }

  scrollToCurrentSong() {
    if (this.playbackService.currentSongIndex >= 0) {
      this.selectedIndex = this.playbackService.currentSongIndex;
      const offset = this.playbackService.currentSongIndex * this.rowHeight - (this.viewPortHeight / 2) + (this.rowHeight / 2) + this.viewPortPadding;
      this.viewPort.scrollToOffset(offset);
      // Workaround for CdkVirtualScroll bug (content total height is calculated incorrectly).
      setTimeout(() => this.viewPort.scrollToOffset(offset));
    }
  }

  private checkIfCurrentSongShown() {
    if (!this.linkElements) {
      // View is not initialized yet.
      return;
    }
    const currentSongElementIndex = this.linkElements.toArray().findIndex(next => this.resolveDragItemIndex(next.nativeElement.id) === this.playbackService.currentSongIndex);
    if (currentSongElementIndex >= 0) {
      const currentSongElement = this.linkElements.toArray()[currentSongElementIndex];
      this.currentSongShown = this.songElementIntersectsViewPort(currentSongElement);
    }
  }

  private songElementIntersectsViewPort(songElementRef: ElementRef): boolean {
    const viewPortRect = this.viewPort.elementRef.nativeElement.getBoundingClientRect();
    const currentSongRect = songElementRef.nativeElement.getBoundingClientRect();
    return (currentSongRect.y > viewPortRect.y && currentSongRect.y < viewPortRect.y + viewPortRect.height) ||
      (currentSongRect.y + currentSongRect.height > viewPortRect.y && currentSongRect.y < viewPortRect.y + viewPortRect.height);
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
    this.playbackService.removeFromQueue(index);
    this.currentSongIndex = this.playbackService.currentSongIndex;
    if (this.selectedIndex === index) {
      this.selectedIndex = -1;
    }
  }

  onDropListDropped(event: CdkDragDrop<any, any>) {
    const toIndex = this.dragFromIndex! - event.previousIndex + event.currentIndex;
    this.playbackService.moveInQueue(this.dragFromIndex!, toIndex);
    this.currentSongIndex = this.playbackService.currentSongIndex;
    this.dragFromIndex = undefined;
    this.selectedIndex = toIndex;
    requestAnimationFrame(() => this.checkIfCurrentSongShown());
  }

  private dragFromIndex: number | undefined;

  onDragStarted(event: CdkDragStart) {
    this.dragFromIndex = this.resolveDragItemIndex(event.source.element.nativeElement.id);
  }

  private resolveDragItemIndex(id: string): number {
    return Number(id.replaceAll('queueSong_', ''));
  }

  onWheel(event: WheelEvent) {
    if (this.dragFromIndex !== undefined) {
      event.preventDefault();
    }
  }

  onTouchmove(event: TouchEvent) {
    if (this.dragFromIndex !== undefined) {
      event.preventDefault();
    }
  }

  onScroll() {
    this.checkIfCurrentSongShown();
  }

  selectIndex(i: number) {
    this.selectedIndex = i;
  }

  onMouseMove(i: number) {
    this.mouseOverIndex = i;
  }

  onMouseLeave(i: number) {
    if (this.mouseOverIndex === i) {
      this.mouseOverIndex = undefined;
    }
  }

  onPlaybackClick(i: number) {
    if (this.currentSongIndex === i) {
      this.playbackService.playOrPause();
    } else {
      this.playbackService.play(i);
    }
  }
}
