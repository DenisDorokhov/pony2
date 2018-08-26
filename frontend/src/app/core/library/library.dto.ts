export interface AlbumDto {
  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  year: number | undefined;
  artworkId: number | undefined;
  artistId: number;
}

export interface AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}

export interface ArtistDto {
  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  artworkId: number | undefined;
}

export interface ArtistSongsDto {
  artist: ArtistDto;
  albumSongs: AlbumSongsDto[];
}

export class SongDto {
  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
  mimeType: string;
  fileExtension: string;
  size: number;
  duration: number;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number | undefined;
  trackNumber: number | undefined;
  name: string | undefined;
  artistName: string | undefined;
  albumId: number;
  genreId: number;
}
