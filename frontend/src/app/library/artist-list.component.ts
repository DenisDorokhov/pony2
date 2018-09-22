import {Component, OnDestroy, OnInit} from '@angular/core';
import * as Logger from 'js-logger';
import {Subscription} from 'rxjs/Subscription';
import {LoadingState} from '../core/common/common.model';
import {Artist} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-artist-list',
  templateUrl: './artist-list.component.html',
  styleUrls: ['./artist-list.component.scss']
})
export class ArtistListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;
  
  loadingState = LoadingState.LOADING;
  artists: Artist[] = [];

  private artistsSubscription: Subscription;
  private refreshRequestSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.loadArtists();
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest()
      .subscribe(() => {
        this.loadArtists(true);
      });
  }

  ngOnDestroy(): void {
    this.artistsSubscription.unsubscribe();
    this.refreshRequestSubscription.unsubscribe();
  }
  
  trackByArtistId(index: number, artist: Artist): string {
    return artist.id;
  }

  private loadArtists(refreshing: boolean = false) {
    if (refreshing) {
      Logger.info('Refreshing artists...');
    } else {
      Logger.info('Loading artists...');
      this.loadingState = LoadingState.LOADING;
    }
    if (this.artistsSubscription) {
      this.artistsSubscription.unsubscribe();
    }
    this.artistsSubscription = this.libraryService.getArtists()
      .subscribe(
        artists => {
          this.artists = artists;
          if (artists.length > 0) {
            this.loadingState = LoadingState.LOADED;
            Logger.info(`${artists.length} artists loaded.`);
            const selectedArtist = this.libraryService.selectDefaultArtist(artists);
            if (selectedArtist) {
              this.libraryService.startScrollToArtist(selectedArtist);
            }
          } else {
            this.loadingState = LoadingState.EMPTY;
            Logger.info(`No artists found.`);
            this.libraryService.deselectArtist();
            this.libraryService.deselectSong();
          }
        },
        error => {
          if (!refreshing) {
            this.loadingState = LoadingState.ERROR;
          }
          Logger.error(`Could not load artists: "${error.message}".`);
        }
      );
  }
}
