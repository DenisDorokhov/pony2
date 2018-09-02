import {Component, ElementRef, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {Artist} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';
import {ScrollingUtils} from '../shared/scrolling.utils';

@Component({
  selector: 'pony-artist',
  templateUrl: './artist.component.html',
  styleUrls: ['./artist.component.scss']
})
export class ArtistComponent implements OnInit, OnDestroy {
  
  @Input() artist: Artist;

  @ViewChild('container') containerElement: ElementRef;
  
  selected = false;
  
  private selectedArtistSubscription: Subscription;
  private scrollToSongRequestSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.selectedArtistSubscription = this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        this.selected = artist.id === this.artist.id;
      });
    this.scrollToSongRequestSubscription = this.libraryService.observeScrollToSongRequest()
      .subscribe(song => {
        if (song.album.artist.id === this.artist.id) {
          window.requestAnimationFrame(() => {
            ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          });
        }
      });
  }
  
  ngOnDestroy(): void {
    this.selectedArtistSubscription.unsubscribe();
    this.scrollToSongRequestSubscription.unsubscribe();
  }
  
  select() {
    this.libraryService.selectArtist(this.artist);
  }
}
