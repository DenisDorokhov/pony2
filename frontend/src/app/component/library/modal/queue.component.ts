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
import {CdkDrag, CdkDragDrop, CdkDragHandle, CdkDragPreview, CdkDragStart, CdkDropList} from "@angular/cdk/drag-drop";
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {ReactiveFormsModule} from "@angular/forms";
import {PlaylistMode} from "../../../domain/playlist.model";

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
    ReactiveFormsModule,
  ],
  selector: 'pony-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent implements OnInit, OnDestroy, AfterViewInit {

  protected readonly PlaybackState = PlaybackState;
  protected readonly LoadingState = LoadingState;

  protected readonly rowHeight = 76;
  protected readonly viewPortHeight = 600;
  protected readonly viewPortPadding = 16;

  readonly playlistModes: PlaylistMode[] = [PlaylistMode.NORMAL, PlaylistMode.RANDOM, PlaylistMode.REPEAT_ALL, PlaylistMode.REPEAT_ONE];

  queue: Song[] = [];
  playbackEvent: PlaybackEvent | undefined;
  currentSongIndex = -1;
  currentSongShown = false;
  playlistMode: PlaylistMode = PlaylistMode.NORMAL;

  @ViewChild(CdkVirtualScrollViewport) viewPort!: CdkVirtualScrollViewport;
  @ViewChildren('songElements') linkElements!: QueryList<ElementRef>;

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
    this.playlistMode = this.playbackService.playlistMode;
    this.subscriptions.push(this.playbackService.observeQueue()
      .subscribe(queue => this.queue = queue));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.playbackEvent = playbackEvent));
    this.subscriptions.push(this.playbackService.observeCurrentSong()
      .subscribe(_ => this.currentSongIndex = this.playbackService.currentSongIndex));
  }

  ngAfterViewInit(): void {
    this.subscriptions.push(this.viewPort.renderedRangeStream.subscribe(() =>
      requestAnimationFrame(() => this.checkIfCurrentSongShown())));
    requestAnimationFrame(() => this.scrollToCurrentSong());
  }

  scrollToCurrentSong() {
    if (this.playbackService.currentSongIndex >= 0) {
      this.viewPort.scrollToOffset(this.playbackService.currentSongIndex * this.rowHeight - (this.viewPortHeight / 2) + (this.rowHeight / 2) + this.viewPortPadding);
    }
  }

  private checkIfCurrentSongShown() {
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
    this.playbackService.removeSongFromQueue(index);
    this.currentSongIndex = this.playbackService.currentSongIndex
  }

  onDropListDropped(event: CdkDragDrop<any, any>) {
    const toIndex = this.dragFromIndex! - event.previousIndex + event.currentIndex;
    this.playbackService.moveSongInQueue(this.dragFromIndex!, toIndex);
    this.currentSongIndex = this.playbackService.currentSongIndex
    this.dragFromIndex = undefined;
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

  applyPlaylistMode(event: Event) {
    this.playbackService.playlistMode = (event.target as any).value;
  }
}
