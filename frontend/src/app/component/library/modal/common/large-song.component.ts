import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {UnknownArtistPipe} from '../../../../pipe/unknown-artist.pipe';
import {UnknownSongPipe} from '../../../../pipe/unknown-song.pipe';
import {ImageLoaderComponent} from '../../../common/image-loader.component';
import {UnknownAlbumPipe} from '../../../../pipe/unknown-album.pipe';
import {UnknownGenrePipe} from '../../../../pipe/unknown-genre.pipe';
import {Song} from '../../../../domain/library.model';
import {PlaybackState} from '../../../../service/audio-player.service';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle
} from '@ng-bootstrap/ng-bootstrap';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    UnknownArtistPipe,
    UnknownSongPipe,
    ImageLoaderComponent,
    UnknownAlbumPipe,
    UnknownGenrePipe,
    NgbDropdown,
    NgbDropdownButtonItem,
    NgbDropdownItem,
    NgbDropdownMenu,
    NgbDropdownToggle,
  ],
  selector: 'pony-large-song',
  templateUrl: './large-song.component.html',
  styleUrls: ['./large-song.component.scss']
})
export class LargeSongComponent {

  static readonly HEIGHT = 76;

  readonly PlaybackState = PlaybackState;

  @Input()
  index!: number;
  @Input()
  song!: Song;
  @Input()
  selected = false;
  @Input()
  isCurrentSong = false;
  @Input()
  playbackState: PlaybackState | undefined;
  @Input()
  showIndex = true;
  @Input()
  showDuration = true;

  @Output()
  doubleClick = new EventEmitter<number>();
  @Output()
  playOrPauseRequested = new EventEmitter<number>();
  @Output()
  goToSongRequested = new EventEmitter<number>();
  @Output()
  removalRequested = new EventEmitter<number>();

  isMouseOver = false;

  onMouseMove() {
    this.isMouseOver = true;
  }

  onMouseLeave() {
    this.isMouseOver = false;
  }

  onPlaybackClick() {
    this.playOrPauseRequested.emit(this.index);
  }

  goToSong() {
    this.goToSongRequested.emit(this.index);
  }

  removeSong() {
    this.removalRequested.emit(this.index);
  }

  onDoubleClick(event: MouseEvent) {
    let checkElement: Node | null = event.target as Node;
    let isButtonClick = false;
    do {
      isButtonClick = checkElement.nodeName === 'BUTTON';
      checkElement = (checkElement as Node).parentNode;
    } while (!isButtonClick && checkElement);
    if (!isButtonClick) {
      this.doubleClick.emit(this.index);
    }
  }

  preventDoubleClickDefault(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }
}
