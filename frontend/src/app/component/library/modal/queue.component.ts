import {Component, OnDestroy, OnInit} from "@angular/core";
import {CommonModule} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {PlaybackEvent, PlaybackService, PlaybackState} from "../../../service/playback.service";
import {Subscription} from "rxjs";
import {Song} from "../../../domain/library.model";
import {UnknownArtistPipe} from "../../../pipe/unknown-artist.pipe";
import {UnknownSongPipe} from "../../../pipe/unknown-song.pipe";
import {ImageLoaderComponent} from "../../common/image-loader.component";
import {LibraryService} from "../../../service/library.service";
import {LoadingState} from "../../../domain/common.model";
import {NoContentIndicatorComponent} from "../../common/no-content-indicator.component";

@Component({
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    UnknownArtistPipe,
    UnknownSongPipe,
    ImageLoaderComponent,
    NoContentIndicatorComponent
  ],
  selector: 'pony-queue',
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.scss']
})
export class QueueComponent implements OnInit, OnDestroy {

  queue: Song[] = [];
  playbackEvent: PlaybackEvent | undefined;

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

  playSong(song: Song) {
    this.libraryService.requestSongPlayback(song);
  }

  onMouseDown(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  protected readonly PlaybackState = PlaybackState;
  protected readonly LoadingState = LoadingState;
}
