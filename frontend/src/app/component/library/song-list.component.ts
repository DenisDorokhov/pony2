import {Component, Input} from '@angular/core';
import {Song} from "../../domain/library.model";
import {SongComponent} from "./song.component";
import {CommonModule} from "@angular/common";

@Component({
  standalone: true,
  imports: [CommonModule, SongComponent],
  selector: 'pony-song-list',
  templateUrl: './song-list.component.html',
  styleUrls: ['./song-list.component.scss']
})
export class SongListComponent {

  @Input() caption!: string;
  @Input() songs!: Song[];
  @Input() showArtist!: boolean;

  trackByIndex(index: number, _: Song) {
    return index;
  }
}
