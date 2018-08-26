import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {Song} from '../core/library/library.model';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy {

  @Input() song: Song;
  @Input() showArtist = false;

  selected = false;
  duration: string;

  private selectedSongSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.selectedSongSubscription = this.libraryService.selectedSong.subscribe(song => {
      this.selected = song && song.id === this.song.id;
    });
    this.duration = this.song.durationInMinutes;
  }

  ngOnDestroy(): void {
    this.selectedSongSubscription.unsubscribe();
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  play() {
    this.libraryService.requestSongPlayback(this.song);
  }
}
