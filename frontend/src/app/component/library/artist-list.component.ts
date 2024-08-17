import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from "../../domain/library.model";
import {LibraryService} from "../../service/library.service";
import {LoadingState} from "../../domain/common.model";
import {TranslateModule} from "@ngx-translate/core";
import {LoadingIndicatorComponent} from "../common/loading-indicator.component";
import {CommonModule} from "@angular/common";
import {ErrorIndicatorComponent} from "../common/error-indicator.component";
import {NoContentIndicatorComponent} from "../common/no-content-indicator.component";
import {ArtistComponent} from "./artist.component";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, ArtistComponent],
  selector: 'pony-artist-list',
  templateUrl: './artist-list.component.html',
  styleUrls: ['./artist-list.component.scss']
})
export class ArtistListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artists: Artist[] = [];

  private artistsSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

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
    this.artistsSubscription?.unsubscribe();
    this.refreshRequestSubscription?.unsubscribe();
  }

  trackByArtist(_: number, artist: Artist) {
    return artist.id;
  }

  private loadArtists(refreshing: boolean = false) {
    if (refreshing) {
      console.info('Refreshing artists...');
    } else {
      console.info('Loading artists...');
      this.loadingState = LoadingState.LOADING;
    }
    if (this.artistsSubscription) {
      this.artistsSubscription.unsubscribe();
    }
    this.artistsSubscription = this.libraryService.getArtists()
      .subscribe({
        next: artists => {
          this.artists = artists;
          if (artists.length > 0) {
            this.loadingState = LoadingState.LOADED;
            console.info(`${artists.length} artists loaded.`);
            const oldSelectedArtist = this.libraryService.selectedArtist;
            const selectedArtist = this.libraryService.selectDefaultArtist(artists)!;
            if (!Artist.equals(selectedArtist, oldSelectedArtist)) {
              this.libraryService.startScrollToArtist(selectedArtist);
            }
          } else {
            this.loadingState = LoadingState.EMPTY;
            console.info(`No artists found.`);
            this.libraryService.deselectArtist();
            this.libraryService.deselectSong();
          }
        },
        error: error => {
          if (!refreshing) {
            this.loadingState = LoadingState.ERROR;
          }
          console.error(`Could not load artists: "${error.message}".`);
        }
      });
  }
}
