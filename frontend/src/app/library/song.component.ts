import {Component, Input} from '@angular/core';
import {SongDto} from '../core/library/song.dto';

@Component({
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent {

  @Input() song: SongDto;

  durationInMinutes(): string {
    const minutes = Math.floor(this.song.duration / 60);
    const seconds = this.song.duration - minutes * 60;
    let buf = '';
    buf += minutes + ':';
    if (seconds <= 9) {
      buf += '0';
    }
    buf += seconds;
    return buf;
  }
}
