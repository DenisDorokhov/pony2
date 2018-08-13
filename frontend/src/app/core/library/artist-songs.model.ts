import {AlbumSongs} from './album-songs.model';
import {Artist} from './artist.model';

export class ArtistSongs {

  artist: Artist;
  albumSongs: AlbumSongs[];

  constructor(partial?: Partial<ArtistSongs>) {
    Object.assign(this, partial);
  }
}
