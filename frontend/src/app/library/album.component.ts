import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {AlbumSongsDto} from '../core/library/album-songs.dto';
import {SongDto} from '../core/library/song.dto';

class Disc {
  discNumber: number;
  songs: SongDto[];
}

@Component({
  selector: 'pony-album',
  templateUrl: './album.component.html',
  styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnChanges {

  @Input() albumSongs: AlbumSongsDto;

  discs: Disc[];

  private static compareSongs(song1: SongDto, song2: SongDto): number {
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

  ngOnChanges(changes: SimpleChanges): void {
    this.splitAlbumsIntoDiscs();
  }

  private splitAlbumsIntoDiscs() {

    const discToSongs = new Map<number, SongDto[]>();
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
      this.discs.push(disc);
    });
    this.discs.sort(AlbumComponent.compareDiscs);
  }
}
