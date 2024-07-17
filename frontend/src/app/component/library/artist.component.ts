import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from "../../domain/library.model";
import {LibraryService} from "../../service/library.service";
import {PlaybackService, PlaybackState} from "../../service/playback.service";
import {ScrollingUtils} from "../../utils/scrolling.utils";
import {ImageLoaderComponent} from "../common/image-loader.component";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, ImageLoaderComponent],
  selector: 'pony-artist',
  templateUrl: './artist.component.html',
  styleUrls: ['./artist.component.scss']
})
export class ArtistComponent implements OnInit, OnDestroy {

  PlaybackState = PlaybackState;

  @Input() artist!: Artist;

  @ViewChild('container') containerElement!: ElementRef;

  selected = false;
  playbackState: PlaybackState | undefined;

  private selectedArtistSubscription: Subscription | undefined;
  private scrollToArtistRequestSubscription: Subscription | undefined;
  private scrollToSongRequestSubscription: Subscription | undefined;
  private playbackEventSubscription: Subscription | undefined;

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService
  ) {
  }

  ngOnInit(): void {
    this.selectedArtistSubscription = this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        this.selected = artist != null && artist.id === this.artist.id;
      });
    this.scrollToArtistRequestSubscription = this.libraryService.observeScrollToArtistRequest()
      .subscribe(artist => {
        if (artist.id === this.artist.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToArtist();
        }
      });
    this.scrollToSongRequestSubscription = this.libraryService.observeScrollToSongRequest()
      .subscribe(song => {
        if (song.album.artist.id === this.artist.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
        }
      });
    this.playbackEventSubscription = this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.album.artist.id === this.artist.id) {
          this.playbackState = playbackEvent.state;
        } else {
          this.playbackState = undefined;
        }
      });
  }

  ngOnDestroy(): void {
    this.selectedArtistSubscription?.unsubscribe();
    this.scrollToArtistRequestSubscription?.unsubscribe();
    this.scrollToSongRequestSubscription?.unsubscribe();
    this.playbackEventSubscription?.unsubscribe();
  }

  select() {
    this.libraryService.selectArtistAndMakeDefault(this.artist);
  }
}
