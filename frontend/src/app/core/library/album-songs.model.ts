import {Album} from './album.model';
import {Song} from './song.model';

export class AlbumSongs {

  album: Album;
  songs: Song[];

  constructor(partial?: Partial<AlbumSongs>) {
    Object.assign(this, partial);
  }
}
