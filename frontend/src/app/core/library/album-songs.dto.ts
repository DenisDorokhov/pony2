import {AlbumDto} from './album.dto';
import {SongDto} from './song.dto';

export interface AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}
