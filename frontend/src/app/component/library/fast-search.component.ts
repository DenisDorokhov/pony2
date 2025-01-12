import {Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {debounceTime, fromEvent, mergeMap, of, Subject, Subscription} from 'rxjs';
import {distinctUntilChanged, map} from 'rxjs/operators';
import {LibraryService} from '../../service/library.service';
import {Album, Artist, SearchResult, Song} from '../../domain/library.model';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {ScrollingUtils} from '../../utils/scrolling.utils';
import {UnknownSongPipe} from '../../pipe/unknown-song.pipe';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {UnknownAlbumPipe} from '../../pipe/unknown-album.pipe';
import scrollIntoElement = ScrollingUtils.scrollIntoElement;

class NavigationItem {

  id: string;
  selected = false;
  activate: () => void;

  constructor(id: string, activate: () => void) {
    this.id = id;
    this.activate = activate;
  }
}

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ImageLoaderComponent, UnknownArtistPipe, UnknownArtistPipe, UnknownAlbumPipe, UnknownSongPipe],
  selector: 'pony-fast-search',
  templateUrl: './fast-search.component.html',
  styleUrls: ['./fast-search.component.scss']
})
export class FastSearchComponent implements OnInit, OnDestroy {

  open = false;
  searchResult: SearchResult | undefined;

  navigationItems: NavigationItem[] = [];
  idToNavigationItem: Record<string, NavigationItem> = {};

  @ViewChild('container') containerElement!: ElementRef;
  @ViewChild('input') inputElement!: ElementRef;
  @ViewChild('searchResults') searchResultsElement!: ElementRef;
  @ViewChildren('links') linkElements!: QueryList<ElementRef>;

  private inputChangeSubject = new Subject<string>();
  private searchRequestSubject = new Subject<string>();

  private lastSearchQuery: string | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  ngOnInit(): void {

    this.subscriptions.push(this.inputChangeSubject.pipe(
      map(value => value.trim()),
      debounceTime(200),
      distinctUntilChanged(),
    ).subscribe(query => this.searchRequestSubject.next(query)));

    this.subscriptions.push(this.searchRequestSubject.pipe(
      mergeMap(query => {
        this.lastSearchQuery = query;
        if (query.length > 1) {
          return this.libraryService.search(query);
        } else {
          return of(undefined);
        }
      }),
      map(searchResult => {
        if (searchResult?.songs.length || searchResult?.albums.length || searchResult?.artists.length) {
          return searchResult;
        } else {
          return undefined;
        }
      })
    ).subscribe(searchResult => {
      this.updateSearchResult(searchResult);
      this.searchResult?.artists.forEach(artist => {
        const navigationItem = new NavigationItem(artist.id, () => this.goToArtist(artist));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      this.searchResult?.songs.forEach(song => {
        const navigationItem = new NavigationItem(song.id, () => this.goToSong(song));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      this.searchResult?.albums.forEach(album => {
        const navigationItem = new NavigationItem(album.id, () => this.goToAlbum(album));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      if (this.navigationItems.length > 0) {
        this.selectNavigationItem(0);
      }
    }));

    this.subscriptions.push(fromEvent(window.document.body, 'mousedown').subscribe(event => {
      let checkElement: Node | null = event.target as Node;
      let clickWithinContainer = false;
      do {
        clickWithinContainer = this.containerElement.nativeElement === checkElement;
        checkElement = (checkElement as Node).parentNode;
      } while (!clickWithinContainer && checkElement);
      if (!clickWithinContainer) {
        this.close();
      }
    }));

    this.subscriptions.push(fromEvent<KeyboardEvent>(window.document.body,'keydown').subscribe(event => {
      if (event.ctrlKey && event.shiftKey && event.code === 'KeyF') {
        this.inputElement.nativeElement.focus();
        event.preventDefault();
      }
    }));
  }

  private close() {
    this.updateSearchResult(undefined);
    this.open = false;
  }

  private updateSearchResult(searchResult: SearchResult | undefined) {
    this.searchResult = searchResult;
    this.navigationItems = [];
    this.idToNavigationItem = {};
    this.searchResultsElement.nativeElement.scrollTop = 0;
    requestAnimationFrame(() => this.searchResultsElement.nativeElement.scrollTop = 0);
  }

  onInputChange(event: Event) {
    this.inputChangeSubject.next((event.target as any).value);
  }

  onFocusIn() {
    if (!this.open && this.lastSearchQuery) {
      this.searchRequestSubject.next(this.lastSearchQuery);
    }
    this.open = true;
  }

  onInputKeyDown(event: KeyboardEvent) {
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
        this.moveNavigationIndex(4, true, false);
        event.preventDefault();
        break;
      case 'PageUp':
        this.moveNavigationIndex(4, false, false);
        event.preventDefault();
        break;
      case 'Enter': {
        const selectedIndex = this.indexOfSelectedNavigationItem();
        if (selectedIndex !== undefined) {
          this.navigationItems[selectedIndex].activate();
          this.inputElement.nativeElement.blur();
        }
        event.preventDefault();
        break;
      }
      case 'Escape':
        this.close();
        this.inputElement.nativeElement.blur();
        event.preventDefault();
        break;
    }
  }

  private indexOfSelectedNavigationItem() {
    return this.navigationItems.findIndex(next => next.selected);
  }

  private moveNavigationIndex(value: number, next: boolean, loop = true) {
    const selectedIndex = this.indexOfSelectedNavigationItem();
    let indexToSelect: number | undefined = next ? selectedIndex + value : selectedIndex - value;
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
      this.selectNavigationItem(indexToSelect);
    }
  }

  private selectNavigationItem(index: number) {
    this.navigationItems.forEach(next => next.selected = false);
    this.navigationItems[index].selected = true;
    const selectedElement = this.linkElements.toArray()[index];
    // Element could be not shown yet, therefore undefined is fine.
    if (selectedElement) {
      scrollIntoElement(selectedElement.nativeElement, false);
    }
  }

  selectSong(song: Song) {
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[song.id]));
  }

  goToSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.requestScrollToSong(song);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[song.id]));
    this.close();
  }

  selectAlbum(album: Album) {
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[album.id]));
  }

  goToAlbum(album: Album) {
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.requestScrollToAlbum(album);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[album.id]));
    this.close();
  }

  selectArtist(artist: Artist) {
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[artist.id]));
  }

  goToArtist(artist: Artist) {
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.requestScrollToArtist(artist);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[artist.id]));
    this.close();
  }

  trackByArtist() {
    return 'reuseView';
  }

  trackBySong() {
    return 'reuseView';
  }

  trackByAlbum() {
    return 'reuseView';
  }
}
