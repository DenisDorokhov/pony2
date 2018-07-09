import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {AlbumSongsDto} from '../core/library/album-songs.dto';
import {SongDto} from '../core/library/song.dto';

class SongList {
  caption: string;
  songs: SongDto[];
}

@Component({
  selector: 'pony-album',
  templateUrl: './album.component.html',
  styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnChanges {

  @Input() albumSongs: AlbumSongsDto;

  songLists: SongList[];

  constructor(private translateService: TranslateService) {
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

    this.songLists = [];
    discToSongs.forEach((discSongs, discNumber) => {
      const songList = new SongList();
      if (discNumber !== 1 || discToSongs.size !== 1) {
        songList.caption = this.translateService.instant('library.album.discLabel', {discNumber: discNumber});
      }
      songList.songs = discSongs;
      this.songLists.push(songList);
    });
  }
}
