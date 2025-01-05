import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist, Genre} from '../../domain/library.model';
import {LibraryService} from '../../service/library.service';
import {LoadingState} from '../../domain/common.model';
import {TranslateModule} from '@ngx-translate/core';
import {LoadingIndicatorComponent} from '../common/loading-indicator.component';
import {CommonModule} from '@angular/common';
import {ErrorIndicatorComponent} from '../common/error-indicator.component';
import {NoContentIndicatorComponent} from '../common/no-content-indicator.component';
import {ArtistComponent} from './artist.component';
import {UnknownGenrePipe} from '../../pipe/unknown-genre.pipe';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle
} from '@ng-bootstrap/ng-bootstrap';

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, ArtistComponent, UnknownGenrePipe, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownButtonItem, NgbDropdownItem],
  selector: 'pony-artist-list',
  templateUrl: './artist-list.component.html',
  styleUrls: ['./artist-list.component.scss']
})
export class ArtistListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artists: Artist[] = [];
  filteredArtists: Artist[] = [];
  genres: Genre[] = [];
  selectedGenre: Genre | undefined;
  genreCounter: Record<string, number> = {};

  @ViewChild('scroller') scrollerElement!: ElementRef;

  private artistsSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;

  constructor(
    private readonly libraryService: LibraryService
  ) {
  }

  ngOnInit(): void {
    this.loadGenres();
    this.loadArtists();
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest()
      .subscribe(() => {
        this.loadGenres();
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

  private loadGenres() {
    this.libraryService.getGenres().subscribe(genres => {
      this.genres = genres;
      const oldSelectedGenre = this.selectedGenre;
      this.selectedGenre = this.genres.filter(genre => genre.id === this.selectedGenre?.id)[0];
      this.filterArtists(oldSelectedGenre?.id !== this.selectedGenre?.id);
    });
  }

  private loadArtists(refreshing = false) {
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
          this.reloadGenreCounter();
          this.filterArtists();
          if (artists.length > 0) {
            this.loadingState = LoadingState.LOADED;
            console.info(`${artists.length} artists loaded.`);
            const oldSelectedArtist = this.libraryService.selectedArtist;
            const selectedArtist = this.libraryService.selectDefaultArtist(artists)!;
            if (!Artist.equals(selectedArtist, oldSelectedArtist)) {
              this.libraryService.requestScrollToArtist(selectedArtist);
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

  private reloadGenreCounter() {
    this.genreCounter = {};
    for (const artist of this.artists) {
      for (const genre of artist.genres) {
        if (this.genreCounter[genre.id] === undefined) {
          this.genreCounter[genre.id] = 0;
        }
        this.genreCounter[genre.id]++;
      }
    }
  }

  private filterArtists(scrollToSelectedArtist = false) {
    this.filteredArtists = this.selectedGenre ? this.artists.filter(artist =>
      artist.genres.findIndex(artistGenre =>
        artistGenre.id === this.selectedGenre!.id) > -1
    ) : this.artists;
    if (scrollToSelectedArtist) {
      this.scrollToSelectedArtist();
    }
  }

  private scrollToSelectedArtist() {
    if (this.filteredArtists.findIndex(artist => artist.id === this.libraryService.selectedArtist?.id) > -1) {
      setTimeout(() => this.libraryService.requestScrollToArtist(this.libraryService.selectedArtist!));
    }
  }

  onGenreClick(genre: Genre | undefined) {
    this.selectedGenre = genre;
    this.filterArtists();
    if (this.scrollerElement) {
      this.scrollerElement.nativeElement.scrollTop = 0;
    }
    this.scrollToSelectedArtist();
  }
}
