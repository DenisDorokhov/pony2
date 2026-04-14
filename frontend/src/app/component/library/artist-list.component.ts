import {Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {fromEvent, Subscription} from 'rxjs';
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
import {ScrollingUtils} from '../../utils/scrolling.utils';
import {InstallationService} from '../../service/installation.service';
import {keyboardLayoutInsensitiveMatch} from '../../utils/search.utils';
import scrollIntoElement = ScrollingUtils.scrollIntoElement;

export class NavigationItem {

  id: string;
  title: string;
  menuEntry: string;
  active = false;

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
  filterGenre = '';

  navigationItems: NavigationItem[] = [];
  selectedNavigationItem!: NavigationItem;

  @ViewChild('filterElement') filterElement!: ElementRef;
  @ViewChild('scrollerElement') scrollerElement!: ElementRef;
  @ViewChild(NgbDropdown) filterDropdown!: NgbDropdown;
  @ViewChildren('navigationItemElement') dropdownItems!: QueryList<ElementRef>;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
    private readonly translateService: TranslateService,
    private readonly installationService: InstallationService,
  ) {
    this.navigationItems = [this.allArtistsNavigationItem(), this.updatedArtistsNavigationItem()];
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
        this.executeNavigationItem(navigationItems[0]);
      }
    }));
    this.subscriptions.push(fromEvent<KeyboardEvent>(window.document.body, 'keydown').subscribe(event => {
      if (event.ctrlKey && event.shiftKey && event.code === 'KeyG') {
        this.filterDropdown.open();
        this.filterElement.nativeElement.focus();
        event.preventDefault();
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

  private reloadNavigationItems() {
    const oldActiveNavigationItemId = this.navigationItems.filter(item => item.active)[0]?.id;
    this.navigationItems = [];
    this.navigationItems.push(this.allArtistsNavigationItem());
    this.navigationItems.push(this.updatedArtistsNavigationItem());
    const idToNavigationItem: Record<string, NavigationItem> = {};
    this.navigationItems.forEach(navigationItem =>
      idToNavigationItem[navigationItem.id] = navigationItem);
    this.genres.forEach(genre => {
      const genreName = genre.name ?? this.translateService.instant('library.genre.unknownLabel');
      const navigationItem = new NavigationItem(
        genre.id,
        this.translateService.instant('library.artist.genreNavigationLabel', {
          genreName,
          'artistCount': this.genreCounter[genre.id]
        }),
        genreName
      );
      idToNavigationItem[navigationItem.id] = navigationItem;
    });
    this.genres.filter(genre => {
      const genreNameNormalized = genre.name ?? this.translateService.instant('library.genre.unknownLabel');
      return keyboardLayoutInsensitiveMatch(this.filterGenre, genreNameNormalized);
    }).forEach(genre => {
      this.navigationItems.push(idToNavigationItem[genre.id]);
    });
    const oldSelectedNavigationItemId = this.selectedNavigationItem.id;
    this.selectedNavigationItem = idToNavigationItem[oldSelectedNavigationItemId] ?? this.navigationItems[0];
    this.navigationItems.forEach(navigationItem =>
      navigationItem.active = navigationItem.id === oldActiveNavigationItemId);
  }

  private allArtistsNavigationItem() {
    return new NavigationItem(
      'all',
      this.translateService.instant('library.artist.allArtistsLabel', {'artistCount': this.artists.length}),
      this.translateService.instant('library.artist.allArtistsTitleLabel')
    );
  }

  private updatedArtistsNavigationItem() {
    return new NavigationItem(
      'updated',
      this.translateService.instant('library.artist.updatedArtistsLabel', {
        'artistCount': this.artists.filter(artist =>
          shouldShowNewIndicator(artist.updateDate, this.installationService.installationStatus) ||
          shouldShowNewIndicator(artist.creationDate, this.installationService.installationStatus)).length
      }),
      this.translateService.instant('library.artist.updatedArtistsTitleLabel')
    );
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
    } else {
      this.scrollerElement.nativeElement.scrollTop = 0;
    }
  }

  onNavigationDropdownOpenChange(open: boolean) {
    if (open) {
      setTimeout(() => this.filterElement.nativeElement.focus(), 50);
      const selectedIndex = this.navigationItems.findIndex(item => item.id === this.selectedNavigationItem.id);
      for (let i = 0; i < this.navigationItems.length; i++) {
        this.navigationItems[i].active = i === selectedIndex;
      }
      const selectedElement = this.dropdownItems.toArray()[selectedIndex];
      if (selectedElement) {
        setTimeout(() =>
          ScrollingUtils.scrollIntoElement(selectedElement.nativeElement, true));
      }
    } else {
      this.filterGenre = '';
      this.reloadNavigationItems();
    }
  }

  executeNavigationItem(navigationItem: NavigationItem) {
    this.selectedNavigationItem = navigationItem;
    this.filterArtists();
    this.scrollToSelectedArtist();
    this.filterDropdown.close();
  }

  filterGenres(value: string) {
    this.filterElement.nativeElement.focus();
    this.filterGenre = value;
    this.reloadNavigationItems();
    if (keyboardLayoutInsensitiveMatch(this.filterGenre, this.navigationItems[0].title)) {
      this.activateNavigationItem(0);
    } else if (keyboardLayoutInsensitiveMatch(this.filterGenre, this.navigationItems[1].title)) {
      this.activateNavigationItem(1);
    } else {
      this.activateNavigationItem(this.filterGenre.trim().length > 0 && this.navigationItems.length > 2 ? 2 : 0);
    }
  }

  clearGenreFilter() {
    this.filterElement.nativeElement.focus();
    this.filterGenre = '';
    this.reloadNavigationItems();
    setTimeout(() => {
      const selectedIndex = this.navigationItems.findIndex(item => item.id === this.selectedNavigationItem.id);
      if (selectedIndex >= 0) {
        this.activateNavigationItem(selectedIndex, true);
      }
    });
  }

  onFilterKeyDown(event: KeyboardEvent) {
    switch (event.key) {
      case 'ArrowDown':
        this.moveNavigationIndex(1, true);
        event.preventDefault();
        break;
      case 'ArrowUp':
        this.moveNavigationIndex(1, false);
        event.preventDefault();
        break;
      case 'PageDown':
        this.moveNavigationIndex(7, true, false);
        event.preventDefault();
        break;
      case 'PageUp':
        this.moveNavigationIndex(7, false, false);
        event.preventDefault();
        break;
      case 'Home':
        this.activateNavigationItem(0);
        event.preventDefault();
        break;
      case 'End':
        this.activateNavigationItem(this.navigationItems.length - 1);
        event.preventDefault();
        break;
      case 'Enter': {
        const selectedIndex = this.indexOfActiveNavigationItem();
        if (selectedIndex >= 0) {
          this.executeNavigationItem(this.navigationItems[selectedIndex]);
        }
        event.preventDefault();
        break;
      }
    }
  }

  private indexOfActiveNavigationItem(): number {
    return this.navigationItems.findIndex(next => next.active);
  }

  private moveNavigationIndex(value: number, next: boolean, loop = true) {
    let selectedIndex = this.indexOfActiveNavigationItem();
    if (selectedIndex < 0) {
      selectedIndex = this.navigationItems.findIndex(item => item.id === this.selectedNavigationItem.id);
    }
    let indexToSelect: number = next ? selectedIndex + value : selectedIndex - value;
    if (indexToSelect < 0) {
      if (loop) {
        indexToSelect = this.navigationItems.length - 1;
      } else {
        indexToSelect = 0;
      }
    }
    if (indexToSelect > this.navigationItems.length - 1) {
      if (loop) {
        indexToSelect = 0;
      } else {
        indexToSelect = this.navigationItems.length - 1;
      }
    }
    if (indexToSelect >= 0) {
      this.activateNavigationItem(indexToSelect);
    }
  }

  activateNavigationItem(index: number, scrollToCenter = false) {
    this.navigationItems.forEach(next => next.active = false);
    this.navigationItems[index].active = true;
    setTimeout(() => {
      const selectedElement = this.dropdownItems.toArray()[index];
      scrollIntoElement(selectedElement.nativeElement, scrollToCenter);
    });
  }

  protected resetNavigation() {
    this.executeNavigationItem(this.navigationItems[0]);
  }
}
