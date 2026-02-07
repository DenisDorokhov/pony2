import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist, Genre} from '../../domain/library.model';
import {LibraryService} from '../../service/library.service';
import {LoadingState} from '../../domain/common.model';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {LoadingIndicatorComponent} from '../common/loading-indicator.component';
import {CommonModule} from '@angular/common';
import {ErrorIndicatorComponent} from '../common/error-indicator.component';
import {NoContentIndicatorComponent} from '../common/no-content-indicator.component';
import {ArtistComponent} from './artist.component';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle
} from '@ng-bootstrap/ng-bootstrap';
import {shouldShowNewIndicator} from '../../utils/indicator.utils';
import {InstallationService} from '../../service/installation.service';

export class NavigationItem {

  id: string;
  title: string;
  menuEntry: string;

  constructor(id: string, menuEntry: string, title: string) {
    this.id = id;
    this.menuEntry = menuEntry;
    this.title = title;
  }
}

@Component({
  imports: [CommonModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, ArtistComponent, NgbDropdown, NgbDropdownToggle, NgbDropdownMenu, NgbDropdownButtonItem, NgbDropdownItem],
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
  genreCounter: Record<string, number> = {};

  navigationItems: NavigationItem[] = [];
  selectedNavigationItem!: NavigationItem;

  @ViewChild('scroller') scrollerElement!: ElementRef;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
    private readonly translateService: TranslateService,
    private readonly installationService: InstallationService,
  ) {
    this.navigationItems = [this.allArtistsNavigationItem()];
    this.selectedNavigationItem = this.navigationItems[0];
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeGenres().subscribe(genres => {
      this.genres = genres;
      const oldSelectedNavigationItem = this.selectedNavigationItem;
      this.reloadNavigationItems();
      this.filterArtists(oldSelectedNavigationItem.id !== this.selectedNavigationItem.id);
    }));
    this.subscriptions.push(this.libraryService.observeArtists().subscribe(artists => {
      this.artists = artists;
      this.reloadGenreCounter();
      this.reloadNavigationItems();
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
    this.subscriptions.push(this.libraryService.observeFilterByGenreRequest().subscribe(genre => {
      const navigationItems = this.navigationItems.filter(navigationItem => navigationItem.id === genre!.id);
      if (navigationItems.length > 0) {
        this.onNavigationItemClick(navigationItems[0]);
      }
    }));
  }

  private reloadNavigationItems() {
    this.navigationItems = [];
    this.navigationItems.push(this.allArtistsNavigationItem());
    this.navigationItems.push(new NavigationItem(
      'updated',
      this.translateService.instant('library.artist.updatedArtistsLabel', {'artistCount': this.artists.filter(artist =>
          shouldShowNewIndicator(artist.updateDate, this.installationService.installationStatus) ||
          shouldShowNewIndicator(artist.creationDate, this.installationService.installationStatus)).length}),
      this.translateService.instant('library.artist.updatedArtistsTitleLabel')
    ));
    this.genres.forEach(genre =>
      this.navigationItems.push(
        new NavigationItem(
          genre.id,
          this.translateService.instant('library.artist.genreNavigationLabel', {'genreName': genre.name, 'artistCount': this.genreCounter[genre.id]}),
          genre.name ?? this.translateService.instant('library.genre.unknownLabel')
        )));
    const oldSelectedNavigationItemId = this.selectedNavigationItem.id;
    this.selectedNavigationItem = this.navigationItems.filter(item => item.id === oldSelectedNavigationItemId)[0] ?? this.navigationItems[0];
  }

  private allArtistsNavigationItem() {
    return new NavigationItem(
      'all',
      this.translateService.instant('library.artist.allArtistsLabel', {'artistCount': this.artists.length}),
      this.translateService.instant('library.artist.allArtistsTitleLabel')
    );
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
    if (this.selectedNavigationItem.id === 'all') {
      this.filteredArtists = this.artists;
    } else if (this.selectedNavigationItem.id === 'updated') {
      this.filteredArtists = this.artists.filter(artist =>
        shouldShowNewIndicator(artist.updateDate, this.installationService.installationStatus) ||
        shouldShowNewIndicator(artist.creationDate, this.installationService.installationStatus));
    } else {
      this.filteredArtists = this.artists.filter(artist =>
        artist.genres.findIndex(artistGenre =>
          artistGenre.id === this.selectedNavigationItem.id) > -1
      );
    }
    if (scrollToSelectedArtist) {
      this.scrollToSelectedArtist();
    }
  }

  private scrollToSelectedArtist() {
    if (this.filteredArtists.findIndex(artist => artist.id === this.libraryService.selectedArtist?.id) > -1) {
      setTimeout(() => this.libraryService.requestScrollToArtist(this.libraryService.selectedArtist!));
    }
  }

  onNavigationItemClick(navigationItem: NavigationItem) {
    this.selectedNavigationItem = navigationItem;
    this.filterArtists();
    this.scrollerElement.nativeElement.scrollTop = 0;
    this.scrollToSelectedArtist();
  }
}
