import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Song} from "../../domain/library.model";
import {PlaybackService, PlaybackState} from "../../service/playback.service";
import {TranslateModule} from "@ngx-translate/core";
import {LibraryService} from "../../service/library.service";
import {ScrollingUtils} from "../../utils/scrolling.utils";
import {UnknownSongPipe} from "../../pipe/unknown-song.pipe";

@Component({
  standalone: true,
  imports: [TranslateModule, UnknownSongPipe],
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy, AfterViewInit {

  PlaybackState = PlaybackState;

  private _song!: Song;

  get song(): Song {
    return this._song;
  }

  @Input()
  set song(song: Song) {
    this._song = song;
    this.duration = this.song.durationInMinutes;
    this.selected = this.libraryService.selectedSong?.id === this.song.id;
    if (this.playbackService.lastPlaybackEvent.song?.id === this.song.id) {
      this.playbackState = this.playbackService.lastPlaybackEvent.state;
    } else {
      this.playbackState = undefined;
    }
  }

  @Input() showArtist = false;

  @ViewChild('container') containerElement!: ElementRef;

  duration: string | undefined;
  selected = false;
  playbackState: PlaybackState | undefined;

  private selectedSongSubscription: Subscription | undefined;
  private scrollToSongRequestSubscription: Subscription | undefined;
  private playbackEventSubscription: Subscription | undefined;

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService
  ) {
  }

  ngOnInit(): void {
    this.selectedSongSubscription = this.libraryService.observeSelectedSong()
      .subscribe(song => {
        this.selected = song != null && song.id === this.song.id;
      });
    this.playbackEventSubscription = this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.id === this.song.id) {
          this.playbackState = playbackEvent.state;
        } else {
          this.playbackState = undefined;
        }
      });
  }

  ngAfterViewInit(): void {
    this.scrollToSongRequestSubscription = this.libraryService.observeScrollToSongRequest()
      .subscribe(song => {
        if (song.id === this.song.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToSong();
        }
      });
  }

  ngOnDestroy(): void {
    this.selectedSongSubscription?.unsubscribe();
    this.scrollToSongRequestSubscription?.unsubscribe();
    this.playbackEventSubscription?.unsubscribe();
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  play() {
    this.libraryService.requestSongPlayback(this.song);
  }
}
