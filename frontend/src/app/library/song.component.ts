import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {LibraryService} from '../core/library/library.service';
import {Song} from '../core/library/song.model';

@Component({
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy {

  @Input() song: Song;
  @Input() showArtist = false;

  selected = false;

  private selectedSongSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.selectedSongSubscription = this.libraryService.selectedSong.subscribe(song => {
      this.selected = song && song.id === this.song.id;
    });
  }

  ngOnDestroy(): void {
    this.selectedSongSubscription.unsubscribe();
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  get durationInMinutes(): string {
    const minutes = Math.floor(this.song.duration / 60);
    const seconds = this.song.duration - minutes * 60;
    let buf = minutes + ':';
    if (seconds <= 9) {
      buf += '0';
    }
    buf += seconds;
    return buf;
  }
}
