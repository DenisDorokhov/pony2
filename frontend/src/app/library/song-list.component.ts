import {Component, Input} from '@angular/core';
import {SongDto} from '../core/library/song.dto';

@Component({
  selector: 'pony-song-list',
  templateUrl: './song-list.component.html',
  styleUrls: ['./song-list.component.scss']
})
export class SongListComponent {
  @Input() caption: string;
  @Input() songs: SongDto[];
}
