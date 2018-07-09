import {AlbumDto} from './album.dto';
import {SongDto} from './song.dto';

export class AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}
