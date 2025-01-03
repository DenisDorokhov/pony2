import {Component, Input, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {Song} from '../../domain/library.model';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {SongListComponent} from './song-list.component';
import {nullSafeNormalizedEquals} from '../../utils/common.utils';

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, ImageLoaderComponent, SongListComponent],
  selector: 'pony-like-album',
  templateUrl: './like-album.component.html',
  styleUrls: ['./like-album.component.scss']
})
export class LikeAlbumComponent implements OnInit {

  @Input()
  songs: Song[] = [];
  @Input()
  artworkUrl: string | undefined;

  showArtist = false;

  ngOnInit(): void {
    this.songs.forEach(song => {
      if (!this.showArtist) {
        this.showArtist = !nullSafeNormalizedEquals(song.artistName, song.album.artist.name);
      }
    });
  }
}
