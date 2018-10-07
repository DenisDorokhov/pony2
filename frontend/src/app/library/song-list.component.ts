import {Component, Input} from '@angular/core';
import {Song} from '../core/library/library.model';

@Component({
  selector: 'pony-song-list',
  templateUrl: './song-list.component.html',
  styleUrls: ['./song-list.component.scss']
})
export class SongListComponent {

  @Input() caption: string;
  @Input() songs: Song[];
  @Input() showArtist: boolean;

  trackByIndex(index: number): number {
    return index;
  }
}
