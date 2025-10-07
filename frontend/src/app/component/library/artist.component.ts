import {AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from '../../domain/library.model';
import {LibraryService} from '../../service/library.service';
import {PlaybackService} from '../../service/playback.service';
import {ScrollingUtils} from '../../utils/scrolling.utils';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {PlaybackState} from '../../service/audio-player.service';

@Component({
    imports: [CommonModule, TranslateModule, ImageLoaderComponent, UnknownArtistPipe],
    selector: 'pony-artist',
    templateUrl: './artist.component.html',
    styleUrls: ['./artist.component.scss']
})
export class ArtistComponent implements AfterViewInit, OnInit, OnDestroy {

  PlaybackState = PlaybackState;

  genreNames: string[] = [];

  private _artist!: Artist;

  get artist(): Artist {
    return this._artist;
  }

  @Input()
  set artist(artist: Artist) {
    this._artist = artist;
    this.genreNames = this.artist.genres.map(genre => {
      if (genre.name) {
        return genre.name;
      }
      return this.translateService.instant('library.genre.unknownLabel');
    });
    this.selected = this.libraryService.selectedArtist?.id === this.artist.id;
    if (this.playbackService.lastPlaybackEvent.song?.album.artist.id === this.artist.id) {
      this.playbackState = this.playbackService.lastPlaybackEvent.state;
    } else {
      this.playbackState = undefined;
    }
  }

  @ViewChild('container') containerElement!: ElementRef;

  selected = false;
  playbackState: PlaybackState | undefined;

  private selectedArtistSubscription: Subscription | undefined;
  private scrollToArtistRequestSubscription: Subscription | undefined;
  private scrollToAlbumRequestSubscription: Subscription | undefined;
  private scrollToSongRequestSubscription: Subscription | undefined;
  private playbackEventSubscription: Subscription | undefined;

  constructor(
    private readonly libraryService: LibraryService,
    private readonly playbackService: PlaybackService,
    private readonly translateService: TranslateService,
  ) {
  }

  ngOnInit(): void {
    this.selectedArtistSubscription = this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        this.selected = artist != null && artist.id === this.artist.id;
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

  ngAfterViewInit(): void {
    this.scrollToArtistRequestSubscription = this.libraryService.observeScrollToArtistRequest()
      .subscribe(artist => {
        if (artist.id === this.artist.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToArtist();
        }
      });
    this.scrollToAlbumRequestSubscription = this.libraryService.observeScrollToAlbumRequest()
      .subscribe(album => {
        if (album.artist.id === this.artist.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
        }
      });
    this.scrollToSongRequestSubscription = this.libraryService.observeScrollToSongRequest()
      .subscribe(request => {
        if (request.scrollToArtist && request.song.album.artist.id === this.artist.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
        }
      });
  }

  ngOnDestroy(): void {
    this.selectedArtistSubscription?.unsubscribe();
    this.scrollToArtistRequestSubscription?.unsubscribe();
    this.scrollToAlbumRequestSubscription?.unsubscribe();
    this.scrollToSongRequestSubscription?.unsubscribe();
    this.playbackEventSubscription?.unsubscribe();
  }

  select() {
    this.libraryService.selectArtistAndMakeDefault(this.artist);
    ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement, false);
  }
}
