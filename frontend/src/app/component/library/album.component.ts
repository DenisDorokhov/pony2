import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {AlbumSongs, Song} from "../../domain/library.model";
import {SongListComponent} from "./song-list.component";
import {TranslateModule} from "@ngx-translate/core";
import {ImageLoaderComponent} from "../common/image-loader.component";
import {CommonModule} from "@angular/common";

interface Disc {
  discNumber: number | undefined;
  showArtist: boolean;
  songs: Song[];
}

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, ImageLoaderComponent, SongListComponent],
  selector: 'pony-album',
  templateUrl: './album.component.html',
  styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnChanges {

  @Input() albumSongs!: AlbumSongs;

  discs: Disc[] = [];

  private static compareSongs(song1: Song, song2: Song): number {
    if ((song1.trackNumber ?? 0) > (song2.trackNumber ?? 0)) {
      return 1;
    }
    if ((song1.trackNumber ?? 0) < (song2.trackNumber ?? 0)) {
      return -1;
    }
    if ((song1.name ?? "") > (song2.name ?? "")) {
      return 1;
    }
    if ((song1.name ?? "") < (song2.name ?? "")) {
      return -1;
    }
    return 0;
  }

  private static compareDiscs(disc1: Disc, disc2: Disc) {
    if ((disc1.discNumber ?? 0) > (disc2.discNumber ?? 0)) {
      return 1;
    } else {
      return -1;
    }
  }

  private static nullSafeNormalizedEquals(value1: string | undefined, value2: string | undefined): boolean {
    if (value1 == null && value2 == null) {
      return true;
    }
    if (value1 == null || value2 == null) {
      return false;
    }
    const normalizedValue1 = value1.trim().toLowerCase();
    const normalizedValue2 = value2.trim().toLowerCase();
    return normalizedValue1 === normalizedValue2;
  }

  ngOnChanges(changes: SimpleChanges) {
    this.splitAlbumsIntoDiscs();
  }

  download() {
    window.open(`/api/file/export/album/${this.albumSongs.album.id}`, '_blank', '');
  }

  trackByIndex(index: number): number {
    return index;
  }

  private splitAlbumsIntoDiscs() {

    const discToSongs = new Map<number, Song[]>();
    this.albumSongs.songs.forEach(song => {
      const discNumber = song.discNumber && song.discNumber !== 0 ? song.discNumber : 1;
      let discSongs = discToSongs.get(discNumber);
      if (!discSongs) {
        discSongs = [];
      }
      discSongs.push(song);
      discToSongs.set(discNumber, discSongs);
    });

    this.discs = [];
    discToSongs.forEach((discSongs, discNumber) => {
      const disc: Disc = {
        discNumber: (discNumber !== 1 || discToSongs.size !== 1 ? discNumber : undefined),
        showArtist: false,
        songs: discSongs
      };
      disc.songs.sort(AlbumComponent.compareSongs);
      disc.songs.forEach(song => {
        if (!disc.showArtist) {
          disc.showArtist = !AlbumComponent.nullSafeNormalizedEquals(song.artistName, this.albumSongs.album.artist.name);
        }
      });
      this.discs.push(disc);
    });
    this.discs.sort(AlbumComponent.compareDiscs);
  }
}
