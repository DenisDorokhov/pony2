import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {fromEvent, Subscription} from 'rxjs';
import {Song} from "../../domain/library.model";
import {PlaybackService, PlaybackState} from "../../service/playback.service";
import {TranslateModule} from "@ngx-translate/core";
import {LibraryService} from "../../service/library.service";
import {ScrollingUtils} from "../../utils/scrolling.utils";
import {UnknownSongPipe} from "../../pipe/unknown-song.pipe";
import {UnknownArtistPipe} from "../../pipe/unknown-artist.pipe";

@Component({
  standalone: true,
  imports: [TranslateModule, UnknownSongPipe, UnknownArtistPipe],
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

  private subscriptions: Subscription[] = [];

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeSelectedSong()
      .subscribe(song => {
        this.selected = song != null && song.id === this.song.id;
        if (this.selected && this.containerElement) {
          this.containerElement.nativeElement.focus();
        }
      }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.id === this.song.id) {
          this.playbackState = playbackEvent.state;
        } else {
          this.playbackState = undefined;
        }
      }));
  }

  ngAfterViewInit(): void {
    this.subscriptions.push(this.libraryService.observeScrollToSongRequest()
      .subscribe(song => {
        if (song.id === this.song.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToSong();
        }
      }));
    this.subscriptions.push(fromEvent<KeyboardEvent>(this.containerElement.nativeElement, 'keydown').subscribe(event => {
      if (event.code === 'Enter' || event.code === 'Space') {
        this.select();
        this.libraryService.requestSongPlayback(this.song);
        event.preventDefault();
      }
    }));
    if (this.selected) {
      this.containerElement.nativeElement.focus();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  play() {
    this.libraryService.requestSongPlayback(this.song);
  }
}
