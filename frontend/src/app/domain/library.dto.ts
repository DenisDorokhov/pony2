export interface AlbumDto {
  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  year: number | undefined;
  artworkId: string | undefined;
  artistId: string;
}

export interface AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}

export interface ArtistDto {
  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  artworkId: string | undefined;
}

export interface ArtistSongsDto {
  artist: ArtistDto;
  albumSongs: AlbumSongsDto[];
}

export class SongDto {
  id!: string;
  creationDate!: Date;
  updateDate: Date | undefined;
  path!: string;
  mimeType!: string;
  fileExtension!: string;
  size!: number;
  duration!: number;
  bitRate!: number;
  bitRateVariable!: boolean;
  discNumber: number | undefined;
  trackNumber: number | undefined;
  name: string | undefined;
  artistName: string | undefined;
  albumId!: string;
  genreId!: string;
}

export class ScanStatisticsDto {
  date!: Date;
  duration!: number;
  songSize!: number;
  artworkSize!: number;
  genreCount!: number;
  artistCount!: number;
  albumCount!: number;
  songCount!: number;
  artworkCount!: number;
}
