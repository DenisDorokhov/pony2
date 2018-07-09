import {Component} from '@angular/core';

@Component({
  selector: 'pony-player',
  templateUrl: './player.component.html',
  styleUrls: ['./player.component.scss']
})
export class PlayerComponent {

  progress = 0.0; // 0.0 - 1.0.

  seek(event: MouseEvent) {
    const progressBar = event.currentTarget as Element;
    const progressBarRect = progressBar.getBoundingClientRect();
    this.progress = (event.clientX - progressBarRect.left) / progressBar.clientWidth;
  }

  get timeInMinutes(): string {
    return '00:00';
  }

  get durationInMinutes(): string {
    return '00:00';
  }
}
