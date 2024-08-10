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
import {UnknownSongPipe} from "../../pipe/unknown-song.pipe";
import {UnknownArtistPipe} from "../../pipe/unknown-artist.pipe";
import {UnknownAlbumPipe} from "../../pipe/unknown-album.pipe";

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
      case 'Enter':
        const selectedIndex = this.indexOfSelectedNavigationItem();
        if (selectedIndex !== undefined) {
          this.navigationItems[selectedIndex].activate();
          this.inputElement.nativeElement.blur();
        }
        event.preventDefault();
        break;
      case 'Escape':
        this.open = false;
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
    scrollIntoElement(this.linkElements.toArray()[index].nativeElement, false);
  }

  selectSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.startScrollToSong(song);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[song.id]));
    this.open = false;
  }

  selectAlbum(album: Album) {
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.startScrollToAlbum(album);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[album.id]));
    this.open = false;
  }

  selectArtist(artist: Artist) {
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.startScrollToArtist(artist);
    this.selectNavigationItem(this.navigationItems.indexOf(this.idToNavigationItem[artist.id]));
    this.open = false;
  }

  trackByArtist(_: number, artist: Artist) {
    return artist.id;
  }

  trackBySong(_: number, song: Song) {
    return song.id;
  }

  trackByAlbum(_: number, album: Album) {
    return album.id;
  }
}
