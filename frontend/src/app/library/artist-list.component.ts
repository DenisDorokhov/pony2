import {Component, OnInit} from '@angular/core';
import {LoadingState} from '../core/common/loading-state';
import {ArtistDto} from '../core/library/artist.dto';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-artist-list',
  templateUrl: './artist-list.component.html',
  styleUrls: ['./artist-list.component.scss']
})
export class ArtistListComponent implements OnInit {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artists: ArtistDto[] = [];

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.loadArtists();
  }

  private loadArtists() {
    console.log('Loading artists...');
    this.libraryService.getArtists()
      .subscribe(
        artists => {
          this.artists = artists;
          if (artists.length > 0) {
            this.loadingState = LoadingState.LOADED;
            console.log(`${artists.length} artists loaded.`);
          } else {
            this.loadingState = LoadingState.EMPTY;
            console.log(`No artists found.`);
          }
        },
        error => {
          this.loadingState = LoadingState.ERROR;
          console.log(`Could not load artists: "${error.message}".`);
        }
      );
  }
}
