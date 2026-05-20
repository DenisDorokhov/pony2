import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from '../../domain/library.model';
import {LibraryService} from '../../service/library.service';
import {PlaybackService} from '../../service/playback.service';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {PlaybackState} from '../../service/audio-player.service';
import {shouldShowNewIndicator} from '../../utils/indicator.utils';
import {InstallationService} from '../../service/installation.service';

@Component({
    imports: [CommonModule, TranslateModule, ImageLoaderComponent, UnknownArtistPipe],
    selector: 'pony-artist',
    templateUrl: './artist.component.html',
    styleUrls: ['./artist.component.scss']
})
export class ArtistComponent implements OnInit, OnDestroy {

  static readonly HEIGHT = 70;

  PlaybackState = PlaybackState;

  genreNames: string[] = [];

  showNewIndicator = false;

  private _artist!: Artist;

  get artist(): Artist {
    return this._artist;
  }

  @Input()
  set artist(artist: Artist) {

    this._artist = artist;

    this.showNewIndicator = shouldShowNewIndicator(artist.updateDate, this.installationService.installationStatus) ||
      shouldShowNewIndicator(artist.creationDate, this.installationService.installationStatus);

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

  selected = false;
  playbackState: PlaybackState | undefined;

  private selectedArtistSubscription: Subscription | undefined;
  private playbackEventSubscription: Subscription | undefined;

  constructor(
    private readonly libraryService: LibraryService,
    private readonly playbackService: PlaybackService,
    private readonly translateService: TranslateService,
    private readonly installationService: InstallationService
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

  ngOnDestroy(): void {
    this.selectedArtistSubscription?.unsubscribe();
    this.playbackEventSubscription?.unsubscribe();
  }

  select() {
    this.libraryService.selectArtistAndMakeDefault(this.artist);
  }
}
