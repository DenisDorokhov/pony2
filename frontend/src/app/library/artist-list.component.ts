import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {LoadingState} from '../core/common/loading-state';
import {ArtistDto} from '../core/library/artist.dto';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-artist-list',
  templateUrl: './artist-list.component.html',
  styleUrls: ['./artist-list.component.scss']
})
export class ArtistListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artists: ArtistDto[] = [];

  selectedArtist: number;

  private artistsSubscription: Subscription;
  private selectedArtistSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.loadArtists();
    this.selectedArtistSubscription = this.libraryService.selectedArtist
      .subscribe(artist => {
        if (artist) {
          this.selectedArtist = artist.id;
        }
      });
  }

  ngOnDestroy(): void {
    this.selectedArtistSubscription.unsubscribe();
  }

  selectArtist(artist: ArtistDto) {
    console.log(`Selecting artist ${artist.id} -> '${artist.name}'.`);
    this.libraryService.selectArtist(artist);
  }

  private loadArtists() {
    console.log('Loading artists...');
    this.loadingState = LoadingState.LOADING;
    if (this.artistsSubscription) {
      this.artistsSubscription.unsubscribe();
    }
    this.artistsSubscription = this.libraryService.getArtists()
      .subscribe(
        artists => {
          this.artists = artists;
          if (artists.length > 0) {
            this.loadingState = LoadingState.LOADED;
            console.log(`${artists.length} artists loaded.`);
            if (!this.selectedArtist) {
              this.libraryService.selectArtist(artists[0]);
            }
          } else {
            this.loadingState = LoadingState.EMPTY;
            console.log(`No artists found.`);
            this.libraryService.deselectArtist();
            this.libraryService.deselectSong();
          }
        },
        error => {
          this.loadingState = LoadingState.ERROR;
          console.log(`Could not load artists: "${error.message}".`);
        }
      );
  }
}
