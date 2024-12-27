import {Component, OnInit} from "@angular/core";
import {CommonModule} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal, NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {PlaybackHistory, Song} from "../../../domain/library.model";
import {LoadingState} from "../../../domain/common.model";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {PlaybackHistoryService} from "../../../service/playback-history.service";
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {LargeSongComponent} from "./common/large-song.component";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";
import {LibraryService} from "../../../service/library.service";

@Component({
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    NgbDropdownModule,
    ErrorIndicatorComponent,
    LoadingIndicatorComponent,
    CdkFixedSizeVirtualScroll,
    CdkVirtualScrollViewport,
    NoContentIndicatorComponent,
    CdkVirtualForOf,
    LargeSongComponent,

  ],
  selector: 'pony-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {

  readonly LoadingState = LoadingState;

  readonly rowHeight = LargeSongComponent.HEIGHT;

  loadingState: LoadingState = LoadingState.LOADING;
  playbackHistory: PlaybackHistory | undefined;
  selectedIndex = -1;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playbackHistoryService: PlaybackHistoryService,
    private readonly libraryService: LibraryService,
  ) {
  }

  ngOnInit(): void {
    this.playbackHistoryService.getHistory().subscribe({
      next: playbackHistory => {
        this.loadingState = LoadingState.LOADED;
        this.playbackHistory = playbackHistory;
      },
      error: error => {
        console.error(JSON.stringify(error));
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  onSongDoubleClick(event: MouseEvent, song: Song) {
    let checkElement: Node | null = event.target as Node;
    let isButtonClick = false;
    do {
      isButtonClick = checkElement.nodeName === 'BUTTON';
      checkElement = (checkElement as Node).parentNode;
    } while (!isButtonClick && checkElement);
    if (!isButtonClick) {
      this.goToSong(song, true);
    }
  }

  selectIndex(i: number) {
    this.selectedIndex = i;
  }

  preventDoubleClickDefault(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  goToSong(song: Song, play = false) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song, play);
    this.libraryService.startScrollToSong(song);
    this.activeModal.close();
  }
}
