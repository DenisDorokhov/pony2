import {AlbumSongsDto} from './album-songs.dto';
import {ArtistDto} from './artist.dto';

export interface ArtistSongsDto {
  artist: ArtistDto;
  albumSongs: AlbumSongsDto[];
}
