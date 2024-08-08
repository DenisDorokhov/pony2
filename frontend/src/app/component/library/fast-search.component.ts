import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {SearchService} from "../../service/search.service";
import {SearchResultDto, SongDetailsDto} from "../../domain/library.dto";
import {debounceTime, mergeMap, of, Subject, Subscription} from "rxjs";
import {distinctUntilChanged, map} from "rxjs/operators";
import {LibraryService} from "../../service/library.service";
import {Album, Artist, Song} from "../../domain/library.model";
import {ImageLoaderComponent} from "../common/image-loader.component";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ImageLoaderComponent],
  selector: 'pony-fast-search',
  templateUrl: './fast-search.component.html',
  styleUrls: ['./fast-search.component.scss']
})
export class FastSearchComponent implements OnInit, OnDestroy {

  active = false;
  searchResult: SearchResultDto | undefined;

  @ViewChild('searchResults') searchResultsElement!: ElementRef;

  private searchSubject = new Subject<string>();

  private searchSubscription: Subscription | undefined;

  constructor(
    private readonly searchService: SearchService,
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
          return this.searchService.search(query);
        } else {
          return of(undefined);
        }
      }),
      map(searchResult => {
        if (searchResult?.songDetails.length || searchResult?.albumDetails.length || searchResult?.artists.length) {
          return searchResult;
        } else {
          return undefined;
        }
      })
    ).subscribe(searchResult => {
      this.searchResult = searchResult;
      this.searchResultsElement.nativeElement.scrollTop = 0;
    });
  }

  onInputChange(event: Event) {
    this.searchSubject.next((event.target as any).value);
  }

  onFocusIn() {
    this.active = true;
  }

  onFocusOut() {
    this.active = false;
  }

  onSongSelection(songDetails: SongDetailsDto) {
    const artist = new Artist(songDetails.albumDetails.artist);
    const album = new Album(songDetails.albumDetails.album, artist);
    const song = new Song(songDetails.song, album);
    this.libraryService.selectArtistAndMakeDefault(artist);
    this.libraryService.selectSong(song);
    this.libraryService.startScrollToSong(song);
  }
}
