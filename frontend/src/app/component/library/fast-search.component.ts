import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {debounceTime, mergeMap, of, Subject, Subscription} from "rxjs";
import {distinctUntilChanged, map} from "rxjs/operators";
import {LibraryService} from "../../service/library.service";
import {Album, Artist, SearchResult, Song} from "../../domain/library.model";
import {ImageLoaderComponent} from "../common/image-loader.component";

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

  @ViewChild('container') containerElement!: ElementRef;
  @ViewChild('searchResults') searchResultsElement!: ElementRef;

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

  onSongSelection(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.startScrollToSong(song);
    this.open = false;
  }

  onAlbumSelection(album: Album) {
    this.libraryService.selectArtistAndMakeDefault(album.artist);
    this.libraryService.startScrollToAlbum(album);
    this.open = false;
  }

  onArtistSelection(artist: Artist) {
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.startScrollToArtist(artist);
    this.open = false;
  }
}
