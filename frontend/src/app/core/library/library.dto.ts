export interface AlbumDto {
  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  year: number;
  artworkId: number;
  artistId: number;
}

export interface AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}

export interface ArtistDto {
  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  artworkId: number;
}

export interface ArtistSongsDto {
  artist: ArtistDto;
  albumSongs: AlbumSongsDto[];
}

export class SongDto {
  id: number;
  creationDate: Date;
  updateDate: Date;
  mimeType: string;
  size: number;
  duration: number;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number;
  trackNumber: number;
  name: string;
  artistName: string;
  albumId: number;
  genreId: number;
}
