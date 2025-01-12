import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal, NgbDropdownModule} from '@ng-bootstrap/ng-bootstrap';
import {PlaybackHistory, Song} from '../../../domain/library.model';
import {LoadingState} from '../../../domain/common.model';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {PlaybackHistoryService} from '../../../service/playback-history.service';
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {LargeSongComponent} from './common/large-song.component';
import {NoContentIndicatorComponent} from '../../common/no-content-indicator.component';
import {LibraryService} from '../../../service/library.service';
import {PlaybackEvent} from '../../../service/audio-player.service';
import {PlaybackService} from '../../../service/playback.service';
import {Subscription} from 'rxjs';

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
export class HistoryComponent implements OnInit, OnDestroy {

  readonly LoadingState = LoadingState;

  readonly rowHeight = LargeSongComponent.HEIGHT;

  loadingState: LoadingState = LoadingState.LOADING;
  playbackHistory: PlaybackHistory | undefined;
  selectedIndex = -1;
  lastPlaybackEvent: PlaybackEvent | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playbackHistoryService: PlaybackHistoryService,
    private readonly libraryService: LibraryService,
    private readonly playbackService: PlaybackService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.playbackHistoryService.getHistory().subscribe({
      next: playbackHistory => {
        this.loadingState = LoadingState.LOADED;
        this.playbackHistory = playbackHistory;
      },
      error: () => this.loadingState = LoadingState.ERROR
    });
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.lastPlaybackEvent = playbackEvent));
  }

  onSongDoubleClick(song: Song) {
    this.goToSong(song, true);
  }

  selectIndex(i: number) {
    this.selectedIndex = i;
  }

  onPlaybackClick(index: number) {
    const playlistSong = this.playbackHistory!.songs[index];
    if (playlistSong.song.id === this.lastPlaybackEvent?.song?.id) {
      this.playbackService.playOrPause();
    } else {
      this.goToSong(playlistSong.song, true);
    }
  }

  goToSong(song: Song, play = false) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song, play);
    this.libraryService.requestScrollToSong(song);
    this.activeModal.close();
  }
}
