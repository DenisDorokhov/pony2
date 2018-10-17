import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';
import {PlaybackService, PlaybackState} from '../core/library/playback.service';
import {ScrollingUtils} from '../shared/scrolling.utils';

@Component({
  selector: 'pony-artist',
  templateUrl: './artist.component.html',
  styleUrls: ['./artist.component.scss']
})
export class ArtistComponent implements OnInit, OnDestroy {

  PlaybackState = PlaybackState;
  
  @Input() artist: Artist;

  @ViewChild('container') containerElement: ElementRef;
  
  selected = false;
  playbackState: PlaybackState | undefined;
  
  private selectedArtistSubscription: Subscription;
  private scrollToArtistRequestSubscription: Subscription;
  private scrollToSongRequestSubscription: Subscription;
  private playbackEventSubscription: Subscription;

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService
  ) {
  }

  ngOnInit(): void {
    this.selectedArtistSubscription = this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        this.selected = artist && artist.id === this.artist.id;
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
    this.selectedArtistSubscription.unsubscribe();
    this.scrollToArtistRequestSubscription.unsubscribe();
    this.scrollToSongRequestSubscription.unsubscribe();
    this.playbackEventSubscription.unsubscribe();
  }
  
  select() {
    this.libraryService.selectArtistAndMakeDefault(this.artist);
  }
}
