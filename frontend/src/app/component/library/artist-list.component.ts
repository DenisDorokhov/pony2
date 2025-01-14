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

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeGenres().subscribe(genres => {
      this.genres = genres;
      const oldSelectedGenre = this.selectedGenre;
      this.selectedGenre = this.genres.filter(genre => genre.id === this.selectedGenre?.id)[0];
      this.filterArtists(oldSelectedGenre?.id !== this.selectedGenre?.id);
    }));
    this.subscriptions.push(this.libraryService.observeArtists().subscribe(artists => {
      this.artists = artists;
      this.reloadGenreCounter();
      this.filterArtists();
      if (artists.length > 0) {
        this.loadingState = LoadingState.LOADED;
        const oldSelectedArtist = this.libraryService.selectedArtist;
        const selectedArtist = this.libraryService.selectDefaultArtist(artists)!;
        if (!Artist.equals(selectedArtist, oldSelectedArtist)) {
          this.libraryService.requestScrollToArtist(selectedArtist);
        }
      } else {
        this.loadingState = LoadingState.EMPTY;
        this.libraryService.deselectArtist();
        this.libraryService.deselectSong();
      }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  trackByArtist(_: number, artist: Artist) {
    return artist.id;
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
    this.scrollerElement.nativeElement.scrollTop = 0;
    this.scrollToSelectedArtist();
  }
}
