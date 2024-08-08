import {Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {debounceTime, mergeMap, of, Subject, Subscription} from "rxjs";
import {distinctUntilChanged, map} from "rxjs/operators";
import {LibraryService} from "../../service/library.service";
import {Album, Artist, SearchResult, Song} from "../../domain/library.model";
import {ImageLoaderComponent} from "../common/image-loader.component";
import {ScrollingUtils} from "../../utils/scrolling.utils";
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
  imports: [TranslateModule, CommonModule, ImageLoaderComponent],
  selector: 'pony-fast-search',
  templateUrl: './fast-search.component.html',
  styleUrls: ['./fast-search.component.scss']
})
export class FastSearchComponent implements OnInit, OnDestroy {

  open = false;
  searchResult: SearchResult | undefined;

  navigationItems: NavigationItem[] = [];
  idToNavigationItem: {[key: string]: NavigationItem} = {};

  @ViewChild('container') containerElement!: ElementRef;
  @ViewChild('input') inputElement!: ElementRef;
  @ViewChild('searchResults') searchResultsElement!: ElementRef;
  @ViewChildren('links') linkElements!: QueryList<ElementRef>;

  private searchSubject = new Subject<string>();

  private searchSubscription: Subscription | undefined;

  constructor(
    private readonly libraryService: LibraryService,
  ) {
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      map(value => value.trim()),
      debounceTime(200),
      distinctUntilChanged(),
      mergeMap(query => {
        if (query.length > 2) {
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
      this.searchResult = searchResult;
      this.navigationItems = [];
      this.idToNavigationItem = {};
      this.searchResult?.artists.forEach(artist => {
        const navigationItem = new NavigationItem(artist.id, () => this.selectArtist(artist));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      this.searchResult?.songs.forEach(song => {
        const navigationItem = new NavigationItem(song.id, () => this.selectSong(song));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      this.searchResult?.albums.forEach(album => {
        const navigationItem = new NavigationItem(album.id, () => this.selectAlbum(album));
        this.navigationItems.push(navigationItem);
        this.idToNavigationItem[navigationItem.id] = navigationItem;
      });
      this.searchResultsElement.nativeElement.scrollTop = 0;
    });
    window.document.body.addEventListener('mousedown', event => {
      let checkElement: Node | null = event.target as Node;
      let clickWithinContainer = false;
      do {
        clickWithinContainer = this.containerElement.nativeElement === checkElement;
        checkElement = (checkElement as Node).parentNode;
      } while (!clickWithinContainer && checkElement);
      if (!clickWithinContainer) {
        this.open = false;
      }
    });
  }

  onInputChange(event: Event) {
    this.searchSubject.next((event.target as any).value);
  }

  onFocusIn() {
    this.open = true;
  }

  onInputKeyDown(event: KeyboardEvent) {
    if (event.key === 'ArrowDown') {
      this.moveNavigation(true);
      event.preventDefault();
    }
    if (event.key === 'ArrowUp') {
      this.moveNavigation(false);
      event.preventDefault();
    }
    if (event.key === 'Enter') {
      const selectedIndex = this.indexOfSelectedNavigationItem();
      if (selectedIndex !== undefined) {
        this.navigationItems[selectedIndex].activate();
      }
      event.preventDefault();
    }
    if (event.key === 'Escape') {
      this.open = false;
      this.inputElement.nativeElement.blur();
      event.preventDefault();
    }
  }

  private indexOfSelectedNavigationItem() {
    return this.navigationItems.findIndex(next => next.selected);
  }

  private moveNavigation(next: boolean) {
    const selectedIndex = this.indexOfSelectedNavigationItem();
    this.navigationItems.forEach(next => next.selected = false);
    let indexToSelect: number | undefined = next ? selectedIndex + 1 : selectedIndex - 1;
    if (indexToSelect < 0) {
      indexToSelect = this.navigationItems.length - 1;
    }
    if (indexToSelect > this.navigationItems.length - 1) {
      indexToSelect = 0;
    }
    if (indexToSelect >= 0) {
      this.navigationItems[indexToSelect].selected = true;
      scrollIntoElement(this.linkElements.toArray()[indexToSelect].nativeElement, false);
    }
  }

  selectSong(song: Song, close = true) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.startScrollToSong(song);
    if (close) {
      this.open = false;
    }
  }

  selectAlbum(album: Album, close = true) {
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.startScrollToAlbum(album);
    if (close) {
      this.open = false;
    }
  }

  selectArtist(artist: Artist, close = true) {
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.startScrollToArtist(artist);
    if (close) {
      this.open = false;
    }
  }
}
