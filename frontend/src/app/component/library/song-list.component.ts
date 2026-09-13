import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Song} from '../../domain/library.model';
import {SongComponent} from './song.component';

@Component({
  imports: [SongComponent],
  selector: 'pony-song-list',
  templateUrl: './song-list.component.html',
  styleUrls: ['./song-list.component.scss']
})
export class SongListComponent {

  @Input() caption!: string;
  @Input() songs!: Song[];
  @Input() showArtist!: boolean;

  @Output() switchQueueRequested = new EventEmitter<Song>();

  trackByIndex(index: number) {
    return index;
  }

  switchQueue(song: Song) {
    this.switchQueueRequested.emit(song);
  }
}
