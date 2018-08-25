import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {AlbumSongs, Song} from '../core/library/library.model';

class Disc {
  discNumber: number;
  showArtist: boolean;
  songs: Song[];
}

@Component({
  selector: 'pony-album',
  templateUrl: './album.component.html',
  styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnChanges {

  @Input() albumSongs: AlbumSongs;

  discs: Disc[];

  private static compareSongs(song1: Song, song2: Song): number {
    if (song1.trackNumber > song2.trackNumber) {
      return 1;
    }
    if (song1.trackNumber < song2.trackNumber) {
      return -1;
    }
    if (song1.name > song2.name) {
      return 1;
    }
    if (song1.name < song2.name) {
      return -1;
    }
    return 0;
  }

  private static compareDiscs(disc1: Disc, disc2: Disc) {
    if (disc1.discNumber > disc2.discNumber) {
      return 1;
    } else {
      return -1;
    }
  }

  private static nullSafeNormalizedEquals(value1: string, value2: string): boolean {
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
      const disc = new Disc();
      if (discNumber !== 1 || discToSongs.size !== 1) {
        disc.discNumber = discNumber;
      }
      disc.songs = discSongs;
      disc.songs.sort(AlbumComponent.compareSongs);
      disc.showArtist = false;
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
