import {AlbumSongsDto} from './album-songs.dto';
import {ArtistDto} from './artist.dto';

export class ArtistSongsDto {
  artist: ArtistDto;
  albums: AlbumSongsDto[];
}
